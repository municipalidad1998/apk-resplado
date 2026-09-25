package com.streamvault.playback

import kotlin.math.pow

/**
 * Turns a measured loudness into the two things Android lets us change:
 * a linear volume (attenuation only, 0..1) and a boost in millibels (system LoudnessEnhancer).
 *
 * Both are derived from the same gain so the result is one single perceived level:
 *   gainDb = targetDb - measuredDb
 *   gainDb > 0  -> boost with the audio effect (quiet songs)
 *   gainDb < 0  -> lower the player volume (loud songs)
 */
object LoudnessMath {

    const val DEFAULT_TARGET_DB = -16f
    const val MAX_BOOST_MB = 1500
    const val MIN_VOLUME = 0.05f
    const val SILENCE_DB = -70f

    /** Gain needed to bring [measuredDb] to [targetDb]. Unknown measurements get no gain at all. */
    fun gainDb(targetDb: Float, measuredDb: Float?): Float {
        if (measuredDb == null || measuredDb <= SILENCE_DB) return 0f
        return (targetDb - measuredDb).coerceIn(-MAX_ATTENUATION_DB, MAX_BOOST_MB / 100f)
    }

    /** Boost to request from android.media.audiofx.LoudnessEnhancer, in millibels. */
    fun boostMillibels(gainDb: Float): Int = if (gainDb <= 0f) 0 else (gainDb * 100).toInt().coerceIn(0, MAX_BOOST_MB)

    /** Linear player volume for negative gains, or 1 when the track needs no attenuation. */
    fun attenuation(gainDb: Float): Float =
        if (gainDb >= 0f) 1f else 10f.pow(gainDb / 20f).coerceIn(MIN_VOLUME, 1f)

    /** What to show the user: "+3.5 dB", "−6.0 dB" or "sin medir". */
    fun label(gainDb: Float, measured: Boolean): String = when {
        !measured -> "sin medir"
        gainDb >= 0.05f -> "+%.1f dB".format(gainDb)
        gainDb <= -0.05f -> "−%.1f dB".format(-gainDb)
        else -> "nivelado"
    }

    private const val MAX_ATTENUATION_DB = 40f
}
