package com.streamvault.playback

import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.util.Log

/**
 * Wraps the system LoudnessEnhancer effect, which is the only supported way to make a quiet file
 * louder without touching the file itself. One instance per audio session, because during a
 * crossfade two players sound at the same time and each one has its own session.
 */
class LoudnessNormalizer {

    private val enhancers = mutableMapOf<Int, LoudnessEnhancer>()

    fun setBoost(sessionId: Int, millibels: Int) {
        if (sessionId <= 0 || millibels <= 0) {
            clear(sessionId)
            return
        }
        if (!supported) return
        val existing = enhancers[sessionId]
        if (existing != null) {
            runCatching { existing.setTargetGain(millibels); existing.enabled = true }.getOrElse { recreate(sessionId, millibels) }
            return
        }
        recreate(sessionId, millibels)
    }

    private fun recreate(sessionId: Int, millibels: Int) {
        runCatching {
            val enhancer = LoudnessEnhancer(sessionId)
            enhancer.setTargetGain(millibels)
            enhancer.enabled = true
            enhancers[sessionId] = enhancer
        }.onFailure { Log.w("Loudness", "No se pudo aplicar la amplificación: ${it.message}") }
    }

    fun clear(sessionId: Int) {
        enhancers.remove(sessionId)?.let { enhancer ->
            runCatching { enhancer.enabled = false; enhancer.release() }
        }
    }

    fun release() {
        enhancers.values.forEach { runCatching { it.release() } }
        enhancers.clear()
    }

    companion object {
        /** Null until queried: asking requires no permission but it is not free. */
        private var cached: Boolean? = null
        val supported: Boolean
            get() {
                cached?.let { return it }
                val value = runCatching {
                    val effects = AudioEffect.queryEffects() ?: return@runCatching true
                    // Some devices report no effects at all; assume support and let the constructor fail.
                    effects.isEmpty() || effects.any { it.type == AudioEffect.EFFECT_TYPE_LOUDNESS_ENHANCER }
                }.getOrDefault(true)
                cached = value
                return value
            }
    }
}
