package com.screenwakelock.detector.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.screenwakelock.detector.data.repository.PermissionStatusRepository
import com.screenwakelock.detector.domain.model.PermissionKind
import com.screenwakelock.detector.domain.model.PermissionStatus

@Composable
fun MissingPermissionsBanner(
    onNavigatePermissions: (highlight: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repo = remember { PermissionStatusRepository(context) }
    var missing by remember { mutableStateOf(repo.getAllStatuses().filter { !it.granted }) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                missing = repo.getAllStatuses().filter { !it.granted }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (missing.isEmpty()) return

    val highlight = missing.firstHighlightKey()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${missing.size} permission(s) off",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = "Turn on recommended permissions to identify which apps wake your screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = { onNavigatePermissions(highlight) }) {
                Text("Turn on")
            }
        }
    }
}

private fun List<PermissionStatus>.firstHighlightKey(): String? = when {
    any { it.kind == PermissionKind.NOTIFICATION_LISTENER && !it.granted } -> "notification_access"
    any { it.kind == PermissionKind.USAGE_STATS && !it.granted } -> "usage_access"
    else -> null
}
