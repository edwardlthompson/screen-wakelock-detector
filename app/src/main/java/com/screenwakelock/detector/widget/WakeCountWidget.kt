package com.screenwakelock.detector.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.screenwakelock.detector.R
import com.screenwakelock.detector.MainActivity
import com.screenwakelock.detector.data.db.AppDatabase
import com.screenwakelock.detector.data.db.toDomain
import com.screenwakelock.detector.util.TimeUtils

class WakeCountWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getInstance(context)
        val allEvents = db.wakeEventDao().getAll().map { it.toDomain() }
        val tonightKey = TimeUtils.nightKey(System.currentTimeMillis())
        val tonightEvents = allEvents.filter {
            TimeUtils.nightKey(it.timestampMillis) == tonightKey
        }
        val count = tonightEvents.size
        val topOffender = tonightEvents
            .filter { it.attributedPackage != null }
            .groupBy { it.attributedPackage }
            .maxByOrNull { it.value.size }
            ?.value
            ?.firstOrNull()
        val offenderLabel = topOffender?.displayAppName ?: "None yet"
        val homeIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = android.net.Uri.parse("screenwakelock://insights")
        }
        provideContent {
            WakeCountWidgetContent(
                count = count,
                topOffender = offenderLabel,
                homeIntent = homeIntent,
            )
        }
    }

    companion object {
        suspend fun requestUpdate(context: Context) {
            WakeCountWidget().updateAll(context)
        }
    }
}

@Composable
private fun WakeCountWidgetContent(
    count: Int,
    topOffender: String,
    homeIntent: Intent,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(R.color.widget_background)
            .padding(12.dp)
            .clickable(actionStartActivity(homeIntent)),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Text(text = "Tonight", style = TextStyle(fontSize = 12.sp))
        Text(text = "$count wakes", style = TextStyle(fontSize = 18.sp))
        Text(
            text = "Top: $topOffender",
            style = TextStyle(fontSize = 12.sp),
        )
    }
}
