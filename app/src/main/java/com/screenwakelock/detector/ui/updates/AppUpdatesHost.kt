package com.screenwakelock.detector.ui.updates

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenwakelock.detector.R
import com.screenwakelock.detector.ui.viewmodel.AppUpdatesViewModel
import com.screenwakelock.detector.updates.AppUpdates
import com.screenwakelock.detector.updates.ProductUpdate
import com.screenwakelock.detector.util.IntentUtils

@Composable
fun AppUpdatesHost(
    enabled: Boolean,
    viewModel: AppUpdatesViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val prompt by viewModel.prompt.collectAsState()

    LaunchedEffect(enabled) {
        if (enabled) viewModel.onLaunch()
    }

    when (val current = prompt) {
        is AppUpdates.LaunchPrompt.Donate -> {
            AlertDialog(
                onDismissRequest = { viewModel.onDonateFinished() },
                title = { Text(stringResource(R.string.about_donate_nudge_title)) },
                text = { Text(stringResource(R.string.about_donate_nudge_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.onDonateFinished()
                            IntentUtils.viewDonateUri(context, ProductUpdate.DONATION_URL)
                        },
                    ) {
                        Text(stringResource(R.string.about_donate))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onDonateFinished() }) {
                        Text(stringResource(R.string.about_not_now))
                    }
                },
            )
        }
        is AppUpdates.LaunchPrompt.Update -> {
            AlertDialog(
                onDismissRequest = { viewModel.onUpdateFinished(current.version) },
                title = { Text(stringResource(R.string.about_update_title)) },
                text = { Text(stringResource(R.string.about_update_message, current.version)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.onUpdateFinished(current.version)
                            IntentUtils.viewUri(context, current.url)
                        },
                    ) {
                        Text(stringResource(R.string.about_install))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onUpdateFinished(current.version) }) {
                        Text(stringResource(R.string.about_later))
                    }
                },
            )
        }
        null -> Unit
    }
}
