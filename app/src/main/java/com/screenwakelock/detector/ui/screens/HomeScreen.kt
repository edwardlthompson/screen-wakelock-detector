package com.screenwakelock.detector.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.ui.components.MissingPermissionsBanner
import com.screenwakelock.detector.ui.components.QuickFixBottomSheet
import com.screenwakelock.detector.ui.components.WakeEventCard
import com.screenwakelock.detector.ui.viewmodel.HomeViewModel
import com.screenwakelock.detector.util.ChannelMuter
import com.screenwakelock.detector.util.SilenceWake
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateHistory: () -> Unit,
    onNavigateDetail: (Long) -> Unit,
    onNavigatePermissions: (String?) -> Unit,
    deepLinkQuickFixWakeId: Long? = null,
    onDeepLinkConsumed: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val latest by viewModel.latestWake.collectAsState()
    val healthScore = viewModel.permissionHealthScore
    var showQuickFix by remember { mutableStateOf(false) }
    var quickFixEvent by remember { mutableStateOf<WakeEvent?>(null) }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
            val message = SilenceWake.snackbarMessage(result, event.displayAppName)
            val snackResult = snackbar.showSnackbar(message = message, actionLabel = "Undo")
            if (snackResult == SnackbarResult.ActionPerformed) {
                SilenceWake.openSettings(context, event)
            }
        }
    }

    QuickFixBottomSheet(
        event = quickFixEvent ?: latest.takeIf { showQuickFix },
        visible = showQuickFix && (quickFixEvent ?: latest) != null,
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
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Home") }) },
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
                    onClick = { onNavigateDetail(latest!!.id) },
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
