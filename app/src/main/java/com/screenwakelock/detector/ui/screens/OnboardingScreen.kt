package com.screenwakelock.detector.ui.screens

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenwakelock.detector.data.repository.PermissionStatusRepository
import com.screenwakelock.detector.domain.model.PermissionKind
import com.screenwakelock.detector.service.WakeMonitorService
import com.screenwakelock.detector.ui.components.PermissionChip
import com.screenwakelock.detector.ui.viewmodel.OnboardingViewModel
import com.screenwakelock.detector.util.IntentUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pages = listOf(
        OnboardingPage.Welcome,
        OnboardingPage.HowItWorks,
        OnboardingPage.Privacy,
        OnboardingPage.Permissions,
        OnboardingPage.Verify,
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val permissionRepo = remember { PermissionStatusRepository(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LinearProgressIndicator(
            progress = { (pagerState.currentPage + 1f) / pages.size },
            modifier = Modifier.fillMaxWidth(),
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            when (pages[page]) {
                OnboardingPage.Welcome -> OnboardingWelcome()
                OnboardingPage.HowItWorks -> OnboardingHowItWorks()
                OnboardingPage.Privacy -> OnboardingPrivacy()
                OnboardingPage.Permissions -> OnboardingPermissions(permissionRepo)
                OnboardingPage.Verify -> OnboardingVerify(permissionRepo)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (pagerState.currentPage > 0) {
                TextButton(onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) {
                    Text("Back")
                }
            } else {
                TextButton(onClick = {}) { Text("") }
            }
            Button(
                onClick = {
                    if (pagerState.currentPage < pages.lastIndex) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        scope.launch {
                            viewModel.completeIntro()
                            WakeMonitorService.start(context)
                            onComplete()
                        }
                    }
                },
            ) {
                Text(if (pagerState.currentPage < pages.lastIndex) "Next" else "Get started")
            }
        }
    }
}

private enum class OnboardingPage { Welcome, HowItWorks, Privacy, Permissions, Verify }

@Composable
private fun OnboardingWelcome() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Welcome", style = MaterialTheme.typography.displayLarge)
        Text(
            "Screen Wakelock Detector logs when your screen turns on and identifies which app likely caused it.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun OnboardingHowItWorks() {
    val steps = listOf(
        "Monitor listens for screen-on events locally",
        "Notifications posted nearby are matched to the wake",
        "Usage stats provide fallback when no notification matches",
        "Optional root adds wakelock details — never required",
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("How it works", style = MaterialTheme.typography.headlineSmall)
        steps.forEachIndexed { index, step ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${index + 1}. $step",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun OnboardingPrivacy() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Privacy", style = MaterialTheme.typography.headlineSmall)
        Text(
            "All processing stays on your device. We never collect notification message bodies, " +
                "contacts, or send data over the network. This app has no Internet permission.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "What we store: app name, notification channel metadata, and timestamps.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OnboardingPermissions(repo: PermissionStatusRepository) {
    val context = LocalContext.current
    val statuses = remember { repo.getAllStatuses() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Permissions", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Grant these for best attribution. You can change them anytime in Settings.",
            style = MaterialTheme.typography.bodyMedium,
        )
        statuses.forEach { status ->
            PermissionChip(
                label = status.label,
                granted = status.granted,
                onClick = {
                    val intent = when (status.kind) {
                        PermissionKind.NOTIFICATION_LISTENER ->
                            IntentUtils.notificationListenerSettings()
                        PermissionKind.USAGE_STATS ->
                            IntentUtils.usageAccessSettings()
                        PermissionKind.POST_NOTIFICATIONS -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                            } else null
                        }
                        PermissionKind.BATTERY_OPTIMIZATION ->
                            IntentUtils.requestIgnoreBatteryOptimizations(context)
                    }
                    intent?.let { context.startActivity(it) }
                },
            )
        }
    }
}

@Composable
private fun OnboardingVerify(repo: PermissionStatusRepository) {
    val statuses = remember { repo.getAllStatuses() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Verify setup", style = MaterialTheme.typography.headlineSmall)
        statuses.forEach { status ->
            PermissionChip(label = status.label, granted = status.granted)
        }
        Text(
            "Monitoring will start when you tap Get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
