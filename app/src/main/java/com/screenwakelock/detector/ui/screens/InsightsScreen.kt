package com.screenwakelock.detector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenwakelock.detector.domain.insights.InsightsCalculator
import com.screenwakelock.detector.ui.viewmodel.InsightsViewModel
import com.screenwakelock.detector.util.TimeUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onFilterHour: (Int) -> Unit = {},
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val events by viewModel.events.collectAsState()
    val startHour by viewModel.nighttimeStart.collectAsState()
    val endHour by viewModel.nighttimeEnd.collectAsState()
    val insights = remember(events, startHour, endHour) {
        InsightsCalculator.compute(events, startHour, endHour)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Insights") }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard("Total wakes", insights.totalWakes.toString(), Modifier.weight(1f))
                    StatCard("Nighttime", insights.nighttimeWakes.toString(), Modifier.weight(1f))
                }
            }
            item {
                Text("Top offenders", style = MaterialTheme.typography.titleMedium)
            }
            if (insights.topOffenders.isEmpty()) {
                item {
                    Text(
                        "No attributed wakes yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(insights.topOffenders) { offender ->
                    ListItem(
                        headlineContent = {
                            Text(offender.appLabel ?: offender.packageName)
                        },
                        supportingContent = {
                            Text(
                                "${offender.count} wakes · ${offender.nighttimeCount} at night",
                            )
                        },
                    )
                }
            }
            if (insights.recurringPatterns.isNotEmpty()) {
                item {
                    Text("Recurring patterns", style = MaterialTheme.typography.titleMedium)
                }
                items(insights.recurringPatterns) { pattern ->
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
                        }
                    }
                }
            }
            item {
                Text("7-day heatmap", style = MaterialTheme.typography.titleMedium)
                HeatmapGrid(cells = insights.heatmap, onCellClick = onFilterHour)
            }
        }
    }
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
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = if (count == 0) 0.1f else 0.2f + intensity * 0.8f,
                                ),
                            )
                            .clickable(enabled = count > 0) { onCellClick(hour) },
                    )
                }
            }
        }
    }
}
