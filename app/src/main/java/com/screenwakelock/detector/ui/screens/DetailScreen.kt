package com.screenwakelock.detector.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenwakelock.detector.domain.model.ReasonCode
import com.screenwakelock.detector.domain.model.WakeEventIdentity
import com.screenwakelock.detector.root.RootAttributor
import com.screenwakelock.detector.ui.components.ConfidenceIndicator
import com.screenwakelock.detector.ui.components.MissingPermissionsBanner
import com.screenwakelock.detector.ui.components.rememberAppDisplayResolver
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
    onNavigatePermissions: (String?) -> Unit = {},
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val eventFlow = androidx.compose.runtime.remember(wakeEventId) {
        viewModel.observeEvent(wakeEventId)
    }
    val event by eventFlow.collectAsState()
    val ignoredPackages by viewModel.ignoredPackages.collectAsState()
    val appDisplayResolver = rememberAppDisplayResolver()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var similarWakes by remember { mutableStateOf<List<com.screenwakelock.detector.domain.model.WakeEvent>>(emptyList()) }
    var similarExpanded by remember { mutableStateOf(false) }
    var rootTimeline by remember { mutableStateOf<List<com.screenwakelock.detector.domain.model.WakeEvent>>(emptyList()) }
    var rootTimelineExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(event?.id) {
        val current = event ?: return@LaunchedEffect
        similarWakes = viewModel.loadSimilarWakes(current)
        rootTimeline = viewModel.loadRootTimeline(current)
    }

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
                if (e.reasonCode == ReasonCode.NOTIFICATION_FULL_SCREEN) {
                    item {
                        FullScreenIntentBanner(
                            onOpenSettings = {
                                val pkg = e.attributedPackage
                                val intent = if (pkg != null && e.channelId != null &&
                                    IntentUtils.canOpenChannelSettings()
                                ) {
                                    IntentUtils.channelNotificationSettings(pkg, e.channelId)
                                } else if (pkg != null) {
                                    IntentUtils.appNotificationSettings(pkg)
                                } else {
                                    Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                                }
                                context.startActivity(intent)
                            },
                        )
                    }
                }
                if (e.isLowConfidence || e.attributedPackage == null) {
                    item {
                        MissingPermissionsBanner(onNavigatePermissions = onNavigatePermissions)
                    }
                }
                item {
                    val appName = appDisplayResolver.resolveAppName(e)
                    Text(appName, style = MaterialTheme.typography.headlineSmall)
                    appDisplayResolver.resolveSubtitle(e)?.let { subtitle ->
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    e.displayChannel?.let {
                        Text(it, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        TimeUtils.formatDateTime(e.timestampMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    e.screenOffDurationMs?.let { duration ->
                        Text(
                            "Screen was off for ${duration / 1000}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
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
                if (e.rootEnhanced) {
                    item {
                        val parserLabel = RootAttributor.parserDisplayName(e.rootParserId)
                        Text(
                            parserLabel?.let { "Matched via $it" }
                                ?: "Root-enhanced attribution",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (rootTimeline.isNotEmpty()) {
                    item {
                        OutlinedButton(
                            onClick = { rootTimelineExpanded = !rootTimelineExpanded },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (rootTimelineExpanded) {
                                    "Hide root timeline (${rootTimeline.size})"
                                } else {
                                    "Root timeline (${rootTimeline.size} recent)"
                                },
                            )
                        }
                    }
                    if (rootTimelineExpanded) {
                        items(rootTimeline, key = { it.id }) { rootEvent ->
                            ListItem(
                                headlineContent = {
                                    Text(TimeUtils.formatDateTime(rootEvent.timestampMillis))
                                },
                                supportingContent = {
                                    Text(
                                        listOfNotNull(
                                            rootEvent.wakelockTag,
                                            RootAttributor.parserDisplayName(rootEvent.rootParserId),
                                        ).joinToString(" · "),
                                    )
                                },
                            )
                        }
                    }
                }
                if (similarWakes.isNotEmpty()) {
                    item {
                        OutlinedButton(
                            onClick = { similarExpanded = !similarExpanded },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (similarExpanded) {
                                    "Hide similar wakes (${similarWakes.size})"
                                } else {
                                    "Similar wakes (${similarWakes.size} in last 7 days)"
                                },
                            )
                        }
                    }
                    if (similarExpanded) {
                        items(similarWakes, key = { it.id }) { similar ->
                            ListItem(
                                headlineContent = { Text(TimeUtils.formatDateTime(similar.timestampMillis)) },
                                supportingContent = {
                                    Text(similar.reasonCode.friendlyLabel())
                                },
                            )
                        }
                    }
                }
                WakeEventIdentity.effectivePackage(e)?.let { pkg ->
                    item {
                        Button(
                            onClick = {
                                val result = SilenceWake.silence(e)
                                SilenceWake.openSettings(context, e)
                                scope.launch {
                                    val message = SilenceWake.snackbarMessage(
                                        result,
                                        appDisplayResolver.resolveAppName(e),
                                    )
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
                                context.startActivity(IntentUtils.appDetailsSettings(pkg))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("App info")
                        }
                        if (pkg !in ignoredPackages) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        viewModel.ignoreApp(pkg)
                                        val appName = appDisplayResolver.resolveAppName(e)
                                        val snackResult = snackbar.showSnackbar(
                                            message = "Ignored $appName — hidden from History; remove in Settings",
                                            actionLabel = "Undo",
                                        )
                                        if (snackResult == SnackbarResult.ActionPerformed) {
                                            viewModel.unignoreApp(pkg)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Ignore this app")
                            }
                        } else {
                            Text(
                                text = "This app is ignored — alerts and insights skip it",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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
                        val candidateIgnored = candidate.packageName in ignoredPackages
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
                            trailingContent = {
                                if (!candidateIgnored && e.attributedPackage != candidate.packageName) {
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                viewModel.ignoreApp(candidate.packageName)
                                                val label = candidate.appLabel ?: candidate.packageName
                                                val snackResult = snackbar.showSnackbar(
                                                    message = "Ignored $label",
                                                    actionLabel = "Undo",
                                                )
                                                if (snackResult == SnackbarResult.ActionPerformed) {
                                                    viewModel.unignoreApp(candidate.packageName)
                                                }
                                            }
                                        },
                                    ) {
                                        Text("Ignore")
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenIntentBanner(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        Text(
            "Full-screen intent",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            "This wake used a full-screen intent notification, which can turn on the display " +
                "even when the phone is locked. Review notification settings for this app.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Open notification settings")
        }
    }
}
