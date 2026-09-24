package com.streamvault.playback

import org.junit.Assert.*
import org.junit.Test

class PlaybackCueTest {
    @Test fun elevenSecondsAndFractionalSecondsAreUnambiguous() {
        assertEquals(11_000L, parseAudioTime("00:11")); assertEquals(11_000L, parseAudioTime("11"))
        assertEquals(110L, parseAudioTime("0.11")); assertEquals(110L, parseAudioTime("0,11"))
        assertEquals(3_723_500L, parseAudioTime("1:02:03.5"))
    }
    @Test fun invalidTimesAreRejected() {
        listOf("", "00:61", "-1", "NaN", "Infinity", "1::2", "1.5:20", "abc").forEach { assertNull(it, parseAudioTime(it)) }
    }
    @Test fun twoMinuteOutroCanBeSkippedWithoutTwoMinuteMix() {
        assertEquals(200_000L, PlaybackCue.end(320_000, 11_000, 200_000))
        assertEquals(5_000L, PlaybackCue.overlap(5_000, 189_000, 180_000))
    }
    @Test fun longCrossfadeIsNotSilentlyCutInHalf() {
        assertEquals(120_000L, PlaybackCue.overlap(120_000, 180_000, 180_000))
        assertEquals(2900L, PlaybackCue.overlap(120_000, 3000, 8000))
        assertEquals(0L, PlaybackCue.overlap(0, 3000, 3000))
    }
    @Test fun endMustBeInsidePlayableAudio() {
        assertEquals(9000L, PlaybackCue.end(9000, 1000, 10_000))
        assertEquals(9000L, PlaybackCue.end(9000, 1000, 500))
    }
}
