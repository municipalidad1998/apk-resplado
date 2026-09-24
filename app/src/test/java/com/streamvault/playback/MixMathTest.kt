package com.streamvault.playback

import org.junit.Assert.*
import org.junit.Test

class MixMathTest {
    @Test fun endpoints() {
        assertEquals(1f, MixMath.gains(0f).first, .0001f); assertEquals(0f, MixMath.gains(0f).second, .0001f)
        assertEquals(0f, MixMath.gains(1f).first, .0001f); assertEquals(1f, MixMath.gains(1f).second, .0001f)
    }
    @Test fun constantPower() {
        repeat(101) { val (a, b) = MixMath.gains(it / 100f); assertEquals(1f, a * a + b * b, .0001f) }
    }
    @Test fun monotonicAndBounded() {
        var previous = 1f
        repeat(101) { val (a, b) = MixMath.gains(it / 100f); assertTrue(a <= previous); assertTrue(a in 0f..1f && b in 0f..1f); previous = a }
        assertEquals(MixMath.gains(0f), MixMath.gains(-1f)); assertEquals(MixMath.gains(1f), MixMath.gains(2f))
    }
    @Test fun shortTracksClampOverlap() {
        assertEquals(2000L, MixMath.duration(30000, 4000, 10000)); assertEquals(0L, MixMath.duration(0, 10000, 10000))
        assertEquals(0L, MixMath.duration(5000, -1, 5000))
    }
    @Test fun skipsCannotLeaveFile() { assertEquals(0L, MixMath.seek(5000, -10000, 20000)); assertEquals(20000L, MixMath.seek(18000, 10000, 20000)) }
}
