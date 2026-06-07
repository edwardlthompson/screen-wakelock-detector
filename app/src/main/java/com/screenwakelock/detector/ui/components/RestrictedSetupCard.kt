package com.screenwakelock.detector.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.screenwakelock.detector.util.PermissionSetupGuide
import com.screenwakelock.detector.util.RestrictedSettingsHelper

@Composable
fun RestrictedSetupCard(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showInstructions by remember { mutableStateOf(false) }
    var showSaiFallback by remember { mutableStateOf(false) }
    val needsUnlock = RestrictedSettingsHelper.needsUnlock(context)

    if (!needsUnlock && !showSaiFallback) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Unlock permissions (sideloaded app)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "Android blocks Notification and Usage access until you allow restricted settings for this app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "1. Tap Start — try enabling Notification access (you may see a blocked message).\n" +
                    "2. Open App info → menu (⋮) → Allow restricted settings → confirm PIN.\n" +
                    "3. Return here — status updates automatically — then grant Notification and Usage access.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Button(
                onClick = {
                    PermissionSetupGuide.openNotificationListenerSettings(context)
                    showInstructions = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Start setup")
            }
            TextButton(
                onClick = {
                    PermissionSetupGuide.openAppInfo(context)
                    showInstructions = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open App info")
            }
            TextButton(
                onClick = { showSaiFallback = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("OnePlus: menu missing?")
            }
        }
    }

    if (showInstructions) {
        AlertDialog(
            onDismissRequest = { showInstructions = false },
            title = { Text("Allow restricted settings") },
            text = {
                Text(
                    "On the App info screen for Screen Wakelock Detector:\n\n" +
                        "• Tap the menu (⋮) in the top-right corner\n" +
                        "• Tap Allow restricted settings\n" +
                        "• Confirm with your PIN or fingerprint\n\n" +
                        "Then tap Back to return here and enable Notification access and Usage access.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        PermissionSetupGuide.openAppInfo(context)
                        showInstructions = false
                    },
                ) {
                    Text("Open App info")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstructions = false }) {
                    Text("Done")
                }
            },
        )
    }

    if (showSaiFallback) {
        AlertDialog(
            onDismissRequest = { showSaiFallback = false },
            title = { Text("OnePlus / OxygenOS workaround") },
            text = {
                Text(
                    "Some OnePlus devices hide Allow restricted settings when the APK was installed from Files or a browser.\n\n" +
                        "1. Install SAI (Split APKs Installer) from Google Play\n" +
                        "2. Uninstall this app\n" +
                        "3. Reinstall the same APK using SAI\n" +
                        "4. Grant Notification access and Usage access again",
                )
            },
            confirmButton = {
                Button(onClick = { PermissionSetupGuide.openReleaseDownload(context) }) {
                    Text("Get APK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaiFallback = false }) {
                    Text("Close")
                }
            },
        )
    }
}

/** Opens guided unlock first when restricted; otherwise opens the permission settings directly. */
fun openPermissionWithGuidedUnlock(
    context: android.content.Context,
    kind: com.screenwakelock.detector.domain.model.PermissionKind,
    showInstructions: () -> Unit,
) {
    if (RestrictedSettingsHelper.needsUnlock(context)) {
        when (kind) {
            com.screenwakelock.detector.domain.model.PermissionKind.NOTIFICATION_LISTENER,
            com.screenwakelock.detector.domain.model.PermissionKind.USAGE_STATS,
            -> {
                PermissionSetupGuide.openNotificationListenerSettings(context)
                showInstructions()
            }
            else -> PermissionSetupGuide.openPermissionSettings(context, kind)
        }
    } else {
        PermissionSetupGuide.openPermissionSettings(context, kind)
    }
}
