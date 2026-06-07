package com.screenwakelock.detector

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.screenwakelock.detector.service.WakeMonitorService
import com.screenwakelock.detector.ui.navigation.AppNavigation
import com.screenwakelock.detector.ui.theme.ScreenWakelockTheme
import com.screenwakelock.detector.util.DeepLinkParams
import com.screenwakelock.detector.util.parseDeepLinkString
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var deepLinkParams by mutableStateOf(DeepLinkParams())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WakeMonitorService.start(this)
        deepLinkParams = parseDeepLinkString(intent?.data?.toString())

        setContent {
            ScreenWakelockTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        deepLinkWakeId = deepLinkParams.wakeId,
                        deepLinkHighlight = deepLinkParams.highlight,
                        deepLinkRoute = deepLinkParams.route,
                        deepLinkQuickFixWakeId = deepLinkParams.quickFixWakeId,
                        onDeepLinkConsumed = { deepLinkParams = DeepLinkParams() },
                    )
                }
            }
        }
    }
}
