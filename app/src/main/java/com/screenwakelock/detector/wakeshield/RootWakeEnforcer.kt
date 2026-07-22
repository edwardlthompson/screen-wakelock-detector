package com.screenwakelock.detector.wakeshield

import android.util.Log
import com.screenwakelock.detector.root.RootCommandAllowlist
import com.screenwakelock.detector.root.RootCommandRunner
import javax.inject.Inject
import javax.inject.Singleton

data class RootEnforceResult(
    val slept: Boolean,
    val deniedAppOp: Boolean,
    val wakeUnlocked: Boolean,
    val detail: String,
)

@Singleton
class RootWakeEnforcer @Inject constructor(
    private val rootCommandRunner: RootCommandRunner,
) {
    suspend fun enforce(
        packageName: String?,
        wakelockTag: String?,
        allowAppOpDeny: Boolean,
        displayStillOn: Boolean,
    ): RootEnforceResult {
        var slept = false
        var denied = false
        var unlocked = false
        val notes = mutableListOf<String>()

        // Never fall back to KEYCODE_POWER — it toggles and can create a wake/sleep storm.
        val sleepResult = rootCommandRunner.execute(RootCommandAllowlist.INPUT_KEYCODE_SLEEP)
        if (sleepResult.success) {
            slept = true
            notes += "sleep"
            if (displayStillOn) {
                notes += "sleep_sent_display_was_on"
            }
        } else {
            notes += "sleep_fail:${sleepResult.error}"
        }

        if (allowAppOpDeny && !packageName.isNullOrBlank()) {
            val cmd = RootCommandAllowlist.appopsTurnScreenOnIgnore(packageName)
            if (cmd != null) {
                val result = rootCommandRunner.executeCommand(cmd)
                if (result.success) {
                    denied = true
                    notes += "appops_ignore"
                } else {
                    notes += "appops_fail:${result.error}"
                }
            } else {
                notes += "appops_reject_pkg"
            }
        }

        if (!wakelockTag.isNullOrBlank()) {
            val cmd = RootCommandAllowlist.wakeUnlock(wakelockTag)
            if (cmd != null) {
                val result = rootCommandRunner.executeCommand(cmd)
                if (result.success) {
                    unlocked = true
                    notes += "wake_unlock"
                } else {
                    notes += "wake_unlock_fail"
                }
            }
        }

        Log.i(TAG, "Root enforce: ${notes.joinToString(",")}")
        return RootEnforceResult(
            slept = slept,
            deniedAppOp = denied,
            wakeUnlocked = unlocked,
            detail = notes.joinToString(","),
        )
    }

    suspend fun restoreAppOp(packageName: String): Boolean {
        val cmd = RootCommandAllowlist.appopsTurnScreenOnAllow(packageName) ?: return false
        return rootCommandRunner.executeCommand(cmd).success
    }

    companion object {
        private const val TAG = "RootWakeEnforcer"
    }
}
