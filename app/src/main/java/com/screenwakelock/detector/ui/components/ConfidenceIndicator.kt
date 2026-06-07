package com.screenwakelock.detector.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun ConfidenceIndicator(
    confidence: Float,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    val label = when {
        confidence >= 0.75f -> "High confidence"
        confidence >= 0.5f -> "Medium confidence"
        confidence > 0f -> "Low confidence"
        else -> "Unknown"
    }
    if (showLabel) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }
    LinearProgressIndicator(
        progress = { confidence.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label ${(confidence * 100).toInt()} percent" },
    )
}
