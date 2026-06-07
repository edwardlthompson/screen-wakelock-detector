package com.screenwakelock.detector.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log

class WakeMonitorReceiver(
    private val callbackHolder: WakeMonitorCallbackHolder,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive action=${intent.action}")
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> callbackHolder.onScreenOn?.invoke()
            Intent.ACTION_SCREEN_OFF -> callbackHolder.onScreenOff?.invoke()
        }
    }

    companion object {
        private const val TAG = "WakeMonitor"

        fun register(context: Context, receiver: WakeMonitorReceiver) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        }

        fun unregister(context: Context, receiver: WakeMonitorReceiver) {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }
}

class WakeMonitorCallbackHolder {
    var onScreenOn: (() -> Unit)? = null
    var onScreenOff: (() -> Unit)? = null
}
