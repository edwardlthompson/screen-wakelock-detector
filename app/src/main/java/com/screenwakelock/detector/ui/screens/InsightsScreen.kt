package com.screenwakelock.detector.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenwakelock.detector.domain.insights.InsightsCalculator
import com.screenwakelock.detector.domain.model.OffenderSummary
import com.screenwakelock.detector.domain.model.RecurringPattern
import com.screenwakelock.detector.ui.viewmodel.InsightsViewModel
import com.screenwakelock.detector.ui.modifiers.highRefreshScroll
import com.screenwakelock.detector.util.ChannelMuter
import com.screenwakelock.detector.util.ExportUtils
import com.screenwakelock.detector.util.IntentUtils
import com.screenwakelock.detector.util.SilenceWake
import com.screenwakelock.detector.util.TimeUtils
import java.util.Calendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InsightsScreen(
    onFilterHour: (Int) -> Unit = {},
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val events by viewModel.events.collectAsState()
    val startHour by viewModel.nighttimeStart.collectAsState()
    val endHour by viewModel.nighttimeEnd.collectAsState()
    val ignored by viewModel.ignoredPackages.collectAsState()
    val nightIgnored by viewModel.nightIgnoredPackages.collectAsState()
    val nightlyBudgets by viewModel.nightlyBudgets.collectAsState()
    val insights = remember(events, startHour, endHour, ignored, nightIgnored) {
        InsightsCalculator.compute(events, startHour, endHour, ignored, nightIgnored)
    }
    val isWide = LocalConfiguration.current.screenWidthDp >= 840
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var batchMuteTarget by remember { mutableStateOf<OffenderSummary?>(null) }
    var budgetTarget by remember { mutableStateOf<OffenderSummary?>(null) }
    var budgetInput by remember { mutableStateOf("") }

    fun onIgnoreOffender(offender: OffenderSummary) {
        scope.launch {
            viewModel.ignoreApp(offender.packageName)
            val label = offender.appLabel ?: offender.packageName
            snackbar.showSnackbar("Ignored $label — hidden from History; remove in Settings")
        }
    }

    fun onNightIgnoreOffender(offender: OffenderSummary) {
        scope.launch {
            viewModel.nightIgnoreApp(offender.packageName)
            val label = offender.appLabel ?: offender.packageName
            snackbar.showSnackbar("Night-only ignore for $label — hidden during quiet hours")
        }
    }

    fun onNeverShieldOffender(offender: OffenderSummary) {
        scope.launch {
            viewModel.neverShieldApp(offender.packageName)
            val label = offender.appLabel ?: offender.packageName
            snackbar.showSnackbar("Never shield $label — Wake Shield will allow it")
        }
    }

    fun onNightOnlyShieldOffender(offender: OffenderSummary) {
        scope.launch {
            viewModel.nightOnlyShield(offender.packageName)
            val label = offender.appLabel ?: offender.packageName
            snackbar.showSnackbar("Shield $label only at night")
        }
    }

    fun onExportOffender(offender: OffenderSummary) {
        val subset = events.filter { it.attributedPackage == offender.packageName }
        ExportUtils.shareCsv(context, subset)
    }

    budgetTarget?.let { offender ->
        AlertDialog(
            onDismissRequest = { budgetTarget = null },
            title = { Text("Nightly wake budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Alert when ${offender.appLabel ?: offender.packageName} " +
                            "exceeds this many wakes in one night.",
                    )
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Max wakes per night") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val max = budgetInput.toIntOrNull() ?: 0
                        scope.launch {
                            if (max > 0) {
                                viewModel.setNightlyBudget(offender.packageName, max)
                                snackbar.showSnackbar("Budget set to $max wakes/night")
                            } else {
                                viewModel.removeNightlyBudget(offender.packageName)
                                snackbar.showSnackbar("Budget removed")
                            }
                        }
                        budgetTarget = null
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { budgetTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    batchMuteTarget?.let { offender ->
        AlertDialog(
            onDismissRequest = { batchMuteTarget = null },
            title = { Text("Batch mute channels?") },
            text = {
                Text(
                    "Dismiss active notifications and open settings for all channels from " +
                        "${offender.appLabel ?: offender.packageName}. " +
                        "Some OEMs block muting other apps programmatically.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pkg = offender.packageName
                        val channels = events
                            .filter { it.attributedPackage == pkg }
                            .mapNotNull { it.channelId }
                            .distinct()
                        var dismissed = 0
                        if (channels.isEmpty()) {
                            dismissed = ChannelMuter.silenceNotifications(pkg, null).dismissedCount
                            ChannelMuter.openBestSettings(context, pkg, null)
                        } else {
                            channels.forEach { channelId ->
                                dismissed += ChannelMuter
                                    .silenceNotifications(pkg, channelId)
                                    .dismissedCount
                            }
                            ChannelMuter.openBestSettings(context, pkg, channels.first())
                        }
                        batchMuteTarget = null
                        scope.launch {
                            val message = if (dismissed > 0) {
                                "Dismissed $dismissed notification(s) — open settings to finish"
                            } else {
                                "Open settings to mute ${offender.appLabel ?: pkg}"
                            }
                            snackbar.showSnackbar(message)
                        }
                    },
                ) {
                    Text("Mute")
                }
            },
            dismissButton = {
                TextButton(onClick = { batchMuteTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Insights") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StatsRow(insights)
                    MonthLine(events)
                    if (insights.topOffenders.size >= 2) {
                        val a = insights.topOffenders[0]
                        val b = insights.topOffenders[1]
                        Text(
                            "${a.appLabel ?: a.packageName} ${a.count} vs ${b.appLabel ?: b.packageName} ${b.count}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    InsightsShieldWeekSection(insights)
                    TopOffendersSection(
                        offenders = insights.topOffenders,
                        nightlyBudgets = nightlyBudgets,
                        onBatchMute = { batchMuteTarget = it },
                        onSetBudget = { offender ->
                            budgetInput = nightlyBudgets[offender.packageName]?.toString() ?: ""
                            budgetTarget = offender
                        },
                        onIgnoreApp = ::onIgnoreOffender,
                        onNightIgnoreApp = ::onNightIgnoreOffender,
                        onNeverShield = ::onNeverShieldOffender,
                        onNightOnlyShield = ::onNightOnlyShieldOffender,
                        onExport = ::onExportOffender,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    RecurringPatternsSection(
                        patterns = insights.recurringPatterns,
                        onMute = { pattern ->
                            val event = events.firstOrNull {
                                it.attributedPackage == pattern.packageName &&
                                    it.channelId == pattern.channelId
                            } ?: return@RecurringPatternsSection
                            SilenceWake.silence(event)
                            SilenceWake.openSettings(context, event)
                        },
                        onOpenSettings = { pattern ->
                            val intent = if (pattern.channelId != null &&
                                IntentUtils.canOpenChannelSettings()
                            ) {
                                IntentUtils.channelNotificationSettings(
                                    pattern.packageName,
                                    pattern.channelId,
                                )
                            } else {
                                IntentUtils.appNotificationSettings(pattern.packageName)
                            }
                            context.startActivity(intent)
                        },
                    )
                    HeatmapSection(
                        cells = insights.heatmap,
                        onCellClick = onFilterHour,
                        nightStartHour = startHour,
                        nightEndHour = endHour,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .highRefreshScroll(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { StatsRow(insights) }
                item { MonthLine(events) }
                if (insights.topOffenders.size >= 2) {
                    item {
                        val a = insights.topOffenders[0]
                        val b = insights.topOffenders[1]
                        Text(
                            "${a.appLabel ?: a.packageName} ${a.count} vs ${b.appLabel ?: b.packageName} ${b.count}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                item { InsightsShieldWeekSection(insights) }
                item { Text("Top offenders", style = MaterialTheme.typography.titleMedium) }
                if (insights.topOffenders.isEmpty()) {
                    item {
                        Text(
                            "No attributed wakes yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(insights.topOffenders) { offender ->
                        TopOffenderRow(
                            offender = offender,
                            nightlyBudget = nightlyBudgets[offender.packageName],
                            onBatchMute = { batchMuteTarget = offender },
                            onSetBudget = {
                                budgetInput = nightlyBudgets[offender.packageName]?.toString() ?: ""
                                budgetTarget = offender
                            },
                            onIgnoreApp = { onIgnoreOffender(offender) },
                            onNightIgnoreApp = { onNightIgnoreOffender(offender) },
                            onNeverShield = { onNeverShieldOffender(offender) },
                            onNightOnlyShield = { onNightOnlyShieldOffender(offender) },
                            onExport = { onExportOffender(offender) },
                        )
                    }
                }
                if (insights.recurringPatterns.isNotEmpty()) {
                    item { Text("Recurring patterns", style = MaterialTheme.typography.titleMedium) }
                    items(insights.recurringPatterns) { pattern ->
                        PatternCard(
                            pattern = pattern,
                            onMute = {
                                val event = events.firstOrNull {
                                    it.attributedPackage == pattern.packageName &&
                                        it.channelId == pattern.channelId
                                } ?: return@PatternCard
                                SilenceWake.silence(event)
                                SilenceWake.openSettings(context, event)
                            },
                            onOpenSettings = {
                                val intent = if (pattern.channelId != null &&
                                    IntentUtils.canOpenChannelSettings()
                                ) {
                                    IntentUtils.channelNotificationSettings(
                                        pattern.packageName,
                                        pattern.channelId,
                                    )
                                } else {
                                    IntentUtils.appNotificationSettings(pattern.packageName)
                                }
                                context.startActivity(intent)
                            },
                        )
                    }
                }
                item {
                    HeatmapSection(
                        cells = insights.heatmap,
                        onCellClick = onFilterHour,
                        nightStartHour = startHour,
                        nightEndHour = endHour,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(insights: com.screenwakelock.detector.domain.model.InsightsData) {
    val wowLabel = formatWeekOverWeek(insights)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatCard("Total wakes", insights.totalWakes.toString(), Modifier.weight(1f))
        StatCard("Nighttime", insights.nighttimeWakes.toString(), Modifier.weight(1f))
        StatCard("Week over week", wowLabel, Modifier.weight(1f))
    }
}

@Composable
private fun MonthLine(events: List<com.screenwakelock.detector.domain.model.WakeEvent>) {
    Text(
        "Last 30 days: ${com.screenwakelock.detector.domain.insights.TonightStats.monthCount(events)} wakes",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun formatWeekOverWeek(insights: com.screenwakelock.detector.domain.model.InsightsData): String {
    val delta = insights.weekOverWeekDeltaPercent
    return when {
        delta == null && insights.weekOverWeekCurrent > 0 -> "${insights.weekOverWeekCurrent} (new)"
        delta == null -> "${insights.weekOverWeekCurrent}"
        else -> {
            val sign = if (delta >= 0) "+" else ""
            "$sign${delta.toInt()}%"
        }
    }
}

@Composable
private fun TopOffendersSection(
    offenders: List<OffenderSummary>,
    nightlyBudgets: Map<String, Int>,
    onBatchMute: (OffenderSummary) -> Unit,
    onSetBudget: (OffenderSummary) -> Unit,
    onIgnoreApp: (OffenderSummary) -> Unit,
    onNightIgnoreApp: (OffenderSummary) -> Unit,
    onNeverShield: (OffenderSummary) -> Unit,
    onNightOnlyShield: (OffenderSummary) -> Unit,
    onExport: (OffenderSummary) -> Unit,
) {
    Text("Top offenders", style = MaterialTheme.typography.titleMedium)
    if (offenders.isEmpty()) {
        Text("No attributed wakes yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        offenders.forEach { offender ->
            TopOffenderRow(
                offender = offender,
                nightlyBudget = nightlyBudgets[offender.packageName],
                onBatchMute = onBatchMute,
                onSetBudget = onSetBudget,
                onIgnoreApp = { onIgnoreApp(offender) },
                onNightIgnoreApp = { onNightIgnoreApp(offender) },
                onNeverShield = { onNeverShield(offender) },
                onNightOnlyShield = { onNightOnlyShield(offender) },
                onExport = { onExport(offender) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopOffenderRow(
    offender: OffenderSummary,
    nightlyBudget: Int?,
    onBatchMute: (OffenderSummary) -> Unit,
    onSetBudget: (OffenderSummary) -> Unit,
    onIgnoreApp: () -> Unit,
    onNightIgnoreApp: () -> Unit,
    onNeverShield: () -> Unit,
    onNightOnlyShield: () -> Unit,
    onExport: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = {},
            onLongClick = { onBatchMute(offender) },
        ),
        headlineContent = { Text(offender.appLabel ?: offender.packageName) },
        supportingContent = {
            val budgetNote = nightlyBudget?.let { " · budget $it/night" }.orEmpty()
            Text(
                "${offender.channelName ?: "Unknown channel"} · " +
                    "${offender.count} wakes · ${offender.nighttimeCount} at night$budgetNote",
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Set nightly budget") },
                        onClick = {
                            menuExpanded = false
                            onSetBudget(offender)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Batch mute channels") },
                        onClick = {
                            menuExpanded = false
                            onBatchMute(offender)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Ignore this app") },
                        onClick = {
                            menuExpanded = false
                            onIgnoreApp()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Night-only ignore") },
                        onClick = {
                            menuExpanded = false
                            onNightIgnoreApp()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Never shield") },
                        onClick = {
                            menuExpanded = false
                            onNeverShield()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Shield only at night") },
                        onClick = {
                            menuExpanded = false
                            onNightOnlyShield()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Export this app") },
                        onClick = {
                            menuExpanded = false
                            onExport()
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun RecurringPatternsSection(
    patterns: List<RecurringPattern>,
    onMute: (RecurringPattern) -> Unit,
    onOpenSettings: (RecurringPattern) -> Unit,
) {
    if (patterns.isEmpty()) return
    Text("Recurring patterns", style = MaterialTheme.typography.titleMedium)
    patterns.forEach { pattern ->
        PatternCard(pattern = pattern, onMute = { onMute(pattern) }, onOpenSettings = { onOpenSettings(pattern) })
    }
}

@Composable
private fun PatternCard(
    pattern: RecurringPattern,
    onMute: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                pattern.appLabel ?: pattern.packageName,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "${pattern.consecutiveNights} nights · ${pattern.nightCount} wakes",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onMute) { Text("Mute channel") }
                OutlinedButton(onClick = onOpenSettings) { Text("Open settings") }
            }
        }
    }
}

@Composable
private fun HeatmapSection(
    cells: List<com.screenwakelock.detector.domain.model.HeatmapCell>,
    onCellClick: (Int) -> Unit,
    nightStartHour: Int,
    nightEndHour: Int,
) {
    Text(
        "7-day heatmap — night window $nightStartHour:00–$nightEndHour:00 shaded",
        style = MaterialTheme.typography.titleMedium,
    )
    HeatmapGrid(
        cells = cells,
        onCellClick = onCellClick,
        nightStartHour = nightStartHour,
        nightEndHour = nightEndHour,
    )
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun HeatmapGrid(
    cells: List<com.screenwakelock.detector.domain.model.HeatmapCell>,
    onCellClick: (Int) -> Unit,
    nightStartHour: Int,
    nightEndHour: Int,
) {
    val maxCount = cells.maxOfOrNull { it.count } ?: 1
    val days = listOf("S", "M", "T", "W", "T", "F", "S")
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(modifier = Modifier.size(24.dp))
            (0..23).forEach { hour ->
                if (hour % 6 == 0) {
                    Text(
                        "$hour",
                        modifier = Modifier.size(width = 24.dp, height = 16.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        (Calendar.SUNDAY..Calendar.SATURDAY).forEach { day ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    days[(day - 1).coerceIn(0, 6)],
                    modifier = Modifier.size(24.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
                (0..23).forEach { hour ->
                    val count = cells.find { it.dayOfWeek == day && it.hourOfDay == hour }?.count ?: 0
                    val intensity = count.toFloat() / maxCount
                    val nightHour = if (nightStartHour > nightEndHour) {
                        hour >= nightStartHour || hour < nightEndHour
                    } else {
                        hour >= nightStartHour && hour < nightEndHour
                    }
                    val color = if (nightHour) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color.copy(
                                    alpha = if (count == 0) {
                                        if (nightHour) 0.18f else 0.1f
                                    } else {
                                        0.2f + intensity * 0.8f
                                    },
                                ),
                            )
                            .clickable(enabled = count > 0) { onCellClick(hour) },
                    )
                }
            }
        }
    }
}
