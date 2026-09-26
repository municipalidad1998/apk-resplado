package com.streamvault.analysis

import kotlin.math.log10
import kotlin.math.pow

/**
 * ITU-R BS.1770-4 loudness meter: K-weighting, 400 ms blocks with 75 % overlap, the two gates
 * (absolute at −70 LUFS and relative at −10 LU) and a 4x oversampled true peak estimate.
 *
 * The K-weighting filters come from the standard's analogue prototype digitised with a bilinear
 * transform, so the curve is right at any sample rate (48 kHz reproduces the published
 * coefficients exactly): +4 dB at 10 kHz, about +0.7 dB at 1 kHz and −5.6 dB at 40 Hz.
 */
class LoudnessMeter(val sampleRate: Int, channelCount: Int) {

    data class Measurement(val lufs: Float, val truePeakDbTp: Float, val seconds: Float)

    private class Biquad(b0: Float, b1: Float, b2: Float, a1: Float, a2: Float) {
        private val b0f = b0; private val b1f = b1; private val b2f = b2
        private val a1f = a1; private val a2f = a2
        private var x1 = 0f; private var x2 = 0f; private var y1 = 0f; private var y2 = 0f
        fun copy() = Biquad(b0f, b1f, b2f, a1f, a2f)
        fun process(input: Float): Float {
            val output = b0f * input + b1f * x1 + b2f * x2 - a1f * y1 - a2f * y2
            x2 = x1; x1 = input; y2 = y1; y1 = output
            return output
        }
    }

    private val channels = channelCount.coerceAtLeast(1)
    private val blockSize = (sampleRate * 0.4).toInt().coerceAtLeast(4)
    private val hop = (blockSize / 4).coerceAtLeast(1)
    private val chains: Array<Array<Biquad>>
    private val sums = DoubleArray(OVERLAP)
    private val counts = IntArray(OVERLAP)
    private val powers = ArrayList<Double>()
    private val previous = DoubleArray(channels)
    private var blockIndex = 0
    private var framesInBlock = 0
    private var peak = 0.0
    private var totalFrames = 0L

    init {
        val prototypes = arrayOf(
            coefficients(SHELF_B, SHELF_A, sampleRate),
            coefficients(RLB_B, RLB_A, sampleRate)
        )
        chains = Array(channels) {
            arrayOf(Biquad(prototypes[0][0], prototypes[0][1], prototypes[0][2], prototypes[0][3], prototypes[0][4]),
                Biquad(prototypes[1][0], prototypes[1][1], prototypes[1][2], prototypes[1][3], prototypes[1][4]))
        }
    }

    /** Interleaved PCM, already normalised to −1..1. */
    fun feed(samples: FloatArray) {
        var offset = 0
        while (offset + channels <= samples.size) {
            for (channel in 0 until channels) {
                val value = samples[offset + channel].coerceIn(-1f, 1f).toDouble()
                // True peak: linear interpolation x4 between consecutive samples.
                val before = previous[channel]
                for (step in 0 until OVERSAMPLING) {
                    val interpolated = before + (value - before) * (step / OVERSAMPLING.toDouble())
                    val magnitude = if (interpolated < 0) -interpolated else interpolated
                    if (magnitude > peak) peak = magnitude
                }
                previous[channel] = value
                val weighted = chains[channel][1].process(chains[channel][0].process(value.toFloat())).toDouble()
                val square = weighted * weighted
                // With 75 % overlap every sample belongs to four consecutive blocks, so it feeds
                // four rolling sums; each one is closed when its own 400 ms window is complete.
                for (phase in 0 until OVERLAP) sums[(blockIndex - phase + OVERLAP) % OVERLAP] += square
            }
            for (phase in 0 until OVERLAP) counts[phase]++
            framesInBlock++
            totalFrames++
            offset += channels
            if (framesInBlock == hop) {
                framesInBlock = 0
                val slot = (blockIndex + 1) % OVERLAP
                if (counts[slot] >= blockSize) {
                    // Mean square per channel, so mono and stereo of the same tone agree.
                    powers += sums[slot] / (counts[slot].toDouble() * channels)
                    sums[slot] = 0.0
                    counts[slot] = 0
                }
                blockIndex++
            }
        }
    }

    fun result(): Measurement {
        val seconds = totalFrames / sampleRate.toDouble()
        val audible = powers.map(::toLu).filter { it > ABSOLUTE_GATE_LUFS }
        if (audible.isEmpty()) return Measurement(SILENT_LUFS, toDb(peak), seconds.toFloat())
        val ungated = toLu(audible.map { 10.0.pow((it + 0.691) / 10.0) }.average())
        val relative = ungated - RELATIVE_GATE_LU
        val kept = audible.filter { it >= relative }
        val mean = kept.map { 10.0.pow((it + 0.691) / 10.0) }.average()
        return Measurement(toLu(mean).toFloat(), toDb(peak), seconds.toFloat())
    }

    private fun toLu(power: Double): Double = when {
        power <= 0.0 -> -120.0
        else -> (-0.691 + 10.0 * log10(power)).coerceIn(-70.0, 10.0)
    }

    private fun toDb(value: Double): Float = if (value <= 0.0) -70f else (20.0 * log10(value)).toFloat().coerceIn(-70f, 6f)

    private companion object {
        const val OVERSAMPLING = 4
        const val OVERLAP = 4
        const val ABSOLUTE_GATE_LUFS = -70.0
        const val RELATIVE_GATE_LU = 10.0
        const val SILENT_LUFS = -70f
        val SHELF_B = doubleArrayOf(1.4075861598943982e-08, 0.00016774276859503974, 1.0)
        val SHELF_A = doubleArrayOf(8.8814279155188241e-09, 0.00013326446280991579, 1.0)
        val RLB_B = doubleArrayOf(1.7504273501765486e-05, 0.0, 0.0)
        val RLB_A = doubleArrayOf(1.7417275972397586e-05, 0.0083413461526517484, 1.0)

        /** Bilinear transform of the analogue prototype: s = 2·fs·(1−z⁻¹)/(1+z⁻¹). */
        fun coefficients(numerator: DoubleArray, denominator: DoubleArray, rate: Int): FloatArray {
            val c = 2.0 * rate
            val b0 = numerator[0] * c * c + numerator[1] * c + numerator[2]
            val b1 = -2.0 * numerator[0] * c * c + 2.0 * numerator[2]
            val b2 = numerator[0] * c * c - numerator[1] * c + numerator[2]
            val a0 = denominator[0] * c * c + denominator[1] * c + denominator[2]
            val a1 = -2.0 * denominator[0] * c * c + 2.0 * denominator[2]
            val a2 = denominator[0] * c * c - denominator[1] * c + denominator[2]
            return floatArrayOf((b0 / a0).toFloat(), (b1 / a0).toFloat(), (b2 / a0).toFloat(), (a1 / a0).toFloat(), (a2 / a0).toFloat())
        }
    }
}
