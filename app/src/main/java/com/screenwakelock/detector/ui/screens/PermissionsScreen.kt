package com.screenwakelock.detector.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.screenwakelock.detector.data.repository.PermissionStatusRepository
import com.screenwakelock.detector.domain.model.PermissionKind
import com.screenwakelock.detector.ui.components.RestrictedSetupCard
import com.screenwakelock.detector.ui.components.openPermissionWithGuidedUnlock
import com.screenwakelock.detector.ui.hooks.usePermissionStatuses
import com.screenwakelock.detector.util.PermissionSetupGuide

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    highlight: String? = null,
    onBack: () -> Unit,
    onReplayOnboarding: () -> Unit = {},
) {
    val context = LocalContext.current
    val repo = remember { PermissionStatusRepository(context) }
    val statuses = usePermissionStatuses(repo)
    var showUnlockInstructions by remember { mutableStateOf(false) }

    if (showUnlockInstructions) {
        AlertDialog(
            onDismissRequest = { showUnlockInstructions = false },
            title = { Text("Allow restricted settings") },
            text = {
                Text(
                    "On App info → menu (⋮) → Allow restricted settings → confirm PIN. " +
                        "Then enable Notification access and Usage access.",
                )
            },
            confirmButton = {
                Button(onClick = {
                    PermissionSetupGuide.openAppInfo(context)
                    showUnlockInstructions = false
                }) {
                    Text("Open App info")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockInstructions = false }) {
                    Text("Done")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                RestrictedSetupCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            item {
                ListItem(
                    headlineContent = { Text("Run setup again") },
                    supportingContent = { Text("Replay the onboarding wizard") },
                    trailingContent = {
                        TextButton(onClick = onReplayOnboarding) {
                            Text("Open")
                        }
                    },
                )
            }
            items(statuses) { status ->
                val highlighted = highlight?.let { key ->
                    when (key) {
                        "notification_access" -> status.kind == PermissionKind.NOTIFICATION_LISTENER
                        "usage_access" -> status.kind == PermissionKind.USAGE_STATS
                        else -> false
                    }
                } ?: false
                ListItem(
                    headlineContent = { Text(status.label) },
                    supportingContent = { Text(status.description) },
                    trailingContent = {
                        Switch(
                            checked = status.granted,
                            onCheckedChange = {
                                openPermissionWithGuidedUnlock(context, status.kind) {
                                    showUnlockInstructions = true
                                }
                            },
                        )
                    },
                    modifier = if (highlighted) {
                        Modifier.padding(4.dp)
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}
