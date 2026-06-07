package com.screenwakelock.detector.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.screenwakelock.detector.MainActivity
import com.screenwakelock.detector.data.db.AppDatabase
import com.screenwakelock.detector.data.db.toDomain
import com.screenwakelock.detector.util.IntentUtils
import com.screenwakelock.detector.util.TimeUtils

class WakeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getInstance(context)
        val entity = db.wakeEventDao().getLatestOnce()
        val event = entity?.toDomain()
        val label = event?.let {
            "${it.displayAppName} · ${TimeUtils.formatRelative(it.timestampMillis)}"
        } ?: "No wakes yet"
        val activityComponent = ComponentName(context, MainActivity::class.java)
        provideContent {
            WakeWidgetContent(label = label, activityComponent = activityComponent)
        }
    }

    companion object {
        suspend fun requestUpdate(context: Context) {
            WakeWidget().updateAll(context)
        }
    }
}

@Composable
private fun WakeWidgetContent(
    label: String,
    activityComponent: ComponentName,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(android.graphics.Color.parseColor("#E8F4F6")))
            .padding(12.dp)
            .clickable(actionStartActivity(activityComponent)),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Text(text = "Last wake", style = TextStyle(fontSize = 12.sp))
        Text(text = label, style = TextStyle(fontSize = 14.sp))
        Text(
            text = "Fix it →",
            style = TextStyle(fontSize = 12.sp),
            modifier = GlanceModifier.padding(top = 4.dp),
        )
    }
}
