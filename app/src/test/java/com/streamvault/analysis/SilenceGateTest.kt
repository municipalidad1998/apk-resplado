package com.streamvault.analysis

import org.junit.Assert.*
import org.junit.Test

class SilenceGateTest {
    @Test fun eightSecondsOfSilence() {
        val gate = SilenceGate(-45, 1000)
        for (i in 0 until 400) gate.window(0.0, i * 20L)
        repeat(3) { gate.window(.2, 8000L + it * 20) }
        assertEquals(7920L, gate.startMs)
    }
    @Test fun immediateAudioKeepsOriginalStart() {
        val gate = SilenceGate(-45, 1000)
        repeat(3) { gate.window(.2, it * 20L) }
        assertEquals(0L, gate.startMs)
    }
    @Test fun aSingleClickIsNotMusic() {
        val gate = SilenceGate(-45, 1000)
        gate.window(.9, 2000); gate.window(0.0, 2020)
        assertNull(gate.startMs)
        repeat(3) { gate.window(.2, 5000L + it * 20) }
        assertEquals(4920L, gate.startMs)
    }
    @Test fun minimumSilenceIsRespected() {
        val gate = SilenceGate(-45, 3000)
        repeat(3) { gate.window(.2, 2000L + it * 20) }
        assertEquals(0L, gate.startMs)
    }
    @Test fun sensitivityChangesDetection() {
        val high = SilenceGate(-60, 1000); val low = SilenceGate(-30, 1000)
        repeat(3) { high.window(.005, 2000L + it * 20); low.window(.005, 2000L + it * 20) }
        assertEquals(1920L, high.startMs); assertNull(low.startMs)
    }
    @Test fun allSilentNeverSeeksToEnd() {
        val gate = SilenceGate(-45, 1000)
        repeat(6000) { gate.window(0.0, it * 20L) }; assertNull(gate.startMs)
    }
    @Test fun nanDoesNotTrigger() { val gate = SilenceGate(-45, 0); repeat(3) { gate.window(Double.NaN, it * 20L) }; assertNull(gate.startMs) }
}
