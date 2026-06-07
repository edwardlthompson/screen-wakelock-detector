package com.screenwakelock.detector.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.ui.components.QuickFixBottomSheet
import com.screenwakelock.detector.ui.components.WakeEventCard
import com.screenwakelock.detector.ui.viewmodel.HistoryViewModel
import com.screenwakelock.detector.util.IntentUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateDetail: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val events by viewModel.events.collectAsState()
    val query by viewModel.query.collectAsState()
    val nightOnly by viewModel.nightOnly.collectAsState()
    var searchActive by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var quickFixEvent by remember { mutableStateOf<WakeEvent?>(null) }

    QuickFixBottomSheet(
        event = quickFixEvent,
        visible = quickFixEvent != null,
        onDismiss = { quickFixEvent = null },
        onWhyThisApp = onNavigateDetail,
        onMuteChannel = { quickFixEvent = null },
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("History") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No screen wakes logged yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                SearchBar(
                    query = query,
                    onQueryChange = viewModel::setQuery,
                    onSearch = { searchActive = false },
                    active = searchActive,
                    onActiveChange = { searchActive = it },
                    placeholder = { Text("Search app or channel") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) {}
                FilterChip(
                    selected = nightOnly,
                    onClick = { viewModel.setNightOnly(!nightOnly) },
                    label = { Text("Night only (11pm–6am)") },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                items(events, key = { it.id }) { event ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            when (value) {
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    event.attributedPackage?.let { pkg ->
                                        context.startActivity(IntentUtils.appNotificationSettings(pkg))
                                    }
                                    true
                                }
                                SwipeToDismissBoxValue.EndToStart -> {
                                    quickFixEvent = event
                                    true
                                }
                                else -> false
                            }
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = event.attributedPackage != null,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            val direction = dismissState.dismissDirection
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = when (direction) {
                                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                    else -> Alignment.Center
                                },
                            ) {
                                Icon(
                                    imageVector = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Settings
                                        else -> Icons.Default.Delete
                                    },
                                    contentDescription = null,
                                )
                            }
                        },
                    ) {
                        WakeEventCard(
                            event = event,
                            onClick = { onNavigateDetail(event.id) },
                            trailingContent = {
                                IconButton(onClick = { quickFixEvent = event }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Quick fix")
                                }
                            },
                        )
                    }
                }
            }
            }
        }
    }
}
