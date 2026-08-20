package com.screenwakelock.detector.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.screenwakelock.detector.domain.insights.UnknownRateSnapshot

@Composable
fun UnknownRateChip(
    snapshot: UnknownRateSnapshot,
    onGrantPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!snapshot.shouldShow()) return
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onGrantPermissions),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = snapshot.chipLabel(),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "Grant notification or usage access so wakes can be attributed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "Turn on",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
