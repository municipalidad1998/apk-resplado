package com.streamvault.settings

import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Backup file for the configuration. Pure JSON logic lives here so it can be tested, and so the
 * names used in the file are written down in exactly one place: adding a setting to
 * [PlayerSettings] means adding one line to [FIELDS].
 */
object SettingsBackup {

    const val FORMAT = "lumina-settings"
    const val VERSION = 1

    enum class Kind { STRING, INT, BOOL }

    /** json name, SharedPreferences key, type. The two names differ for historical reasons. */
    private val FIELDS = listOf(
        Triple("theme", "theme", Kind.STRING),
        Triple("crossfade", "crossfade", Kind.INT),
        Triple("skipSeconds", "skip", Kind.INT),
        Triple("detectSilence", "silence", Kind.BOOL),
        Triple("thresholdDb", "threshold", Kind.INT),
        Triple("minimumSilence", "minimum", Kind.INT),
        Triple("autoScan", "scan", Kind.BOOL),
        Triple("autoPlay", "autoplay", Kind.BOOL),
        Triple("shuffle", "shuffle", Kind.BOOL),
        Triple("repeat", "repeat", Kind.INT),
        Triple("fades", "fades", Kind.BOOL),
        Triple("animations", "animations", Kind.BOOL),
        Triple("largeCovers", "covers", Kind.BOOL),
        Triple("excludedFolders", "excluded", Kind.STRING),
        Triple("autoUpdate", "autoUpdate", Kind.BOOL),
        Triple("dynamicColor", "dynamicColor", Kind.BOOL),
        Triple("normalize", "normalize", Kind.BOOL),
        Triple("targetLoudnessDb", "targetLoudness", Kind.INT),
        Triple("compressor", "compressor", Kind.STRING),
        Triple("onlineQuality", "onlineQuality", Kind.STRING),
        Triple("mobileData", "mobileData", Kind.BOOL),
        Triple("wifiOnly", "wifiOnly", Kind.BOOL)
    )

    fun fieldNames(): List<String> = FIELDS.map { it.first }

    fun toJson(settings: PlayerSettings): String = JSONObject().apply {
        put("format", FORMAT)
        put("version", VERSION)
        put("theme", settings.theme)
        put("crossfade", settings.crossfade)
        put("skipSeconds", settings.skipSeconds)
        put("detectSilence", settings.detectSilence)
        put("thresholdDb", settings.thresholdDb)
        put("minimumSilence", settings.minimumSilence)
        put("autoScan", settings.autoScan)
        put("autoPlay", settings.autoPlay)
        put("shuffle", settings.shuffle)
        put("repeat", settings.repeat)
        put("fades", settings.fades)
        put("animations", settings.animations)
        put("largeCovers", settings.largeCovers)
        put("excludedFolders", settings.excludedFolders)
        put("autoUpdate", settings.autoUpdate)
        put("dynamicColor", settings.dynamicColor)
        put("normalize", settings.normalize)
        put("targetLoudnessDb", settings.targetLoudnessDb)
        put("compressor", settings.compressor)
        put("onlineQuality", settings.onlineQuality)
        put("mobileData", settings.mobileData)
        put("wifiOnly", settings.wifiOnly)
    }.toString()

    fun isBackup(text: String): Boolean = runCatching {
        JSONObject(text).optString("format") == FORMAT
    }.getOrDefault(false)

    /** Writes every recognised value into the preferences. Returns how many were applied. */
    fun applyTo(editor: SharedPreferences.Editor, root: JSONObject): Int {
        var applied = 0
        FIELDS.forEach { (json, key, kind) ->
            if (!root.has(json)) return@forEach
            when (kind) {
                Kind.STRING -> { val value = root.optString(json); if (value.isNotEmpty()) { editor.putString(key, value); applied++ } }
                Kind.INT -> { editor.putInt(key, root.getInt(json)); applied++ }
                Kind.BOOL -> { editor.putBoolean(key, root.getBoolean(json)); applied++ }
            }
        }
        return applied
    }
}
