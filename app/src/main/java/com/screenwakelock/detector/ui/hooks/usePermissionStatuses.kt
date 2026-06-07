package com.screenwakelock.detector.ui.hooks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.screenwakelock.detector.data.repository.PermissionStatusRepository
import com.screenwakelock.detector.domain.model.PermissionStatus

@Composable
fun usePermissionStatuses(
    repo: PermissionStatusRepository,
): List<PermissionStatus> {
    var statuses by remember { mutableStateOf(repo.getAllStatuses()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, repo) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                statuses = repo.getAllStatuses()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return statuses
}

@Composable
fun useRestrictedSetupNeeded(
    needsUnlock: () -> Boolean,
): Boolean {
    var needed by remember { mutableStateOf(needsUnlock()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                needed = needsUnlock()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return needed
}
