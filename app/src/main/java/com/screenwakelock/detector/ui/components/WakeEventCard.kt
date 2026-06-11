package com.screenwakelock.detector.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.screenwakelock.detector.domain.attributor.AppDisplayResolver
import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.util.TimeUtils

@Composable
fun WakeEventCard(
    event: WakeEvent,
    appDisplayResolver: AppDisplayResolver,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val appName = appDisplayResolver.resolveAppName(event)
    val subtitle = appDisplayResolver.resolveSubtitle(event)
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    subtitle?.let { hint ->
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    event.displayChannel?.let { channel ->
                        Text(
                            text = channel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = TimeUtils.formatRelative(event.timestampMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                trailingContent?.invoke()
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ConfidenceIndicator(confidence = event.confidence)
                if (event.rootEnhanced) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("Root enhanced") },
                        leadingIcon = {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                        },
                    )
                }
            }
        }
    }
}
