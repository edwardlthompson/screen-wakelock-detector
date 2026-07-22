package com.screenwakelock.detector.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.screenwakelock.detector.R
import com.screenwakelock.detector.service.NotificationCaptureService
import com.screenwakelock.detector.wakeshield.LockScreenActor
import com.screenwakelock.detector.wakeshield.ShieldAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun LazyListScope.wakeShieldSettingsItems(
    shieldEnabled: Boolean,
    wakeForensics: Boolean,
    rootEnabled: Boolean,
    shieldRootKill: Boolean,
    allowlist: Set<String>,
    denied: Set<String>,
    recentPackages: List<String>,
    packageLabel: (String) -> String,
    scope: CoroutineScope,
    onSetShieldEnabled: suspend (Boolean) -> Unit,
    onSetForensics: suspend (Boolean) -> Unit,
    onSetRootKill: suspend (Boolean) -> Unit,
    onAddAllowlist: suspend (String) -> Unit,
    onRemoveAllowlist: suspend (String) -> Unit,
    onUndoDenied: suspend (String) -> Unit,
    onPanic: suspend () -> Unit,
) {
    item {
        Text(
            stringResource(R.string.shield_section_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    item {
        ListItem(
            headlineContent = { Text(stringResource(R.string.shield_master_title)) },
            supportingContent = { Text(stringResource(R.string.shield_master_summary)) },
            trailingContent = {
                Switch(
                    checked = shieldEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { onSetShieldEnabled(enabled) }
                    },
                )
            },
        )
    }
    item {
        var showRootKillConfirm by remember { mutableStateOf(false) }
        ListItem(
            headlineContent = { Text(stringResource(R.string.shield_root_kill_title)) },
            supportingContent = { Text(stringResource(R.string.shield_root_kill_summary)) },
            trailingContent = {
                Switch(
                    checked = shieldRootKill,
                    enabled = rootEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            showRootKillConfirm = true
                        } else {
                            scope.launch { onSetRootKill(false) }
                        }
                    },
                )
            },
        )
        if (showRootKillConfirm) {
            AlertDialog(
                onDismissRequest = { showRootKillConfirm = false },
                title = { Text(stringResource(R.string.shield_root_kill_confirm_title)) },
                text = { Text(stringResource(R.string.shield_root_kill_confirm_body)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showRootKillConfirm = false
                            scope.launch { onSetRootKill(true) }
                        },
                    ) { Text(stringResource(R.string.shield_confirm_enable)) }
                },
                dismissButton = {
                    TextButton(onClick = { showRootKillConfirm = false }) {
                        Text(stringResource(R.string.shield_confirm_cancel))
                    }
                },
            )
        }
    }
    item {
        ListItem(
            headlineContent = { Text(stringResource(R.string.shield_forensics_title)) },
            supportingContent = { Text(stringResource(R.string.shield_forensics_summary)) },
            trailingContent = {
                Switch(
                    checked = wakeForensics,
                    onCheckedChange = { enabled ->
                        scope.launch { onSetForensics(enabled) }
                    },
                )
            },
        )
    }
    item { ShieldCapabilityRows() }
    item {
        val context = LocalContext.current
        ListItem(
            headlineContent = { Text(stringResource(R.string.shield_accessibility_title)) },
            supportingContent = {
                Text(
                    if (ShieldAccessibilityService.isConnected()) {
                        stringResource(R.string.shield_accessibility_summary_on)
                    } else {
                        stringResource(R.string.shield_accessibility_summary_off)
                    },
                )
            },
            modifier = Modifier.fillMaxWidth(),
            trailingContent = {
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                ) { Text(stringResource(R.string.shield_accessibility_title)) }
            },
        )
    }
    item {
        val context = LocalContext.current
        ListItem(
            headlineContent = { Text(stringResource(R.string.shield_fsi_settings)) },
            supportingContent = { Text(stringResource(R.string.shield_fsi_settings_summary)) },
            modifier = Modifier.fillMaxWidth(),
            trailingContent = {
                TextButton(
                    onClick = {
                        val intent = if (Build.VERSION.SDK_INT >= 34) {
                            Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT")
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                        }
                        runCatching {
                            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    },
                ) { Text(stringResource(R.string.shield_fsi_settings)) }
            },
        )
    }
    item {
        Text(
            stringResource(R.string.shield_allowlist_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    if (allowlist.isEmpty()) {
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.shield_allowlist_empty)) },
            )
        }
    } else {
        allowlist.sorted().forEach { pkg ->
            item(key = "shield-allow-$pkg") {
                ListItem(
                    headlineContent = { Text(packageLabel(pkg)) },
                    supportingContent = { Text(pkg) },
                    trailingContent = {
                        TextButton(onClick = { scope.launch { onRemoveAllowlist(pkg) } }) {
                            Text(stringResource(R.string.shield_confirm_cancel))
                        }
                    },
                )
            }
        }
    }
    item {
        var expanded by remember { mutableStateOf(false) }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            TextButton(onClick = { expanded = true }) {
                Text(stringResource(R.string.shield_allowlist_add))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                recentPackages.filter { it !in allowlist }.take(20).forEach { pkg ->
                    DropdownMenuItem(
                        text = { Text(packageLabel(pkg)) },
                        onClick = {
                            expanded = false
                            scope.launch { onAddAllowlist(pkg) }
                        },
                    )
                }
            }
        }
    }
    if (denied.isNotEmpty()) {
        item {
            Text(
                stringResource(R.string.shield_denied_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        denied.sorted().forEach { pkg ->
            item(key = "shield-deny-$pkg") {
                ListItem(
                    headlineContent = { Text(packageLabel(pkg)) },
                    trailingContent = {
                        TextButton(onClick = { scope.launch { onUndoDenied(pkg) } }) {
                            Text(stringResource(R.string.shield_denied_undo))
                        }
                    },
                )
            }
        }
    }
    item {
        ListItem(
            headlineContent = { Text(stringResource(R.string.shield_panic_title)) },
            supportingContent = { Text(stringResource(R.string.shield_panic_summary)) },
            trailingContent = {
                TextButton(onClick = { scope.launch { onPanic() } }) {
                    Text(stringResource(R.string.shield_panic_action))
                }
            },
        )
    }
}

@Composable
private fun ShieldCapabilityRows() {
    val listenerReady = NotificationCaptureService.isListenerBound()
    val a11yReady = LockScreenActor.isAvailable()
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            stringResource(
                R.string.shield_capability_listener,
                if (listenerReady) {
                    stringResource(R.string.shield_ready)
                } else {
                    stringResource(R.string.shield_missing)
                },
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            stringResource(
                R.string.shield_capability_a11y,
                if (a11yReady) {
                    stringResource(R.string.shield_ready)
                } else {
                    stringResource(R.string.shield_missing)
                },
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
