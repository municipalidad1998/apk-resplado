package com.streamvault.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small settings only. Library, metadata and queues live in Room. */
data class PlayerSettings(
    val theme: String = "system",
    val crossfade: Int = 5,
    val skipSeconds: Int = 10,
    val detectSilence: Boolean = true,
    val thresholdDb: Int = -45,
    val minimumSilence: Int = 1,
    val autoScan: Boolean = true,
    val autoPlay: Boolean = true,
    val shuffle: Boolean = false,
    val repeat: Int = 0,
    val fades: Boolean = true,
    val animations: Boolean = true,
    val largeCovers: Boolean = true,
    val excludedFolders: String = ""
) { val analysisKey get() = "rms-v2:$thresholdDb:$minimumSilence" }

class Preferences(context: Context) {
    private val prefs = context.getSharedPreferences("lumina", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(read())
    val state = mutable.asStateFlow()
    private fun read() = PlayerSettings(
        theme = prefs.getString("theme", "system")!!,
        crossfade = prefs.getInt("crossfade", 5), skipSeconds = prefs.getInt("skip", 10),
        detectSilence = prefs.getBoolean("silence", true), thresholdDb = prefs.getInt("threshold", -45),
        minimumSilence = prefs.getInt("minimum", 1), autoScan = prefs.getBoolean("scan", true),
        autoPlay = prefs.getBoolean("autoplay", true), shuffle = prefs.getBoolean("shuffle", false),
        repeat = prefs.getInt("repeat", 0), fades = prefs.getBoolean("fades", true),
        animations = prefs.getBoolean("animations", true), largeCovers = prefs.getBoolean("covers", true),
        excludedFolders = prefs.getString("excluded", "")!!
    )
    fun update(change: (PlayerSettings) -> PlayerSettings) {
        val s = change(mutable.value)
        prefs.edit().putString("theme", s.theme).putInt("crossfade", s.crossfade).putInt("skip", s.skipSeconds)
            .putBoolean("silence", s.detectSilence).putInt("threshold", s.thresholdDb).putInt("minimum", s.minimumSilence)
            .putBoolean("scan", s.autoScan).putBoolean("autoplay", s.autoPlay).putBoolean("shuffle", s.shuffle)
            .putInt("repeat", s.repeat).putBoolean("fades", s.fades).putBoolean("animations", s.animations)
            .putBoolean("covers", s.largeCovers).putString("excluded", s.excludedFolders).apply()
        mutable.value = s
    }
    fun roots(): Set<String> = prefs.getStringSet("roots", emptySet())!!.toSet()
    fun addRoot(uri: String) { prefs.edit().putStringSet("roots", roots() + uri).apply() }
    fun removeRoot(uri: String) { prefs.edit().putStringSet("roots", roots() - uri).apply() }
    fun savePosition(index: Int, position: Long) { prefs.edit().putInt("index", index).putLong("position", position).apply() }
    fun position() = prefs.getInt("index", 0) to prefs.getLong("position", 0)
}
