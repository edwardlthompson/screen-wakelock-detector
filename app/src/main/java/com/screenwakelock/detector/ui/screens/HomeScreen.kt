package com.screenwakelock.detector.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenwakelock.detector.ui.components.QuickFixBottomSheet
import com.screenwakelock.detector.ui.components.WakeEventCard
import com.screenwakelock.detector.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateHistory: () -> Unit,
    onNavigateDetail: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val latest by viewModel.latestWake.collectAsState()
    var showQuickFix by remember { mutableStateOf(false) }

    QuickFixBottomSheet(
        event = latest,
        visible = showQuickFix,
        onDismiss = { showQuickFix = false },
        onWhyThisApp = onNavigateDetail,
        onMuteChannel = { showQuickFix = false },
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Home") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
                    onClick = { showQuickFix = true },
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
