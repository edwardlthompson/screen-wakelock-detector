package com.screenwakelock.detector.updates

import java.net.HttpURLConnection
import java.net.URL

object GithubRelease {
    fun fetchLatest(userAgentVersion: String): Parsed? {
        val conn = URL(ProductUpdate.RELEASES_API).openConnection() as HttpURLConnection
        return try {
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "Screen-Wakelock-Detector/$userAgentVersion")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            if (conn.responseCode != 200) return null
            parse(conn.inputStream.bufferedReader().use { it.readText() })
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    fun parse(json: String): Parsed? {
        val trimmed = json.trim()
        if (trimmed.isEmpty() || !trimmed.startsWith("{")) return null
        return try {
            val htmlUrl = readStringField(trimmed, "html_url") ?: ProductUpdate.RELEASES_PAGE
            val assetsJson = readArray(trimmed, "assets") ?: return Parsed(htmlUrl, emptyList())
            val assets = mutableListOf<ProductUpdate.NamedAsset>()
            for (obj in readObjects(assetsJson)) {
                val name = readStringField(obj, "name") ?: continue
                val url = readStringField(obj, "browser_download_url") ?: continue
                if (name.isNotBlank() && url.isNotBlank()) {
                    assets.add(ProductUpdate.NamedAsset(name, url))
                }
            }
            Parsed(htmlUrl, assets)
        } catch (_: Exception) {
            null
        }
    }

    private fun readStringField(json: String, key: String): String? {
        val match = Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").find(json)
            ?: return null
        return match.groupValues[1]
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    private fun readArray(json: String, key: String): String? {
        val startMatch = Regex("\"${Regex.escape(key)}\"\\s*:\\s*\\[").find(json) ?: return null
        val start = startMatch.range.last
        var depth = 1
        var i = start + 1
        while (i < json.length && depth > 0) {
            when (json[i]) {
                '[' -> depth++
                ']' -> depth--
                '"' -> {
                    i++
                    while (i < json.length && json[i] != '"') {
                        if (json[i] == '\\') i++
                        i++
                    }
                }
            }
            i++
        }
        if (depth != 0) return null
        return json.substring(start + 1, i - 1)
    }

    private fun readObjects(arrayBody: String): List<String> {
        val objects = mutableListOf<String>()
        var depth = 0
        var start = -1
        var i = 0
        while (i < arrayBody.length) {
            when (arrayBody[i]) {
                '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        objects.add(arrayBody.substring(start, i + 1))
                        start = -1
                    }
                }
                '"' -> {
                    i++
                    while (i < arrayBody.length && arrayBody[i] != '"') {
                        if (arrayBody[i] == '\\') i++
                        i++
                    }
                }
            }
            i++
        }
        return objects
    }

    data class Parsed(val htmlUrl: String, val assets: List<ProductUpdate.NamedAsset>)
}
