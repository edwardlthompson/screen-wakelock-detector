package com.screenwakelock.detector.wakeshield

import android.content.Context
import android.telecom.TelecomManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShieldHardExemptResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun resolve(): Set<String> {
        val dialer = runCatching {
            val tm = context.getSystemService(TelecomManager::class.java)
            tm?.defaultDialerPackage
        }.getOrNull()
        return buildSet {
            addAll(ShieldExemptPackages.STATIC_HARD_EXEMPT)
            add(context.packageName)
            if (!dialer.isNullOrBlank()) add(dialer)
        }
    }
}
