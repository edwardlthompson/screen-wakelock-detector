package com.screenwakelock.detector.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.screenwakelock.detector.domain.model.WakeEvent
import com.screenwakelock.detector.util.IntentUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickFixBottomSheet(
    event: WakeEvent?,
    visible: Boolean,
    onDismiss: () -> Unit,
    onWhyThisApp: (Long) -> Unit,
    onMuteChannel: (WakeEvent) -> Unit,
) {
    if (!visible || event == null) return
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                text = event.displayAppName,
                style = MaterialTheme.typography.headlineSmall,
            )
            event.displayChannel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ConfidenceIndicator(confidence = event.confidence)

            Button(
                onClick = {
                    val pkg = event.attributedPackage
                    val channel = event.channelId
                    if (pkg != null && channel != null && IntentUtils.canOpenChannelSettings()) {
                        context.startActivity(IntentUtils.channelNotificationSettings(pkg, channel))
                    } else if (pkg != null) {
                        context.startActivity(IntentUtils.appNotificationSettings(pkg))
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Silence channel")
            }

            OutlinedButton(
                onClick = {
                    event.attributedPackage?.let {
                        context.startActivity(IntentUtils.appNotificationSettings(it))
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open notification settings")
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
