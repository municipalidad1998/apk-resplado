package com.streamvault.data

import org.junit.Assert.*
import org.junit.Test

class TrackTest {
    private val track = Track("hash", "content://audio/1", "audio.mp3", "Audio", durationMs = 10000, detectedOffsetMs = 3000)
    @Test fun manualOverridesAutomaticEvenWhenDisabled() { assertEquals(5000L, track.copy(manualOffsetMs = 5000).offset(false)) }
    @Test fun manualZeroMeansOriginal() { assertEquals(0L, track.copy(manualOffsetMs = 0).offset(true)) }
    @Test fun automaticCanBeDisabled() { assertEquals(3000L, track.offset(true)); assertEquals(0L, track.offset(false)) }
    @Test fun invalidOffsetsAreClamped() { assertEquals(9900L, track.copy(manualOffsetMs = 50000).offset(true)); assertEquals(3000L, track.copy(durationMs = 0).offset(true)) }
    @Test fun searchEscapesUserWildcards() { assertEquals("%a\\%b\\_c\\\\d%", searchPattern("a%b_c\\d")) }
}
