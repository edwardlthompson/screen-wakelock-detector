package com.screenwakelock.detector.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.screenwakelock.detector.domain.model.InsightsData

@Composable
fun InsightsShieldWeekSection(
    insights: InsightsData,
    modifier: Modifier = Modifier,
) {
    val total = insights.shieldWeekShielded + insights.shieldWeekAllowed + insights.shieldWeekOther
    if (total == 0) return
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Wake Shield this week",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Shielded ${insights.shieldWeekShielded} · allowed ${insights.shieldWeekAllowed}" +
                if (insights.shieldWeekOther > 0) " · other ${insights.shieldWeekOther}" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("${insights.shieldWeekShielded} blocked", style = MaterialTheme.typography.labelMedium)
            Text("${insights.shieldWeekAllowed} allowed", style = MaterialTheme.typography.labelMedium)
        }
    }
}
