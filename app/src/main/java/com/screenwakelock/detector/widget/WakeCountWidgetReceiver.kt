package com.screenwakelock.detector.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WakeCountWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = WakeCountWidget()

    companion object {
        fun requestUpdate(context: Context) {
            CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
                WakeCountWidget.requestUpdate(context.applicationContext)
            }
        }
    }
}
