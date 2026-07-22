package com.screenwakelock.detector.wakeshield

import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory rails for cooldown and self-wake suppression.
 */
@Singleton
class ShieldRuntimeState @Inject constructor() {
    private val lastEnforcementAt = AtomicLong(0L)
    private val selfWakeUntil = AtomicLong(0L)

    fun markEnforcement(now: Long = System.currentTimeMillis()) {
        lastEnforcementAt.set(now)
        selfWakeUntil.set(now + ShieldPolicy.SELF_WAKE_SUPPRESS_MS)
    }

    fun inCooldown(now: Long = System.currentTimeMillis()): Boolean =
        now - lastEnforcementAt.get() < ShieldPolicy.COOLDOWN_MS

    fun isSelfWakeWindow(now: Long = System.currentTimeMillis()): Boolean =
        now < selfWakeUntil.get()

    fun clear() {
        lastEnforcementAt.set(0L)
        selfWakeUntil.set(0L)
    }
}
