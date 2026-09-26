package com.streamvault.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompressorPresetTest {

    @Test fun unknownKeysFallBackToTheBalancedPreset() {
        assertEquals(CompressorPreset.BALANCED, CompressorPreset.from(null))
        assertEquals(CompressorPreset.BALANCED, CompressorPreset.from("inventado"))
        assertEquals(CompressorPreset.VOICE, CompressorPreset.from("voice"))
        assertEquals(CompressorPreset.OFF, CompressorPreset.from("off"))
    }

    @Test fun presetsGetStrongerInOrder() {
        val order = listOf(CompressorPreset.SOFT, CompressorPreset.BALANCED, CompressorPreset.STRONG, CompressorPreset.VOICE)
        assertTrue(order.zipWithNext().all { (a, b) -> b.ratio > a.ratio && b.thresholdDb < a.thresholdDb && b.makeupDb > a.makeupDb })
        // Every preset stays inside the ranges the effect accepts.
        CompressorPreset.ALL.forEach { preset ->
            if (preset.key == "off") return@forEach
            assertTrue(preset.thresholdDb in -60f..0f)
            assertTrue(preset.ratio >= 1f && preset.ratio <= 20f)
            assertTrue(preset.attackMs > 0f && preset.releaseMs > preset.attackMs)
            assertTrue(preset.kneeDb >= 0f)
        }
    }

    @Test fun postGainCombinesMakeUpAndLevelCorrection() {
        // Quiet track: +14 dB of level correction on top of the balanced make-up of 4 dB.
        assertEquals(18f, CompressorMath.postGainDb(CompressorPreset.BALANCED, 14f), 0.01f)
        // Loud track: no positive gain is added, only the preset make-up.
        assertEquals(4f, CompressorMath.postGainDb(CompressorPreset.BALANCED, -8f), 0.01f)
        // Switched off: no post gain at all.
        assertEquals(0f, CompressorMath.postGainDb(CompressorPreset.OFF, 14f), 0f)
        // Capped so a very quiet recording cannot be pushed into distortion territory.
        assertEquals(CompressorMath.MAX_POST_GAIN_DB, CompressorMath.postGainDb(CompressorPreset.VOICE, 60f), 0.01f)
    }

    @Test fun theCompressorReplacesTheLoudnessEnhancer() {
        assertEquals(0, CompressorMath.enhancerMillibels(CompressorPreset.BALANCED, 14f))
        assertEquals(1400, CompressorMath.enhancerMillibels(CompressorPreset.OFF, 14f))
        assertEquals(0, CompressorMath.enhancerMillibels(CompressorPreset.OFF, -6f))
        assertTrue(CompressorMath.isActive(CompressorPreset.STRONG))
        assertFalse(CompressorMath.isActive(CompressorPreset.OFF))
    }
}
