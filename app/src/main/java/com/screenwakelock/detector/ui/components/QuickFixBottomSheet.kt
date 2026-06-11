package com.screenwakelock.detector.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.screenwakelock.detector.domain.attributor.AppDisplayResolver
import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.util.ChannelMuter
import com.screenwakelock.detector.util.IntentUtils
import com.screenwakelock.detector.util.SilenceWake

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickFixBottomSheet(
    event: WakeEvent?,
    visible: Boolean,
    ignoredPackages: Set<String>,
    appDisplayResolver: AppDisplayResolver,
    onDismiss: () -> Unit,
    onWhyThisApp: (Long) -> Unit,
    onMuteChannel: (WakeEvent, ChannelMuter.MuteResult) -> Unit,
    onIgnoreApp: (WakeEvent, String) -> Unit,
) {
    if (!visible || event == null) return
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pkg = event.attributedPackage
    val appName = appDisplayResolver.resolveAppName(event)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = appName,
                style = MaterialTheme.typography.headlineSmall,
            )
            event.displayChannel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            appDisplayResolver.resolveSubtitle(event)?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ConfidenceIndicator(confidence = event.confidence)

            if (pkg != null) {
                Button(
                    onClick = {
                        val result = SilenceWake.silence(event)
                        SilenceWake.openSettings(context, event)
                        onMuteChannel(event, result)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Silence channel")
                }

                OutlinedButton(
                    onClick = {
                        if (event.channelId != null && IntentUtils.canOpenChannelSettings()) {
                            context.startActivity(
                                IntentUtils.channelNotificationSettings(pkg, event.channelId),
                            )
                        } else {
                            context.startActivity(IntentUtils.appNotificationSettings(pkg))
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open notification settings")
                }

                if (pkg in ignoredPackages) {
                    Text(
                        text = "This app is ignored — alerts and insights skip it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    OutlinedButton(
                        onClick = {
                            onIgnoreApp(event, pkg)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Ignore this app")
                    }
                }
            } else {
                Text(
                    text = "No app attributed — grant permissions to identify wake sources.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TextButton(
                onClick = {
                    onWhyThisApp(event.id)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Why this app?")
            }
        }
    }
}
