package com.streamvault.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoudnessMathTest {

    @Test fun theGainIsLimitedByTheTruePeak() {
        // A song at -20 LUFS wants +6 dB, but a peak of -3 dBTP only leaves 2 dB before the ceiling.
        val gain = LoudnessMath.gainDb(-14f, -20f, -3f)
        assertEquals(2f, gain, 0.01f)
        // With headroom, the full correction is applied.
        assertEquals(6f, LoudnessMath.gainDb(-14f, -20f, -10f), 0.01f)
        assertEquals(6f, LoudnessMath.gainDb(-14f, -20f, null), 0.01f)
    }

    @Test fun loudSongsAreAttenuatedEvenWithHeadroom() {
        assertEquals(-6f, LoudnessMath.gainDb(-14f, -8f, -10f), 0.01f)
    }

    @Test fun labelsDescribeLuAndTruePeak() {
        assertEquals("-20.0 LUFS", LoudnessMath.lufsLabel(-20f))
        assertEquals("sin medir", LoudnessMath.lufsLabel(null))
        assertEquals("-0.5 dBTP", LoudnessMath.peakLabel(-0.5f))
    }

    @Test fun boostsQuietSongsAndAttenuatesLoudOnes() {
        // A quiet track at -30 dBFS played against a -16 dBFS target needs +14 dB.
        assertEquals(14f, LoudnessMath.gainDb(-16f, -30f), 0.01f)
        assertEquals(1400, LoudnessMath.boostMillibels(14f))
        assertEquals(1f, LoudnessMath.attenuation(14f), 0.001f)

        // A loud track at -8 dBFS must come down 8 dB.
        assertEquals(-8f, LoudnessMath.gainDb(-16f, -8f), 0.01f)
        assertEquals(0, LoudnessMath.boostMillibels(-8f))
        assertEquals(0.398f, LoudnessMath.attenuation(-8f), 0.01f)
    }

    @Test fun doesNotTouchUnmeasuredTracks() {
        assertEquals(0f, LoudnessMath.gainDb(-16f, null), 0f)
        assertEquals(0, LoudnessMath.boostMillibels(0f))
        assertEquals(1f, LoudnessMath.attenuation(0f), 0f)
        assertEquals(0f, LoudnessMath.gainDb(-16f, LoudnessMath.SILENCE_DB), 0f)
    }

    @Test fun keepsTheResultInsideSafeBounds() {
        assertEquals(LoudnessMath.MAX_BOOST_MB, LoudnessMath.boostMillibels(500f))
        assertTrue(LoudnessMath.attenuation(-500f) >= LoudnessMath.MIN_VOLUME)
        assertTrue(LoudnessMath.attenuation(-1f) in 0.05f..1f)
    }

    @Test fun labelsWhatTheUserHears() {
        assertEquals("sin medir", LoudnessMath.label(0f, measured = false))
        assertEquals("+3.5 dB", LoudnessMath.label(3.5f, measured = true))
        assertEquals("−6.0 dB", LoudnessMath.label(-6f, measured = true))
        assertEquals("nivelado", LoudnessMath.label(0f, measured = true))
    }
}
