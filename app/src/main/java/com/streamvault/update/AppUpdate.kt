package com.streamvault.update

import org.json.JSONObject

/**
 * What the app needs to know about a published release of this repository.
 * Pure data so it can be unit-tested without Android.
 */
data class ReleaseInfo(
    val tag: String,
    val version: String,
    val title: String,
    val notes: String,
    val apkUrl: String,
    val apkName: String,
    val sizeBytes: Long
)

object UpdateConfig {
    const val OWNER = "municipalidad1998"
    const val REPO = "apk-resplado"
    const val LATEST = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"
}

/**
 * Parsing and comparison rules for the GitHub Releases API.
 * Anything malformed is treated as "no update" instead of crashing the app.
 */
object UpdateParser {

    fun parse(json: String): ReleaseInfo? = runCatching {
        val root = JSONObject(json)
        val tag = root.optString("tag_name").orEmpty()
        if (tag.isBlank()) return null
        val assets = root.optJSONArray("assets") ?: return null
        var apk: JSONObject? = null
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val name = asset.optString("name")
            if (name.endsWith(".apk", ignoreCase = true) && asset.optString("browser_download_url").startsWith("https://")) {
                apk = asset
                break
            }
        }
        val found = apk ?: return null
        ReleaseInfo(
            tag = tag,
            version = tag.trimStart('v', 'V'),
            title = root.optString("name").ifBlank { tag },
            notes = root.optString("body").trim(),
            apkUrl = found.optString("browser_download_url"),
            apkName = found.optString("name"),
            sizeBytes = found.optLong("size", 0L)
        )
    }.getOrNull()

    /**
     * Numeric comparison of versions like `2.9.0` and `2.10.0`.
     * Non numeric pieces are ignored, so `2.2.0-rc1` compares as `2.2.0`.
     */
    fun isNewer(candidate: String, current: String): Boolean {
        val left = numbers(candidate)
        val right = numbers(current)
        if (left.isEmpty() || right.isEmpty()) return false
        val size = maxOf(left.size, right.size)
        for (index in 0 until size) {
            val a = left.getOrNull(index) ?: 0
            val b = right.getOrNull(index) ?: 0
            if (a != b) return a > b
        }
        return false
    }

    private fun numbers(version: String): List<Int> = version.trim().trimStart('v', 'V')
        .split('.', '-', '_', '+')
        .mapNotNull { part -> part.takeWhile { it.isDigit() }.toIntOrNull() }
}
