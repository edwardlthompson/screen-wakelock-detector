package com.screenwakelock.detector.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenwakelock.detector.R
import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.ui.components.MissingPermissionsBanner
import com.screenwakelock.detector.ui.components.QuickFixBottomSheet
import com.screenwakelock.detector.ui.components.UnknownRateChip
import com.screenwakelock.detector.ui.components.WakeEventCard
import com.screenwakelock.detector.ui.components.rememberAppDisplayResolver
import com.screenwakelock.detector.ui.viewmodel.HomeViewModel
import com.screenwakelock.detector.updates.ProductUpdate
import com.screenwakelock.detector.wakeshield.ShieldAccessibilityService
import com.screenwakelock.detector.wakeshield.ShieldPolicy
import com.screenwakelock.detector.util.ChannelMuter
import com.screenwakelock.detector.util.IntentUtils
import com.screenwakelock.detector.util.SilenceWake
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateHistory: () -> Unit,
    onNavigateHistoryNight: () -> Unit = onNavigateHistory,
    onNavigateDetail: (Long) -> Unit,
    onNavigatePermissions: (String?) -> Unit,
    onReplayOnboarding: () -> Unit = {},
    deepLinkQuickFixWakeId: Long? = null,
    onDeepLinkConsumed: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val latest by viewModel.latestWake.collectAsState()
    val ignoredPackages by viewModel.ignoredPackages.collectAsState()
    val unknownRate by viewModel.unknownRate.collectAsState()
    val shieldEnabled by viewModel.shieldEnabled.collectAsState()
    val tonight by viewModel.tonight.collectAsState()
    val grantImpact by viewModel.grantImpact.collectAsState()
    val windDown by viewModel.windDownEnabled.collectAsState()
    val appDisplayResolver = rememberAppDisplayResolver()
    val healthScore = viewModel.permissionHealthScore
    var showQuickFix by remember { mutableStateOf(false) }
    var quickFixEvent by remember { mutableStateOf<WakeEvent?>(null) }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val donateFailedMessage = stringResource(R.string.about_no_handler)
    var menuExpanded by remember { mutableStateOf(false) }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val lastEnforcementAt = tonight.lastEnforcements.maxOfOrNull { it.timestampMillis } ?: 0L
    val inCooldown = lastEnforcementAt > 0L &&
        nowMillis - lastEnforcementAt < ShieldPolicy.COOLDOWN_MS
    val inSelfWake = lastEnforcementAt > 0L &&
        nowMillis - lastEnforcementAt < ShieldPolicy.SELF_WAKE_SUPPRESS_MS

    LaunchedEffect(lastEnforcementAt) {
        if (lastEnforcementAt <= 0L) return@LaunchedEffect
        while (true) {
            nowMillis = System.currentTimeMillis()
            if (nowMillis - lastEnforcementAt >= ShieldPolicy.COOLDOWN_MS) break
            delay(1_000)
        }
    }

    LaunchedEffect(deepLinkQuickFixWakeId, latest) {
        when (deepLinkQuickFixWakeId) {
            null -> Unit
            0L -> {
                latest?.let {
                    quickFixEvent = it
                    showQuickFix = true
                }
                onDeepLinkConsumed()
            }
            else -> {
                val event = viewModel.loadEvent(deepLinkQuickFixWakeId)
                if (event != null) {
                    quickFixEvent = event
                    showQuickFix = true
                }
                onDeepLinkConsumed()
            }
        }
    }

    fun onMuted(event: WakeEvent, result: ChannelMuter.MuteResult) {
        scope.launch {
            val message = SilenceWake.snackbarMessage(result, appDisplayResolver.resolveAppName(event))
            val snackResult = snackbar.showSnackbar(message = message, actionLabel = "Undo")
            if (snackResult == SnackbarResult.ActionPerformed) {
                SilenceWake.openSettings(context, event)
            }
        }
    }

    fun onIgnored(event: WakeEvent, packageName: String) {
        scope.launch {
            viewModel.ignoreApp(packageName)
            val appName = appDisplayResolver.resolveAppName(event)
            val snackResult = snackbar.showSnackbar(
                message = "Ignored $appName — alerts and insights will skip this app",
                actionLabel = "Undo",
            )
            if (snackResult == SnackbarResult.ActionPerformed) {
                viewModel.unignoreApp(packageName)
            }
        }
    }

    QuickFixBottomSheet(
        event = quickFixEvent ?: latest.takeIf { showQuickFix },
        visible = showQuickFix && (quickFixEvent ?: latest) != null,
        ignoredPackages = ignoredPackages,
        appDisplayResolver = appDisplayResolver,
        onDismiss = {
            showQuickFix = false
            quickFixEvent = null
        },
        onWhyThisApp = onNavigateDetail,
        onMuteChannel = { event, result ->
            showQuickFix = false
            quickFixEvent = null
            onMuted(event, result)
        },
        onIgnoreApp = { event, pkg -> onIgnored(event, pkg) },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.home_menu))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_permissions)) },
                            onClick = {
                                menuExpanded = false
                                onNavigatePermissions(null)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_replay_onboarding)) },
                            onClick = {
                                menuExpanded = false
                                onReplayOnboarding()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.about_donate)) },
                            onClick = {
                                menuExpanded = false
                                if (!IntentUtils.viewDonateUri(context, ProductUpdate.DONATION_URL)) {
                                    scope.launch { snackbar.showSnackbar(donateFailedMessage) }
                                }
                            },
                        )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MissingPermissionsBanner(onNavigatePermissions = onNavigatePermissions)
            if (shieldEnabled && !ShieldAccessibilityService.isConnected()) {
                Text(
                    text = stringResource(R.string.home_a11y_lost),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clickable { onNavigatePermissions("accessibility") }
                        .semantics {
                            contentDescription = "Accessibility lost. Open permissions to restore Shield."
                        },
                )
            }
            grantImpact?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onNavigatePermissions(null) },
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateHistoryNight() }
                    .semantics {
                        contentDescription = "Tonight ${tonight.count} wakes. Open night history."
                    },
            ) {
                Text(
                    text = stringResource(R.string.home_tonight_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${tonight.count} wakes" +
                        (tonight.topLabel ?: tonight.topPackage)?.let { " · $it" }.orEmpty() +
                        if (tonight.overBudget) " · over typical night" else "",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (windDown) {
                    Text("Wind-down on", style = MaterialTheme.typography.labelSmall)
                }
                Text(
                    text = "7-day " + tonight.dailyCounts.joinToString("·"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (tonight.lastEnforcements.isNotEmpty()) {
                    Text(
                        "Last shield actions: ${tonight.lastEnforcements.size}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (inCooldown) {
                    Text(
                        "Shield cooldown — next block waits a few seconds",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                if (inSelfWake) {
                    Text(
                        "Self-wake window — Shield is ignoring our own lock",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            UnknownRateChip(
                snapshot = unknownRate,
                onGrantPermissions = { onNavigatePermissions("notification_access") },
            )
            if (healthScore < 100) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigatePermissions(null) },
                ) {
                    Text(
                        text = "Permission health: $healthScore%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LinearProgressIndicator(
                        progress = { healthScore / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                }
            }
            Text(
                text = "Last screen wake",
                style = MaterialTheme.typography.headlineSmall,
            )
            if (latest == null) {
                Text(
                    text = "No wakes recorded yet. Monitoring runs in the background.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                WakeEventCard(
                    event = latest!!,
                    appDisplayResolver = appDisplayResolver,
                    onClick = { onNavigateDetail(latest!!.id) },
                    shieldArmed = shieldEnabled,
                )
                Button(
                    onClick = {
                        quickFixEvent = latest
                        showQuickFix = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Fix it")
                }
            }
            Button(onClick = onNavigateHistory, modifier = Modifier.fillMaxWidth()) {
                Text("View full history")
            }
        }
    }
}
