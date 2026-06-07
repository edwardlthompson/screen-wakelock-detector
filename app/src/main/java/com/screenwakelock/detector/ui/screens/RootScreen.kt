package com.screenwakelock.detector.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenwakelock.detector.root.RootAvailabilityState
import com.screenwakelock.detector.ui.viewmodel.RootViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScreen(
    onBack: () -> Unit,
    viewModel: RootViewModel = hiltViewModel(),
) {
    val rootEnabled by viewModel.rootEnabled.collectAsState()
    var probeState by remember { mutableStateOf<RootAvailabilityState?>(null) }
    var diagnostics by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val isRootAvailable = probeState?.isRooted == true
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(Unit) {
        probeState = viewModel.probe()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Root access") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Requires root — all tooling is built into this app. No modules or Shizuku needed.",
                style = MaterialTheme.typography.bodyMedium,
                color = mutedColor,
            )
            if (!isRootAvailable) {
                Text(
                    "Root not detected. Enable root attribution is disabled until su is available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedColor,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Enable root attribution",
                    color = if (isRootAvailable) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        mutedColor
                    },
                )
                Switch(
                    checked = rootEnabled && isRootAvailable,
                    enabled = isRootAvailable,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            if (enabled) {
                                val state = viewModel.probe()
                                probeState = state
                                viewModel.setRootEnabled(state.isRooted)
                            } else {
                                viewModel.setRootEnabled(false)
                            }
                        }
                    },
                )
            }
            probeState?.let { state ->
                Text(
                    state.diagnostics,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isRooted) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        mutedColor
                    },
                )
            }
            Button(
                onClick = {
                    scope.launch {
                        probeState = viewModel.probe()
                        val result = viewModel.runDiagnostics()
                        diagnostics = if (result.success) {
                            "Last command OK (${result.durationMs}ms)"
                        } else {
                            "Failed: ${result.error ?: "Root not available"}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Run diagnostics")
            }
            diagnostics?.let {
                Text(it, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
