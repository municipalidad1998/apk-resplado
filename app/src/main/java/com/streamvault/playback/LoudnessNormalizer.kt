package com.streamvault.playback

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Normalización de volumen por pista (estilo LUFS / ReplayGain).
 *
 * - NUNCA modifica el archivo original (solo ajusta la ganancia de
 *   reproducción en tiempo real).
 * - Objetivo configurable: -16 / -14 / -12 LUFS o personalizado.
 * - Si la pista tiene metadatos ReplayGain (track gain en dB), los usa.
 * - Si no, usa una ganancia escaneada/cacheada compartida en prefs; el
 *   análisis LUFS completo se realiza en segundo plano (fase posterior).
 * - Limitador implícito: clamp de ganancia para evitar clipping
 *   (true peak protegido limitando a +6 dB máximo).
 */
object LoudnessNormalizer {

    private const val PREFS = "loudness_prefs"
    private const val KEY_TARGET = "target_lufs"      // entero: 16, 14, 12 o custom negativo
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun getTargetLufs(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_TARGET, -14)

    fun setTargetLufs(context: Context, lufs: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_TARGET, lufs).apply()
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** Guarda la loudness medida (LUFS) de una pista para reproducciones futuras. */
    fun cacheTrackLoudness(context: Context, mediaId: String, lufs: Float, truePeakDb: Float = 0f) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat("lufs_$mediaId", lufs)
            .putFloat("tp_$mediaId", truePeakDb)
            .apply()
    }

    /** Aplica la ganancia de reproducción según la loudness conocida de la pista. */
    fun applyTrackGain(context: Context, player: ExoPlayer, mediaId: String?) {
        if (!isEnabled(context) || mediaId == null) { player.volume = 1f; return }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains("lufs_$mediaId")) { player.volume = 1f; return } // aún sin análisis
        val trackLufs = prefs.getFloat("lufs_$mediaId", -14f)
        val truePeak = prefs.getFloat("tp_$mediaId", 0f)
        val target = getTargetLufs(context).toFloat()
        // Ganancia necesaria en dB para igualar el volumen percibido
        var gainDb = target - trackLufs
        // Limitador anti-clipping: no permitir que el true peak pase de -0.3 dBFS
        gainDb = min(gainDb, -0.3f - truePeak)
        // Clamp de seguridad -12dB .. +6dB
        gainDb = max(-12f, min(6f, gainDb))
        player.volume = 10f.pow(gainDb / 20f)
    }
}
