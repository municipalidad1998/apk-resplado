package com.streamvault.analysis

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sin

/**
 * The meter is pure Kotlin, so it can be checked against signals with a known loudness.
 * A full scale sine has a mean square of 0.5 (-3.01 dBFS); adding the -0.691 offset of the
 * standard and the +0.70 dB of the K-weighting at 1 kHz gives -3.00 LUFS, in mono or stereo.
 */
class LoudnessMeterTest {

    private fun sine(amplitude: Float, seconds: Int, rate: Int = 48000, channels: Int = 2): FloatArray {
        val frames = rate * seconds
        val output = FloatArray(frames * channels)
        for (frame in 0 until frames) {
            val value = (amplitude * sin(2 * Math.PI * 1000.0 * frame / rate)).toFloat()
            for (channel in 0 until channels) output[frame * channels + channel] = value
        }
        return output
    }

    private fun measure(samples: FloatArray, rate: Int = 48000, channels: Int = 2, chunk: Int = 4096): LoudnessMeter.Measurement {
        val meter = LoudnessMeter(rate, channels)
        var offset = 0
        while (offset < samples.size) {
            val size = minOf(chunk - (chunk % channels), samples.size - offset)
            meter.feed(samples.copyOfRange(offset, offset + size))
            offset += size
        }
        return meter.result()
    }

    @Test fun fullScaleSineIsAboutMinusTwoPointThreeLufs() {
        val result = measure(sine(1f, 8))
        assertEquals(-3.00f, result.lufs, 0.1f)
        assertEquals(8f, result.seconds, 0.2f)
    }

    @Test fun quieterSignalsMeasureLower() {
        val full = measure(sine(1f, 6))
        val quiet = measure(sine(0.1f, 6))
        // 20 dB less amplitude is 20 LU less; the K-weighting is identical for both.
        assertEquals(-20f, quiet.lufs - full.lufs, 0.4f)
        assertEquals(-23.0f, quiet.lufs, 0.1f)
    }

    @Test fun truePeakFollowsTheAmplitude() {
        val full = measure(sine(1f, 3))
        assertEquals(0f, full.truePeakDbTp, 0.2f)
        val half = measure(sine(0.5f, 3))
        assertEquals(-6f, half.truePeakDbTp, 0.3f)
    }

    @Test fun silenceIsReportedAsSilence() {
        val result = measure(FloatArray(48000 * 3))
        assertEquals(-70f, result.lufs, 0.001f)
    }

    @Test fun gatingIgnoresTheSilentHalf() {
        val loud = sine(0.5f, 6)
        val silence = FloatArray(loud.size)
        // 6 seconds of music followed by 6 seconds of silence: the gated result is the music.
        val result = measure(loud + silence)
        val onlyLoud = measure(loud)
        assertEquals(onlyLoud.lufs, result.lufs, 1.5f)
    }

    @Test fun monoAndStereoOfTheSameToneAgree() {
        val stereo = measure(sine(0.7f, 5, channels = 2))
        val mono = measure(sine(0.7f, 5, channels = 1), channels = 1)
        assertEquals(stereo.lufs, mono.lufs, 0.2f)
    }

    @Test fun theWeightingCurveIsTheBs1770One() {
        // The standard's curve is about +0.7 dB at 1 kHz and about -5.6 dB at 40 Hz.
        fun tone(frequency: Int, amplitude: Float, rate: Int): FloatArray {
            val frames = rate * 5
            val output = FloatArray(frames)
            for (frame in 0 until frames) output[frame] = (amplitude * sin(2 * Math.PI * frequency * frame / rate)).toFloat()
            return output
        }
        // 0.5 amplitude is -9.03 dBFS: 1 kHz reads -9.02 LUFS and 40 Hz about -14.6 LUFS.
        assertEquals(-9.02f, measure(tone(1000, 0.5f, 48000), channels = 1).lufs, 0.2f)
        assertEquals(-14.6f, measure(tone(40, 0.5f, 48000), channels = 1).lufs, 0.4f)
    }
}
