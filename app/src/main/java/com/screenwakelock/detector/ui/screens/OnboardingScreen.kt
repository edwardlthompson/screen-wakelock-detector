package com.screenwakelock.detector.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.screenwakelock.detector.service.WakeMonitorService
import com.screenwakelock.detector.ui.components.PermissionGuideHost
import com.screenwakelock.detector.ui.components.PermissionGuideState
import com.screenwakelock.detector.ui.components.PermissionSetupRow
import com.screenwakelock.detector.ui.components.rememberPermissionGuideState
import com.screenwakelock.detector.ui.hooks.usePermissionStatuses
import com.screenwakelock.detector.ui.viewmodel.OnboardingViewModel
import com.screenwakelock.detector.util.PermissionSetupGuide
import com.screenwakelock.detector.util.RestrictedSettingsHelper
import com.screenwakelock.detector.util.SettingsOpenResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pages = listOf(OnboardingPage.Intro, OnboardingPage.Permissions)
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val permissionRepo = remember { PermissionStatusRepository(context) }
    val guideState = rememberPermissionGuideState()

    PermissionGuideHost(guideState)

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
                OnboardingPage.Intro -> OnboardingIntro()
                OnboardingPage.Permissions -> OnboardingPermissions(permissionRepo, guideState)
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
            if (pagerState.currentPage >= OnboardingPage.Permissions.ordinal) {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.completeIntro()
                            WakeMonitorService.start(context)
                            onComplete()
                        }
                    },
                ) {
                    Text("Skip")
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

private enum class OnboardingPage { Intro, Permissions }

@Composable
private fun OnboardingIntro() {
    val bullets = listOf(
        "Detect screen-on events in the background",
        "Match notifications to each wake",
        "Show timestamped history with confidence",
        "Open settings or mute repeat offenders",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Find out what keeps waking your screen",
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            "We log every screen-on locally, identify the likely app or channel, and help you fix it in one tap.",
            style = MaterialTheme.typography.bodyMedium,
        )
        bullets.forEachIndexed { index, bullet ->
            Text(
                "${index + 1}. $bullet",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            "All data stays on your device — no account, no cloud, no Internet permission. " +
                "We read notification metadata only, never message text.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OnboardingPermissions(
    repo: PermissionStatusRepository,
    guideState: PermissionGuideState,
) {
    val context = LocalContext.current
    val statuses = usePermissionStatuses(repo)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Permissions", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Grant these for best attribution. Status updates when you return from Settings.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!RestrictedSettingsHelper.needsUnlock(context)) {
            Text(
                "On Samsung, Xiaomi, and OnePlus, also set Battery to Unrestricted for reliable monitoring.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        statuses.forEach { status ->
            PermissionSetupRow(
                label = status.label,
                granted = status.granted,
                shortRationale = status.shortRationale,
                onClick = {
                    when (val result = PermissionSetupGuide.openWithFallback(context, status.kind)) {
                        is SettingsOpenResult.Opened -> Unit
                        is SettingsOpenResult.ShowManualSteps -> guideState.show(result.guide)
                    }
                },
            )
        }
    }
}
