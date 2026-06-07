package com.screenwakelock.detector.ui.screens

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenwakelock.detector.data.repository.PermissionStatusRepository
import com.screenwakelock.detector.service.WakeMonitorService
import com.screenwakelock.detector.ui.components.PermissionChip
import com.screenwakelock.detector.ui.components.RestrictedSetupCard
import com.screenwakelock.detector.ui.components.openPermissionWithGuidedUnlock
import com.screenwakelock.detector.ui.hooks.usePermissionStatuses
import com.screenwakelock.detector.ui.viewmodel.OnboardingViewModel
import com.screenwakelock.detector.util.PermissionSetupGuide
import com.screenwakelock.detector.util.RestrictedSettingsHelper
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
            if (pagerState.currentPage >= OnboardingPage.Permissions.ordinal) {
                TextButton(
                    onClick = {
                        scope.launch {
                            if (pagerState.currentPage < pages.lastIndex) {
                                pagerState.animateScrollToPage(pages.lastIndex)
                            } else {
                                viewModel.completeIntro()
                                WakeMonitorService.start(context)
                                onComplete()
                            }
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
        "Optional root adds wakelock tags via in-app libsu — never required",
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
        Text(
            "Root (optional): grant su once in Magisk or KernelSU to read wakelock holders. " +
                "All commands stay inside this app — no modules or Shizuku.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OnboardingPermissions(repo: PermissionStatusRepository) {
    val context = LocalContext.current
    val statuses = usePermissionStatuses(repo)
    var showUnlockInstructions by remember { mutableStateOf(false) }

    if (showUnlockInstructions) {
        RestrictedSettingsInstructionDialog(
            onDismiss = { showUnlockInstructions = false },
            onOpenAppInfo = {
                PermissionSetupGuide.openAppInfo(context)
                showUnlockInstructions = false
            },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Permissions", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Grant these for best attribution. Status updates when you return from Settings.",
            style = MaterialTheme.typography.bodyMedium,
        )
        RestrictedSetupCard()
        if (!RestrictedSettingsHelper.needsUnlock(context)) {
            Text(
                "Enable Notifications for this app if you want wake alerts when the screen turns on.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        statuses.forEach { status ->
            PermissionChip(
                label = status.label,
                granted = status.granted,
                onClick = {
                    openPermissionWithGuidedUnlock(context, status.kind) {
                        showUnlockInstructions = true
                    }
                },
            )
        }
    }
}

@Composable
private fun OnboardingVerify(repo: PermissionStatusRepository) {
    val statuses = usePermissionStatuses(repo)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Verify setup", style = MaterialTheme.typography.headlineSmall)
        statuses.forEach { status ->
            PermissionChip(label = status.label, granted = status.granted)
        }
        Text(
            "Monitoring will start when you tap Get started. You can grant permissions later in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "On Samsung, Xiaomi, and OnePlus devices, also disable battery restrictions for reliable monitoring.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RestrictedSettingsInstructionDialog(
    onDismiss: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Allow restricted settings") },
        text = {
            Text(
                "On the App info screen for Screen Wakelock Detector:\n\n" +
                    "• Tap the menu (⋮) in the top-right corner\n" +
                    "• Tap Allow restricted settings\n" +
                    "• Confirm with your PIN or fingerprint\n\n" +
                    "Then return here — permission status updates automatically.",
            )
        },
        confirmButton = {
            Button(onClick = onOpenAppInfo) {
                Text("Open App info")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}
