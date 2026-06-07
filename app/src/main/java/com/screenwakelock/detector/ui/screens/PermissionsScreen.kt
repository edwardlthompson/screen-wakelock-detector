package com.screenwakelock.detector.ui.screens

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.screenwakelock.detector.data.repository.PermissionStatusRepository
import com.screenwakelock.detector.domain.model.PermissionKind
import com.screenwakelock.detector.util.IntentUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    highlight: String? = null,
    onBack: () -> Unit,
    onReplayOnboarding: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repo = remember { PermissionStatusRepository(context) }
    val statuses = remember { mutableStateOf(repo.getAllStatuses()) }

    fun refreshStatuses() {
        statuses.value = repo.getAllStatuses()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStatuses()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Run setup again") },
                    supportingContent = { Text("Replay the onboarding wizard") },
                    trailingContent = {
                        TextButton(onClick = onReplayOnboarding) {
                            Text("Open")
                        }
                    },
                )
            }
            items(statuses.value) { status ->
                val highlighted = highlight?.let { key ->
                    when (key) {
                        "notification_access" -> status.kind == PermissionKind.NOTIFICATION_LISTENER
                        "usage_access" -> status.kind == PermissionKind.USAGE_STATS
                        else -> false
                    }
                } ?: false
                ListItem(
                    headlineContent = { Text(status.label) },
                    supportingContent = { Text(status.description) },
                    trailingContent = {
                        Switch(
                            checked = status.granted,
                            onCheckedChange = {
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
                    },
                    modifier = if (highlighted) {
                        Modifier.padding(4.dp)
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}
