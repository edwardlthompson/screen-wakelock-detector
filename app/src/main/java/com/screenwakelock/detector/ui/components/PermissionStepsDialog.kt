package com.screenwakelock.detector.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.screenwakelock.detector.util.IntentUtils
import com.screenwakelock.detector.util.PermissionSettingsGuide
import com.screenwakelock.detector.util.PermissionSetupGuide

@Composable
fun PermissionStepsDialog(
    guide: PermissionSettingsGuide,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var showInstallerWorkaround by remember { mutableStateOf(false) }

    if (showInstallerWorkaround) {
        InstallerWorkaroundDialog(onDismiss = { showInstallerWorkaround = false })
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(guide.title) },
        text = {
            Column {
                guide.steps.forEachIndexed { index, step ->
                    Text(
                        text = "${index + 1}. $step",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                if (guide.showInstallerWorkaround) {
                    TextButton(onClick = { showInstallerWorkaround = true }) {
                        Text("Installer workaround")
                    }
                }
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        IntentUtils.startFirstResolvable(context, guide.intents)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(guide.confirmLabel)
                }
                if (guide.secondaryLabel != null && guide.secondaryIntents.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            IntentUtils.startFirstResolvable(context, guide.secondaryIntents)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(guide.secondaryLabel)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun InstallerWorkaroundDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Installer workaround") },
        text = {
            Text(
                "Some devices hide Allow restricted settings when the APK was installed from Files or a browser.\n\n" +
                    "1. Install SAI (Split APKs Installer) from a trusted source\n" +
                    "2. Uninstall this app\n" +
                    "3. Reinstall the same APK using SAI (session install)\n" +
                    "4. Grant Notification access and Usage access again",
            )
        },
        confirmButton = {
            Button(onClick = { PermissionSetupGuide.openReleaseDownload(context) }) {
                Text("Get APK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
fun rememberPermissionGuideState(): PermissionGuideState {
    var activeGuide by remember { mutableStateOf<PermissionSettingsGuide?>(null) }
    return remember {
        PermissionGuideState(
            activeGuide = { activeGuide },
            showGuide = { activeGuide = it },
            dismissGuide = { activeGuide = null },
        )
    }
}

class PermissionGuideState internal constructor(
    private val activeGuide: () -> PermissionSettingsGuide?,
    private val showGuide: (PermissionSettingsGuide) -> Unit,
    private val dismissGuide: () -> Unit,
) {
    val guide: PermissionSettingsGuide? get() = activeGuide()

    fun show(guide: PermissionSettingsGuide) = showGuide(guide)

    fun dismiss() = dismissGuide()
}

@Composable
fun PermissionGuideHost(state: PermissionGuideState) {
    val guide = state.guide
    if (guide != null) {
        PermissionStepsDialog(
            guide = guide,
            onDismiss = { state.dismiss() },
        )
    }
}
