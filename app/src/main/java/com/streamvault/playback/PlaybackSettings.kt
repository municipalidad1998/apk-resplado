package com.streamvault.playback

import android.content.Context

/**
 * Preferencias centrales de reproducción de "Reproductor de Música Denilson".
 */
object PlaybackSettings {

    private const val PREFS = "app_settings"

    enum class NormMode { LUFS, REPLAYGAIN }

    fun isGaplessEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("gapless", true)

    fun setGapless(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("gapless", on).apply()
    }

    fun getNormMode(context: Context): NormMode =
        if (context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("norm_mode", "LUFS") == "REPLAYGAIN")
            NormMode.REPLAYGAIN else NormMode.LUFS

    fun setNormMode(context: Context, mode: NormMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("norm_mode", mode.name).apply()
    }

    fun getCrossfadeSec(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("crossfade_sec", 0)

    fun setCrossfadeSec(context: Context, sec: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("crossfade_sec", sec).apply()
    }
}
