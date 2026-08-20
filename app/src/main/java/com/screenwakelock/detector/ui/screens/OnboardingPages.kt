package com.screenwakelock.detector.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.screenwakelock.detector.ui.modifiers.highRefreshScroll

enum class OnboardingPage(val path: String) {
    Welcome("welcome"),
    HowItWorks("how"),
    Privacy("privacy"),
    Root("root"),
    Permissions("permissions"),
    ;

    companion object {
        fun fromPath(path: String?): OnboardingPage =
            entries.firstOrNull { it.path == path } ?: Welcome
    }
}

@Composable
fun OnboardingHowItWorks() {
    val steps = listOf(
        "Detect" to "We listen for screen-on events in the background.",
        "Identify" to "We match each wake to a notification, channel, or wakelock when possible.",
        "Show" to "You get a timestamped history with confidence and an explanation.",
        "Fix" to "Jump to that app’s notification settings or mute the channel.",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .highRefreshScroll(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("How it works", style = MaterialTheme.typography.displaySmall)
        steps.forEachIndexed { index, (title, body) ->
            Text("${index + 1}. $title", style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun OnboardingPrivacy() {
    val bullets = listOf(
        "Wake history stored locally in an app-private database",
        "No cloud upload, no account, no analytics telemetry",
        "You can clear history anytime in Settings",
        "Notification metadata only — not message contents",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .highRefreshScroll(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Your data stays on your phone", style = MaterialTheme.typography.displaySmall)
        bullets.forEach { bullet ->
            Text("• $bullet", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun OnboardingRoot() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .highRefreshScroll(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Optional root", style = MaterialTheme.typography.displaySmall)
        Text(
            "Read wakelock data via an in-app root shell for deeper accuracy when a wake isn’t tied to a notification.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Nothing outside fixed diagnostic commands. Everything is built into this app — no extra modules. Enable later in Settings → Root.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
