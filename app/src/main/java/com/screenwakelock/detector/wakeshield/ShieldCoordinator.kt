package com.screenwakelock.detector.wakeshield

import android.app.KeyguardManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import com.screenwakelock.detector.data.repository.NotificationCacheRepository
import com.screenwakelock.detector.data.repository.PreferencesRepository
import com.screenwakelock.detector.data.repository.WakeEventRepository
import com.screenwakelock.detector.domain.attributor.WakeAttributor
import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.service.NotificationCaptureService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShieldCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val wakeEventRepository: WakeEventRepository,
    private val notificationCacheRepository: NotificationCacheRepository,
    private val wakeAttributor: WakeAttributor,
    private val runtimeState: ShieldRuntimeState,
    private val hardExemptResolver: ShieldHardExemptResolver,
    private val rootWakeEnforcer: RootWakeEnforcer,
) {
    /**
     * Called after a wake is logged. Forensics may run with shield off;
     * enforcement only when shield armed.
     */
    suspend fun onWakeLogged(event: WakeEvent) {
        val forensics = preferencesRepository.wakeForensicsEnabled.first()
        val shieldEnabled = preferencesRepository.shieldEnabled.first()
        if (!forensics && !shieldEnabled) return

        val evidence = ShieldEvidence.build(
            active = NotificationCaptureService.snapshotActiveNotifications(),
            candidates = event.candidates,
            wakelockTag = event.wakelockTag,
        )
        if (forensics || shieldEnabled) {
            wakeEventRepository.updateShieldFields(
                id = event.id,
                shieldOutcome = event.shieldOutcome ?: ShieldOutcome.NONE.name,
                shieldDetail = ShieldEvidence.mergeDetail(event.shieldDetail, evidence.summary),
                evidencePackagesJson = ShieldEvidence.encodePackages(evidence.activePackages),
            )
        }

        if (!shieldEnabled) return

        if (runtimeState.isSelfWakeWindow()) {
            wakeEventRepository.updateShieldFields(
                id = event.id,
                shieldOutcome = ShieldOutcome.SUPPRESSED_SELF.name,
                shieldDetail = evidence.summary,
                evidencePackagesJson = ShieldEvidence.encodePackages(evidence.activePackages),
            )
            return
        }

        delay(ShieldPolicy.GRACE_MS)

        if (!preferencesRepository.shieldEnabled.first()) {
            wakeEventRepository.updateShieldFields(
                id = event.id,
                shieldOutcome = ShieldOutcome.PANIC_DISABLED.name,
                shieldDetail = evidence.summary,
                evidencePackagesJson = ShieldEvidence.encodePackages(evidence.activePackages),
            )
            return
        }

        val interactive = isInteractiveAbort()
        val rootEnabled = preferencesRepository.rootEnabled.first()
        val attribution = wakeAttributor.attribute(
            screenOnMillis = System.currentTimeMillis(),
            notificationCache = notificationCacheRepository,
            rootEnabled = rootEnabled,
        )
        val active = NotificationCaptureService.snapshotActiveNotifications()
        val matched = active.firstOrNull { it.packageName == attribution.packageName }
        val hasFsi = matched?.hasFullScreenIntent == true ||
            attribution.reasonCode == ReasonCode.NOTIFICATION_FULL_SCREEN
        val hardExempt = hardExemptResolver.resolve()
        val allowlist = preferencesRepository.shieldAllowlistPackages.first()

        var decision = ShieldPolicy.decide(
            ShieldPolicyInput(
                shieldEnabled = true,
                inCooldown = runtimeState.inCooldown(),
                interactiveAbort = interactive,
                packageName = attribution.packageName,
                reasonCode = attribution.reasonCode,
                hasFullScreenIntent = hasFsi,
                fsiPermissionGranted = false,
                userAllowlist = allowlist,
                hardExempt = hardExempt,
                selfPackage = context.packageName,
            ),
        )

        // Late soft-allow: re-check FSI/exempt on active notifications before L2/L3.
        if (decision is ShieldDecision.Hostile) {
            val lateFsi = active.any { snap ->
                (snap.hasFullScreenIntent || snap.hasTurnScreenOn) &&
                    (snap.packageName in hardExempt ||
                        snap.packageName in allowlist ||
                        snap.packageName == attribution.packageName && hasFsi)
            }
            val lateExemptPkg = active.any { it.packageName in hardExempt }
            if (lateExemptPkg) {
                decision = ShieldDecision.AllowExempt
            } else if (lateFsi || hasFsi) {
                decision = ShieldDecision.AllowFsi
            }
        }

        when (decision) {
            ShieldDecision.SkipDisabled -> return
            ShieldDecision.SkipCooldown -> {
                persist(event.id, ShieldOutcome.NONE, "cooldown", evidence)
            }
            ShieldDecision.AbortInteractive -> {
                persist(event.id, ShieldOutcome.ABORTED_INTERACTIVE, evidence.summary, evidence)
            }
            ShieldDecision.AllowExempt -> {
                persist(event.id, ShieldOutcome.ALLOWED_EXEMPT, evidence.summary, evidence)
            }
            ShieldDecision.AllowFsi -> {
                persist(event.id, ShieldOutcome.ALLOWED_FSI, evidence.summary, evidence)
            }
            is ShieldDecision.Hostile -> {
                if (!preferencesRepository.shieldEnabled.first()) {
                    persist(event.id, ShieldOutcome.PANIC_DISABLED, evidence.summary, evidence)
                    return
                }
                enforceHostile(event, decision, attribution.wakelockTag, evidence)
            }
        }
    }

    private suspend fun enforceHostile(
        event: WakeEvent,
        decision: ShieldDecision.Hostile,
        wakelockTag: String?,
        evidence: ShieldEvidenceSnapshot,
    ) {
        val tiers = mutableListOf<String>()
        var cancelled = 0
        var locked = false
        var slept = false
        var denied = false
        var rootFailed = false

        // L1 — cancel attributed package notifications (never CALL/ALARM categories in proactive path)
        runCatching {
            val pkg = decision.packageName
            if (!pkg.isNullOrBlank()) {
                cancelled = NotificationCaptureService.dismissNotifications(pkg, null)
                if (cancelled > 0) tiers += "L1"
            }
            NotificationCaptureService.cancelWakeCapableExcept(
                exemptPackages = hardExemptResolver.resolve() +
                    preferencesRepository.shieldAllowlistPackages.first() +
                    context.packageName,
            ).also { if (it > 0) { cancelled += it; if ("L1" !in tiers) tiers += "L1" } }
        }

        if (!preferencesRepository.shieldEnabled.first()) {
            persist(event.id, ShieldOutcome.PANIC_DISABLED, "panic mid-flight", evidence)
            return
        }

        // Late soft-allow again immediately before L2/L3
        val active = NotificationCaptureService.snapshotActiveNotifications()
        val hardExempt = hardExemptResolver.resolve()
        if (active.any { it.packageName in hardExempt && (it.hasFullScreenIntent || it.category == "alarm" || it.category == "call") }) {
            persist(event.id, ShieldOutcome.ALLOWED_EXEMPT, "late-exempt", evidence)
            return
        }

        if (runtimeState.inCooldown()) {
            persist(event.id, ShieldOutcome.NONE, "cooldown-before-l2", evidence)
            return
        }

        // L2
        runCatching {
            if (LockScreenActor.lockNow()) {
                locked = true
                tiers += "L2"
            }
        }

        // L3
        val rootKill = preferencesRepository.shieldRootKillEnabled.first() &&
            preferencesRepository.rootEnabled.first()
        if (rootKill) {
            if (!preferencesRepository.shieldEnabled.first()) {
                persist(event.id, ShieldOutcome.PANIC_DISABLED, "panic before L3", evidence)
                return
            }
            val result = runCatching {
                rootWakeEnforcer.enforce(
                    packageName = decision.packageName,
                    wakelockTag = wakelockTag ?: event.wakelockTag,
                    allowAppOpDeny = decision.allowAppOpDeny &&
                        preferencesRepository.shieldRootKillEnabled.first(),
                    displayStillOn = isDisplayOn(),
                )
            }.getOrElse {
                Log.w(TAG, "L3 failed: ${it.message}")
                rootFailed = true
                null
            }
            if (result != null) {
                slept = result.slept
                denied = result.deniedAppOp
                if (result.slept || result.deniedAppOp || result.wakeUnlocked) {
                    tiers += "L3"
                } else if (!result.slept) {
                    rootFailed = true
                }
                if (denied && !decision.packageName.isNullOrBlank()) {
                    preferencesRepository.addShieldDeniedPackage(decision.packageName)
                }
            } else {
                rootFailed = true
            }
        }

        if (locked || slept || denied || cancelled > 0) {
            runtimeState.markEnforcement()
        }

        val outcome = when {
            rootFailed && !locked && !slept && cancelled == 0 -> ShieldOutcome.ROOT_FAILED
            locked && slept -> ShieldOutcome.PARTIAL
            slept -> ShieldOutcome.SLEPT
            locked -> ShieldOutcome.LOCKED
            denied -> ShieldOutcome.DENIED_APPOP
            cancelled > 0 -> ShieldOutcome.CANCELLED_NOTIFS
            tiers.isEmpty() -> ShieldOutcome.PARTIAL
            else -> ShieldOutcome.PARTIAL
        }
        val detail = buildList {
            add(ShieldEvidence.encodeOutcomeMeta(cancelled, tiers))
            add(evidence.summary)
        }.joinToString(" | ")
        persist(event.id, outcome, detail, evidence)
        Log.i(TAG, "Shield enforced event=${event.id} outcome=$outcome tiers=$tiers")
    }

    private suspend fun persist(
        id: Long,
        outcome: ShieldOutcome,
        detail: String?,
        evidence: ShieldEvidenceSnapshot,
    ) {
        wakeEventRepository.updateShieldFields(
            id = id,
            shieldOutcome = outcome.name,
            shieldDetail = ShieldEvidence.mergeDetail(detail, null),
            evidencePackagesJson = ShieldEvidence.encodePackages(evidence.activePackages),
        )
    }

    private fun isInteractiveAbort(): Boolean {
        val kg = context.getSystemService(KeyguardManager::class.java) ?: return false
        // Only abort when a secure keyguard was dismissed (user looking at phone).
        // Devices without a lock screen do not abort on this heuristic alone.
        return kg.isKeyguardSecure && !kg.isKeyguardLocked && isDisplayOn()
    }

    private fun isDisplayOn(): Boolean {
        val dm = context.getSystemService(DisplayManager::class.java) ?: return false
        val state = dm.getDisplay(Display.DEFAULT_DISPLAY)?.state ?: return false
        return state == Display.STATE_ON || state == Display.STATE_ON_SUSPEND
    }

    suspend fun panicDisable() {
        preferencesRepository.setShieldEnabled(false)
        runtimeState.clear()
        val denied = preferencesRepository.shieldDeniedPackages.first()
        denied.forEach { pkg ->
            runCatching { rootWakeEnforcer.restoreAppOp(pkg) }
        }
        preferencesRepository.clearShieldDeniedPackages()
        Log.i(TAG, "Wake Shield panic disabled; restored ${denied.size} appops")
    }

    companion object {
        private const val TAG = "ShieldCoordinator"
    }
}
