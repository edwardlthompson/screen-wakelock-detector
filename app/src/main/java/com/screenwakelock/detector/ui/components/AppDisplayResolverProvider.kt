package com.screenwakelock.detector.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.screenwakelock.detector.domain.attributor.AppDisplayResolver

@Composable
fun rememberAppDisplayResolver(): AppDisplayResolver {
    val context = LocalContext.current
    return remember(context) { AppDisplayResolver(context) }
}
