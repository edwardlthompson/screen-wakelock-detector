package com.screenwakelock.detector.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenwakelock.detector.R
import com.screenwakelock.detector.data.repository.PermissionStatusRepository
import com.screenwakelock.detector.data.repository.PreferencesRepository
import com.screenwakelock.detector.service.WakeMonitorService
import com.screenwakelock.detector.ui.viewmodel.HistoryViewModel
import com.screenwakelock.detector.ui.viewmodel.SettingsViewModel
import com.screenwakelock.detector.util.BackupUtils
import com.screenwakelock.detector.util.ExportUtils
import com.screenwakelock.detector.worker.RetentionWorker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigatePermissions: () -> Unit,
    onNavigateRoot: () -> Unit,
    onReplayOnboarding: () -> Unit,
    donateAutomation: Boolean = false,
    viewModel: SettingsViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel(),
) {
    val monitoring by viewModel.monitoringEnabled.collectAsState()
    val alertEvery by viewModel.alertOnEveryWake.collectAsState()
    val thresholdAlerts by viewModel.thresholdAlertsEnabled.collectAsState()
    val thresholdCount by viewModel.thresholdCount.collectAsState()
    val quietHours by viewModel.quietHoursEnabled.collectAsState()
    val startHour by viewModel.nighttimeStartHour.collectAsState()
    val endHour by viewModel.nighttimeEndHour.collectAsState()
    val ignoredPackages by viewModel.ignoredPackages.collectAsState()
    val retentionDays by viewModel.retentionDays.collectAsState()
    val minWakeDuration by viewModel.minWakeDurationMs.collectAsState()
    val monitorSchedule by viewModel.monitorScheduleEnabled.collectAsState()
    val pauseStartHour by viewModel.monitorPauseStartHour.collectAsState()
    val pauseEndHour by viewModel.monitorPauseEndHour.collectAsState()
    val allEvents by historyViewModel.allEventsForExport.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permissionRepo = remember { PermissionStatusRepository(context) }
    val alertsPermissionGranted = remember { permissionRepo.isPostNotificationsGranted() }
    val snackbar = remember { SnackbarHostState() }
    val linkOpenFailedMessage = stringResource(R.string.about_no_handler)
    var showExportSheet by remember { mutableStateOf(false) }
    var showAddIgnoredMenu by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var importPreview by remember { mutableStateOf<BackupUtils.ImportPreview?>(null) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = viewModel.buildBackupJson()
                    BackupUtils.writeToUri(context, uri, json)
                    snackbar.showSnackbar("Backup saved")
                }.onFailure {
                    snackbar.showSnackbar("Backup failed: ${it.message ?: "unknown error"}")
                }
            }
        }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                importPreview = BackupUtils.readImportPreview(context, uri)
                pendingImportUri = uri
            }.onFailure {
                scope.launch { snackbar.showSnackbar("Could not read backup file") }
            }
        }
    }

    importPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = {
                importPreview = null
                pendingImportUri = null
            },
            title = { Text("Import backup?") },
            text = {
                Text(
                    "Import ${preview.eventCount} wake event(s)" +
                        if (preview.settingsPresent) " and restore settings." else ".",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingImportUri
                        importPreview = null
                        pendingImportUri = null
                        if (uri != null) {
                            scope.launch {
                                runCatching {
                                    val json = BackupUtils.readUriText(context, uri)
                                    viewModel.importBackupJson(json)
                                    snackbar.showSnackbar("Backup imported")
                                }.onFailure {
                                    snackbar.showSnackbar("Import failed: ${it.message ?: "unknown error"}")
                                }
                            }
                        }
                    },
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    importPreview = null
                    pendingImportUri = null
                }) {
                    Text("Cancel")
                }
            },
        )
    }

    val recentPackages = remember(allEvents) {
        allEvents.mapNotNull { it.attributedPackage }
            .distinct()
            .filter { it !in ignoredPackages }
            .take(20)
    }

    val packageLabels = remember(allEvents, ignoredPackages) {
        ignoredPackages.associateWith { pkg ->
            allEvents.firstOrNull { it.attributedPackage == pkg }?.displayAppName ?: pkg
        }
    }

    if (showExportSheet) {
        ExportBottomSheet(
            events = allEvents,
            onDismiss = { showExportSheet = false },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
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
                    headlineContent = { Text("Monitoring pause schedule") },
                    supportingContent = {
                        Text(
                            if (monitorSchedule) {
                                "Skip logging from ${formatHour(pauseStartHour)} to ${formatHour(pauseEndHour)}"
                            } else {
                                "Pause wake logging during a daily window"
                            },
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = monitorSchedule,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    viewModel.setMonitorSchedule(enabled, pauseStartHour, pauseEndHour)
                                }
                            },
                        )
                    },
                )
            }
            if (monitorSchedule) {
                item {
                    ListItem(
                        headlineContent = { Text("Pause start") },
                        supportingContent = { Text(formatHour(pauseStartHour)) },
                    )
                    Slider(
                        value = pauseStartHour.toFloat(),
                        onValueChange = { value ->
                            scope.launch {
                                viewModel.setMonitorSchedule(monitorSchedule, value.toInt(), pauseEndHour)
                            }
                        },
                        valueRange = 0f..23f,
                        steps = 22,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Pause end") },
                        supportingContent = { Text(formatHour(pauseEndHour)) },
                    )
                    Slider(
                        value = pauseEndHour.toFloat(),
                        onValueChange = { value ->
                            scope.launch {
                                viewModel.setMonitorSchedule(monitorSchedule, pauseStartHour, value.toInt())
                            }
                        },
                        valueRange = 0f..23f,
                        steps = 22,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                    )
                }
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
            if (thresholdAlerts) {
                item {
                    ListItem(
                        headlineContent = { Text("Threshold count") },
                        supportingContent = {
                            Text("$thresholdCount wakes per hour from same app+channel")
                        },
                    )
                    Slider(
                        value = thresholdCount.toFloat(),
                        onValueChange = { scope.launch { viewModel.setThresholdCount(it.toInt()) } },
                        valueRange = 2f..10f,
                        steps = 7,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                    )
                }
            }
            item {
                ListItem(
                    headlineContent = { Text("Quiet hours") },
                    supportingContent = {
                        Text(
                            if (quietHours) {
                                "Suppress alerts from ${formatHour(startHour)} to ${formatHour(endHour)}"
                            } else {
                                "Suppress threshold and wake alerts during a custom window"
                            },
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = quietHours,
                            onCheckedChange = { scope.launch { viewModel.setQuietHoursEnabled(it) } },
                        )
                    },
                )
            }
            if (quietHours) {
                item {
                    ListItem(
                        headlineContent = { Text("Quiet hours start") },
                        supportingContent = { Text(formatHour(startHour)) },
                    )
                    Slider(
                        value = startHour.toFloat(),
                        onValueChange = { value ->
                            scope.launch { viewModel.setNighttimeHours(value.toInt(), endHour) }
                        },
                        valueRange = 0f..23f,
                        steps = 22,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Quiet hours end") },
                        supportingContent = { Text(formatHour(endHour)) },
                    )
                    Slider(
                        value = endHour.toFloat(),
                        onValueChange = { value ->
                            scope.launch { viewModel.setNighttimeHours(startHour, value.toInt()) }
                        },
                        valueRange = 0f..23f,
                        steps = 22,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                    )
                }
            }
            if (!alertsPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    ListItem(
                        modifier = Modifier.clickable {
                            context.startActivity(
                                Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                },
                            )
                        },
                        headlineContent = { Text("Wake alert notifications") },
                        supportingContent = {
                            Text("Allow notifications so wake alerts can appear")
                        },
                    )
                }
            }
            item {
                Text(
                    "Data & privacy",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                )
            }
            item {
                RetentionDropdown(
                    retentionDays = retentionDays,
                    onSelect = { days ->
                        scope.launch {
                            viewModel.setRetentionDays(days)
                            RetentionWorker.schedule(context)
                            if (days > 0) RetentionWorker.pruneNow(context)
                        }
                    },
                )
            }
            item {
                MinWakeDurationDropdown(
                    minWakeDurationMs = minWakeDuration,
                    onSelect = { ms -> scope.launch { viewModel.setMinWakeDurationMs(ms) } },
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable { showExportSheet = true },
                    headlineContent = { Text("Export history") },
                    supportingContent = { Text("Share CSV or JSON with date range") },
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        exportBackupLauncher.launch("screen-wakelock-backup.json")
                    },
                    headlineContent = { Text("Export backup") },
                    supportingContent = { Text("Save wake events and settings as JSON") },
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        importBackupLauncher.launch(arrayOf("application/json"))
                    },
                    headlineContent = { Text("Import backup") },
                    supportingContent = { Text("Restore from a local JSON backup file") },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Ignored apps") },
                    supportingContent = {
                        Text(
                            if (ignoredPackages.isEmpty()) {
                                "No apps ignored — alerts and insights include all apps"
                            } else {
                                "${ignoredPackages.size} app(s) excluded from alerts and insights"
                            },
                        )
                    },
                )
            }
            ignoredPackages.forEach { pkg ->
                item(key = "ignored-$pkg") {
                    ListItem(
                        headlineContent = { Text(packageLabels[pkg] ?: pkg) },
                        supportingContent = { Text(pkg) },
                        trailingContent = {
                            TextButton(
                                onClick = { scope.launch { viewModel.removeIgnoredPackage(pkg) } },
                            ) {
                                Text("Remove")
                            }
                        },
                    )
                }
            }
            if (recentPackages.isNotEmpty()) {
                item {
                    ExposedDropdownMenuBox(
                        expanded = showAddIgnoredMenu,
                        onExpandedChange = { showAddIgnoredMenu = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        OutlinedTextField(
                            value = "Add from recent apps",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showAddIgnoredMenu) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                        )
                        DropdownMenu(
                            expanded = showAddIgnoredMenu,
                            onDismissRequest = { showAddIgnoredMenu = false },
                        ) {
                            recentPackages.forEach { pkg ->
                                val label = allEvents.firstOrNull { it.attributedPackage == pkg }?.displayAppName ?: pkg
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        showAddIgnoredMenu = false
                                        scope.launch { viewModel.addIgnoredPackage(pkg) }
                                    },
                                )
                            }
                        }
                    }
                }
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
                    modifier = Modifier.clickable { onReplayOnboarding() },
                    headlineContent = { Text("Replay onboarding") },
                )
            }
            item {
                SettingsAboutSection(
                    donateAutomation = donateAutomation,
                    onLinkOpenFailed = {
                        scope.launch { snackbar.showSnackbar(linkOpenFailedMessage) }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportBottomSheet(
    events: List<com.screenwakelock.detector.domain.model.WakeEvent>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var preset by remember { mutableStateOf(ExportUtils.DateRangePreset.ALL) }
    var format by remember { mutableStateOf(ExportUtils.ExportFormat.CSV) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Export wake history", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Text("Date range", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            ExportUtils.DateRangePreset.entries.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { preset = option },
                ) {
                    RadioButton(selected = preset == option, onClick = { preset = option })
                    Text(option.label, modifier = Modifier.padding(start = 8.dp))
                }
            }
            Text("Format", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            ExportUtils.ExportFormat.entries.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { format = option },
                ) {
                    RadioButton(selected = format == option, onClick = { format = option })
                    Text(option.name, modifier = Modifier.padding(start = 8.dp))
                }
            }
            TextButton(
                onClick = {
                    val (start, end) = ExportUtils.presetRangeMillis(preset)
                    val filtered = ExportUtils.filterByDateRange(events, start, end)
                    ExportUtils.share(context, filtered, format)
                    onDismiss()
                },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Share export")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RetentionDropdown(
    retentionDays: Int,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = "Auto-delete old events: ${PreferencesRepository.retentionLabel(retentionDays)}",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PreferencesRepository.RETENTION_OPTIONS.forEach { days ->
                DropdownMenuItem(
                    text = { Text(PreferencesRepository.retentionLabel(days)) },
                    onClick = {
                        expanded = false
                        onSelect(days)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinWakeDurationDropdown(
    minWakeDurationMs: Int,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = "Short-wake filter: ${PreferencesRepository.minWakeDurationLabel(minWakeDurationMs)}",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PreferencesRepository.MIN_WAKE_DURATION_OPTIONS.forEach { ms ->
                DropdownMenuItem(
                    text = { Text(PreferencesRepository.minWakeDurationLabel(ms)) },
                    onClick = {
                        expanded = false
                        onSelect(ms)
                    },
                )
            }
        }
    }
}

private fun formatHour(hour: Int): String {
    val suffix = if (hour < 12) "AM" else "PM"
    val display = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$display:00 $suffix"
}
