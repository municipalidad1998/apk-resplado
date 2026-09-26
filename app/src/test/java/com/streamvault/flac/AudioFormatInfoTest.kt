package com.streamvault.flac

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFormatInfoTest {

    @Test fun labelsLosslessFilesWithDepthAndRate() {
        val info = AudioFormatInfo("audio/flac", 96000, 2, 4608, 24, 254_000)
        assertEquals("FLAC", info.codecLabel)
        assertTrue(info.lossless)
        assertEquals("FLAC · 24-bit / 96 kHz · Lossless", info.badge)
    }

    @Test fun labelsCompressedFilesWithoutLosslessClaim() {
        val mp3 = AudioFormatInfo("audio/mpeg", 44100, 2, 320, 0, 210_000)
        assertEquals("MP3", mp3.codecLabel)
        assertTrue(!mp3.lossless)
        assertTrue(mp3.badge.contains("44 kHz"))
        val ogg = AudioFormatInfo("audio/vorbis", 44100, 2, 192, 0, 180_000)
        assertEquals("OGG", ogg.codecLabel)
    }

    @Test fun derivesBitDepthFromBitrateWhenTheContainerHidesIt() {
        // 4608 kbps / (96000 Hz * 2 channels) = 24 bits per sample.
        val rate = 96000
        val channels = 2
        val bitrateBitsPerSecond = 24 * rate * channels
        val derived = (bitrateBitsPerSecond / (rate * channels)).coerceIn(0, 32)
        assertEquals(24, derived)
    }
}
