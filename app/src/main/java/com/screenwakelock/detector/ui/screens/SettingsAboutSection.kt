package com.screenwakelock.detector.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.screenwakelock.detector.BuildConfig
import com.screenwakelock.detector.R
import com.screenwakelock.detector.util.IntentUtils
import com.screenwakelock.detector.util.ReleaseNotesLoader

@Composable
fun SettingsAboutSection(
    onLinkOpenFailed: () -> Unit,
    donateAutomation: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val releaseNotes = remember {
        ReleaseNotesLoader.load(context, BuildConfig.VERSION_CODE)
    }
    val changelogUrl = stringResource(R.string.changelog_url)
    val donateUrl = stringResource(R.string.donate_venmo_url)

    LaunchedEffect(donateAutomation) {
        if (BuildConfig.DEBUG && donateAutomation) {
            delay(400)
            if (!IntentUtils.viewDonateUri(context, donateUrl)) {
                onLinkOpenFailed()
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        Text(
            text = stringResource(R.string.about_section_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        ListItem(
            headlineContent = {
                Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME))
            },
        )
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = releaseNotes ?: stringResource(R.string.about_release_notes_fallback),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        ListItem(
            modifier = Modifier.clickable {
                if (!IntentUtils.viewUri(context, changelogUrl)) {
                    onLinkOpenFailed()
                }
            },
            headlineContent = { Text(stringResource(R.string.about_view_changelog)) },
            supportingContent = { Text(stringResource(R.string.about_view_changelog_summary)) },
        )
        ListItem(
            modifier = Modifier.clickable {
                if (!IntentUtils.viewDonateUri(context, donateUrl)) {
                    onLinkOpenFailed()
                }
            },
            headlineContent = { Text(stringResource(R.string.about_support_development)) },
            supportingContent = { Text(stringResource(R.string.about_support_development_summary)) },
        )
    }
}
