package com.screenwakelock.detector.util

import android.net.Uri

data class DeepLinkParams(
    val wakeId: Long? = null,
    val highlight: String? = null,
    val route: String? = null,
    /** null = none; 0 = latest wake quick-fix; >0 = specific wake event id */
    val quickFixWakeId: Long? = null,
    /** Debug smoke only: "enable" turns on root attribution when su is available */
    val rootAutomation: String? = null,
)

fun parseDeepLinkString(raw: String?): DeepLinkParams {
    if (raw.isNullOrBlank() || !raw.startsWith("${IntentUtils.DEEP_LINK_SCHEME}://")) {
        return DeepLinkParams()
    }
    return when {
        raw.startsWith("screenwakelock://app/quickfix/") -> {
            val id = raw
                .removePrefix("screenwakelock://app/quickfix/")
                .substringBefore('?')
                .toLongOrNull() ?: 0L
            DeepLinkParams(quickFixWakeId = id)
        }
        raw.startsWith("screenwakelock://wake/latest/actions") ->
            DeepLinkParams(quickFixWakeId = 0L)
        raw.startsWith("screenwakelock://wake/latest") ->
            DeepLinkParams(quickFixWakeId = 0L)
        raw.startsWith("screenwakelock://app/detail/") -> {
            val id = raw
                .removePrefix("screenwakelock://app/detail/")
                .substringBefore('?')
                .toLongOrNull()
            DeepLinkParams(wakeId = id)
        }
        raw.startsWith("screenwakelock://app/permissions") -> {
            val highlight = raw.substringAfter('?', missingDelimiterValue = "")
                .substringAfter("highlight=", missingDelimiterValue = "")
                .substringBefore('&')
                .takeIf { it.isNotEmpty() }
            DeepLinkParams(route = "permissions", highlight = highlight)
        }
        raw.startsWith("screenwakelock://settings/root") -> {
            val automation = raw.substringAfter('?', missingDelimiterValue = "")
                .substringAfter("automation=", missingDelimiterValue = "")
                .substringBefore('&')
                .takeIf { it.isNotEmpty() }
            DeepLinkParams(route = "root", rootAutomation = automation)
        }
        raw.startsWith("screenwakelock://settings/permissions") -> {
            val highlight = raw.substringAfter('?', missingDelimiterValue = "")
                .substringAfter("highlight=", missingDelimiterValue = "")
                .substringBefore('&')
                .takeIf { it.isNotEmpty() }
            DeepLinkParams(route = "permissions", highlight = highlight)
        }
        raw.startsWith("screenwakelock://insights") ->
            DeepLinkParams(route = "insights")
        else -> DeepLinkParams()
    }
}

fun parseDeepLink(data: Uri?): DeepLinkParams = parseDeepLinkString(data?.toString())
