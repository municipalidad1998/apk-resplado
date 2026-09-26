package com.streamvault.playback

/**
 * Compression presets, the Audacity "Compressor" step: bring the loud parts down and the quiet
 * parts up so a live recording or a religious song stops jumping between whisper and shout.
 *
 * Pure data and pure math so it can be unit tested without an Android device.
 */
data class CompressorPreset(
    val key: String,
    val label: String,
    val description: String,
    val thresholdDb: Float,
    val ratio: Float,
    val attackMs: Float,
    val releaseMs: Float,
    val kneeDb: Float,
    val makeupDb: Float
) {
    companion object {
        val OFF = CompressorPreset("off", "Desactivado", "Solo nivelación de volumen", 0f, 1f, 5f, 60f, 0f, 0f)
        val SOFT = CompressorPreset("soft", "Suave", "Controla los picos sin cambiar el carácter", -24f, 2.5f, 20f, 300f, 6f, 2f)
        val BALANCED = CompressorPreset("balanced", "Equilibrado", "Buen punto de partida para casi toda la música", -28f, 4f, 12f, 220f, 6f, 4f)
        val STRONG = CompressorPreset("strong", "Fuerte", "Cantos religiosos y grabaciones en vivo", -32f, 8f, 6f, 160f, 8f, 7f)
        val VOICE = CompressorPreset("voice", "Voz y predicación", "Máximo control para voces con eco o ruido de sala", -34f, 12f, 4f, 140f, 10f, 9f)
        val ALL = listOf(OFF, SOFT, BALANCED, STRONG, VOICE)
        fun from(key: String?): CompressorPreset = ALL.firstOrNull { it.key == key } ?: BALANCED
    }
}

object CompressorMath {

    /** The "Normalizar a −1 dB" step of the recipe: a brick wall so the boost cannot clip. */
    const val LIMITER_THRESHOLD_DB = -1f
    const val LIMITER_RATIO = 20f
    const val LIMITER_ATTACK_MS = 1f
    const val LIMITER_RELEASE_MS = 60f
    const val NOISE_GATE_DB = -80f
    const val MAX_POST_GAIN_DB = 24f
    const val BAND_CUTOFF_HZ = 20_000f

    fun isActive(preset: CompressorPreset): Boolean = preset.key != CompressorPreset.OFF.key

    /**
     * Gain applied after the compressor. It is the preset's own make-up plus whatever the track
     * still needs to reach the target level; only the positive part, because lowering is done
     * with the player volume and the limiter protects the top.
     */
    fun postGainDb(preset: CompressorPreset, gainDb: Float): Float {
        if (!isActive(preset)) return 0f
        return (preset.makeupDb + gainDb.coerceAtLeast(0f)).coerceIn(0f, MAX_POST_GAIN_DB)
    }

    /** When the compressor can already lift the track, the loudness enhancer must stay off. */
    fun enhancerMillibels(preset: CompressorPreset, gainDb: Float): Int =
        if (isActive(preset)) 0 else LoudnessMath.boostMillibels(gainDb)
}
