package com.screenwakelock.detector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.screenwakelock.detector.service.WakeMonitorService
import com.screenwakelock.detector.ui.navigation.AppNavigation
import com.screenwakelock.detector.ui.theme.ScreenWakelockTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WakeMonitorService.start(this)

        val data = intent?.data
        val wakeId = data?.lastPathSegment?.toLongOrNull()
        val highlight = data?.getQueryParameter("highlight")
        val deepLinkRoute = when (data?.host) {
            "settings" -> when (data.pathSegments.firstOrNull()) {
                "root" -> "root"
                "permissions" -> "permissions"
                else -> null
            }
            "insights" -> "insights"
            else -> null
        }

        setContent {
            ScreenWakelockTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        deepLinkWakeId = wakeId,
                        deepLinkHighlight = highlight,
                        deepLinkRoute = deepLinkRoute,
                    )
                }
            }
        }
    }
}
