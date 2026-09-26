package com.streamvault.playback

import kotlin.math.pow

/**
 * Turns a measured loudness (BS.1770 LUFS) into the two things Android lets us change:
 * a linear volume (attenuation only, 0..1) and a boost in millibels (system LoudnessEnhancer).
 *
 * Both are derived from the same gain so the result is one single perceived level:
 *   gainDb = targetDb - measuredDb
 *   gainDb > 0  -> boost with the audio effect (quiet songs)
 *   gainDb < 0  -> lower the player volume (loud songs)
 */
object LoudnessMath {

    const val DEFAULT_TARGET_DB = -14f
    const val MAX_BOOST_MB = 1500
    const val MIN_VOLUME = 0.05f
    const val SILENCE_DB = -70f
    /** No sample may go above this level after the gain, so boosting never clips. */
    const val TRUE_PEAK_CEILING_DBTP = -1f
    val TARGET_OPTIONS = listOf(-16f, -14f, -12f)

    /** Gain needed to bring [measuredDb] to [targetDb]. Unknown measurements get no gain at all. */
    fun gainDb(targetDb: Float, measuredDb: Float?): Float = gainDb(targetDb, measuredDb, null)

    /**
     * Same gain, capped by the true peak: if the song already peaks at −0.5 dBTP, it can only take
     * +0.5 dB before clipping, however far it is from the loudness target.
     */
    fun gainDb(targetDb: Float, measuredDb: Float?, peakDb: Float?): Float {
        if (measuredDb == null || measuredDb <= SILENCE_DB) return 0f
        val wanted = (targetDb - measuredDb).coerceIn(-MAX_ATTENUATION_DB, MAX_BOOST_MB / 100f)
        if (peakDb == null || peakDb <= SILENCE_DB) return wanted
        val headroom = TRUE_PEAK_CEILING_DBTP - peakDb
        return wanted.coerceAtMost(headroom)
    }

    /** Boost to request from android.media.audiofx.LoudnessEnhancer, in millibels. */
    fun boostMillibels(gainDb: Float): Int = if (gainDb <= 0f) 0 else (gainDb * 100).toInt().coerceIn(0, MAX_BOOST_MB)

    /** Linear player volume for negative gains, or 1 when the track needs no attenuation. */
    fun attenuation(gainDb: Float): Float =
        if (gainDb >= 0f) 1f else 10f.pow(gainDb / 20f).coerceIn(MIN_VOLUME, 1f)

    /** "−20.1 LUFS", for the original measurement. */
    fun lufsLabel(value: Float?): String = if (value == null || value <= SILENCE_DB) "sin medir" else "%.1f LUFS".format(value)

    /** "−0.6 dBTP", for the true peak. */
    fun peakLabel(value: Float?): String = if (value == null || value <= SILENCE_DB) "—" else "%.1f dBTP".format(value)

    /** What to show the user: "+3.5 dB", "−6.0 dB" or "sin medir". */
    fun label(gainDb: Float, measured: Boolean): String = when {
        !measured -> "sin medir"
        gainDb >= 0.05f -> "+%.1f dB".format(gainDb)
        gainDb <= -0.05f -> "−%.1f dB".format(-gainDb)
        else -> "nivelado"
    }

    private const val MAX_ATTENUATION_DB = 40f
}
