package com.screenwakelock.detector.ui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning

@Composable
fun PermissionChip(
    label: String,
    granted: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    AssistChip(
        onClick = onClick,
        modifier = modifier,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = if (granted) "Granted" else "Not granted",
                tint = if (granted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (granted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    )
}
