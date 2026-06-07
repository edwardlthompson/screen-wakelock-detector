package com.screenwakelock.detector.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenwakelock.detector.service.WakeMonitorService
import com.screenwakelock.detector.ui.viewmodel.SettingsViewModel
import com.screenwakelock.detector.util.ExportUtils
import com.screenwakelock.detector.util.IntentUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigatePermissions: () -> Unit,
    onNavigateRoot: () -> Unit,
    onReplayOnboarding: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    historyViewModel: com.screenwakelock.detector.ui.viewmodel.HistoryViewModel = hiltViewModel(),
) {
    val monitoring by viewModel.monitoringEnabled.collectAsState()
    val alertEvery by viewModel.alertOnEveryWake.collectAsState()
    val thresholdAlerts by viewModel.thresholdAlertsEnabled.collectAsState()
    val events by historyViewModel.events.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Monitoring enabled") },
                    trailingContent = {
                        Switch(
                            checked = monitoring,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    viewModel.setMonitoringEnabled(enabled)
                                    if (enabled) WakeMonitorService.start(context) else WakeMonitorService.stop(context)
                                }
                            },
                        )
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Alert on every wake") },
                    trailingContent = {
                        Switch(
                            checked = alertEvery,
                            onCheckedChange = { scope.launch { viewModel.setAlertOnEveryWake(it) } },
                        )
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Threshold alerts") },
                    trailingContent = {
                        Switch(
                            checked = thresholdAlerts,
                            onCheckedChange = { scope.launch { viewModel.setThresholdAlertsEnabled(it) } },
                        )
                    },
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable { onNavigatePermissions() },
                    headlineContent = { Text("Permissions") },
                    supportingContent = { Text("Notification, usage, battery") },
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable { onNavigateRoot() },
                    headlineContent = { Text("Root access") },
                    supportingContent = { Text("Optional wakelock attribution") },
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        ExportUtils.shareCsv(context, events)
                    },
                    headlineContent = { Text("Export history") },
                    supportingContent = { Text("Share as CSV") },
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable { onReplayOnboarding() },
                    headlineContent = { Text("Replay onboarding") },
                )
            }
        }
    }
}
