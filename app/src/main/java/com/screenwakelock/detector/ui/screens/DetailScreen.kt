package com.screenwakelock.detector.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenwakelock.detector.ui.components.ConfidenceIndicator
import com.screenwakelock.detector.ui.viewmodel.DetailViewModel
import com.screenwakelock.detector.util.IntentUtils
import com.screenwakelock.detector.util.SilenceWake
import com.screenwakelock.detector.util.TimeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    wakeEventId: Long,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val eventFlow = androidx.compose.runtime.remember(wakeEventId) {
        viewModel.observeEvent(wakeEventId)
    }
    val event by eventFlow.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wake detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (event == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            ) {
                Text("Loading…")
            }
        } else {
            val e = event!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(e.displayAppName, style = MaterialTheme.typography.headlineSmall)
                    e.displayChannel?.let {
                        Text(it, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        TimeUtils.formatDateTime(e.timestampMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    ConfidenceIndicator(confidence = e.confidence)
                    Text(
                        e.reasonCode.friendlyLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (e.wakelockTag != null) {
                    item {
                        Text(
                            "Wakelock: ${e.wakelockTag}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                if (e.attributedPackage != null) {
                    item {
                        Button(
                            onClick = {
                                val result = SilenceWake.silence(e)
                                SilenceWake.openSettings(context, e)
                                scope.launch {
                                    val message = SilenceWake.snackbarMessage(result, e.displayAppName)
                                    val snackResult = snackbar.showSnackbar(
                                        message = message,
                                        actionLabel = "Undo",
                                    )
                                    if (snackResult == SnackbarResult.ActionPerformed) {
                                        SilenceWake.openSettings(context, e)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Silence channel")
                        }
                        OutlinedButton(
                            onClick = {
                                val pkg = e.attributedPackage!!
                                val intent = if (e.channelId != null && IntentUtils.canOpenChannelSettings()) {
                                    IntentUtils.channelNotificationSettings(pkg, e.channelId)
                                } else {
                                    IntentUtils.appNotificationSettings(pkg)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Open notification settings")
                        }
                        OutlinedButton(
                            onClick = {
                                context.startActivity(IntentUtils.appDetailsSettings(e.attributedPackage!!))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("App info")
                        }
                    }
                }
                if (e.isLowConfidence || e.candidates.size > 1) {
                    item {
                        Text(
                            "Why this app?",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            "Ranked candidates when attribution is uncertain:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(e.candidates) { candidate ->
                        ListItem(
                            headlineContent = {
                                Text(candidate.appLabel ?: candidate.packageName)
                            },
                            supportingContent = {
                                Text(
                                    "${candidate.reasonCode.friendlyLabel()} · " +
                                        "${(candidate.confidence * 100).toInt()}%",
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
