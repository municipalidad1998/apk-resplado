package com.streamvault.playback

import android.media.audiofx.DynamicsProcessing
import android.os.Build
import android.util.Log

/**
 * Applies the compressor with the system DynamicsProcessing effect (Android 9 / API 28+).
 * One instance per audio session, because a crossfade plays two players at the same time.
 *
 * On older Android versions the compression is simply not applied; the app keeps working with
 * the loudness enhancer and volume attenuation only.
 */
class DynamicsController {

    private val effects = mutableMapOf<Int, DynamicsProcessing>()

    fun apply(sessionId: Int, preset: CompressorPreset, postGainDb: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || sessionId <= 0) return
        if (!CompressorMath.isActive(preset)) {
            clear(sessionId)
            return
        }
        val existing = effects[sessionId]
        if (existing != null) {
            runCatching { configure(existing, preset, postGainDb) }
                .onFailure { runCatching { existing.release() }; effects.remove(sessionId) }
            return
        }
        runCatching {
            val effect = DynamicsProcessing(0, sessionId, buildConfig())
            configure(effect, preset, postGainDb)
            effect.enabled = true
            effects[sessionId] = effect
        }.onFailure { Log.w("Dynamics", "No se pudo aplicar el compresor: ${it.message}") }
    }

    fun clear(sessionId: Int) {
        effects.remove(sessionId)?.let { effect ->
            runCatching { effect.enabled = false; effect.release() }
        }
    }

    fun release() {
        effects.values.forEach { runCatching { it.release() } }
        effects.clear()
    }

    private fun configure(effect: DynamicsProcessing, preset: CompressorPreset, postGainDb: Float) {
        val band = band(preset, postGainDb)
        val limiter = limiter()
        for (channel in 0 until effect.channelCount) {
            effect.setMbcBandByChannelIndex(channel, 0, band)
            effect.setLimiterByChannelIndex(channel, limiter)
        }
    }

    /** Skeleton: two channels, one broadband compressor band each and the limiter switched on. */
    private fun buildConfig(): DynamicsProcessing.Config = DynamicsProcessing.Config.Builder(
        DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
        2, false, 0, true, 1, false, 0, true
    ).build()

    private fun band(preset: CompressorPreset, postGainDb: Float) = DynamicsProcessing.MbcBand(
        true,
        CompressorMath.BAND_CUTOFF_HZ,
        preset.attackMs,
        preset.releaseMs,
        preset.ratio,
        preset.thresholdDb,
        preset.kneeDb,
        CompressorMath.NOISE_GATE_DB,
        1f,
        0f,
        postGainDb
    )

    private fun limiter() = DynamicsProcessing.Limiter(
        true,
        true,
        0,
        CompressorMath.LIMITER_ATTACK_MS,
        CompressorMath.LIMITER_RELEASE_MS,
        CompressorMath.LIMITER_RATIO,
        CompressorMath.LIMITER_THRESHOLD_DB,
        0f
    )
}
