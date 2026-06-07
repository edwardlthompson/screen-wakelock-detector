package com.screenwakelock.detector.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.screenwakelock.detector.data.repository.PermissionStatusRepository
import com.screenwakelock.detector.domain.model.PermissionKind
import com.screenwakelock.detector.ui.components.PermissionGuideHost
import com.screenwakelock.detector.ui.components.rememberPermissionGuideState
import com.screenwakelock.detector.ui.hooks.usePermissionStatuses
import com.screenwakelock.detector.util.PermissionSetupGuide
import com.screenwakelock.detector.util.SettingsOpenResult

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
    val guideState = rememberPermissionGuideState()

    PermissionGuideHost(guideState)

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
                        "restricted_settings" -> status.kind == PermissionKind.RESTRICTED_SETTINGS
                        "notification_access" -> status.kind == PermissionKind.NOTIFICATION_LISTENER
                        "usage_access" -> status.kind == PermissionKind.USAGE_STATS
                        else -> false
                    }
                } ?: false
                ListItem(
                    headlineContent = { Text(status.label) },
                    supportingContent = {
                        Text(
                            if (highlighted) {
                                "${status.shortRationale} (highlighted)"
                            } else {
                                status.shortRationale
                            },
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = status.granted,
                            onCheckedChange = {
                                when (val result = PermissionSetupGuide.openWithFallback(context, status.kind)) {
                                    is SettingsOpenResult.Opened -> Unit
                                    is SettingsOpenResult.ShowManualSteps -> guideState.show(result.guide)
                                }
                            },
                        )
                    },
                )
            }
        }
    }
}
