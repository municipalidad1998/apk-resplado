package com.streamvault.analysis

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

class SilenceAnalyzer(private val context: Context) {
    data class Result(val offsetMs: Long, val waveform: String, val foundSound: Boolean)
    suspend fun analyze(uri: String, thresholdDb: Int, minimumSeconds: Int): Result = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(context, Uri.parse(uri), null)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                ?: error("No hay una pista de audio compatible")
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            decoder = codec
            codec.configure(format, null, null, 0); codec.start()
            var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var encoding = AudioFormat.ENCODING_PCM_16BIT
            val gate = SilenceGate(thresholdDb, minimumSeconds * 1000L)
            val info = MediaCodec.BufferInfo()
            var inputEnded = false
            var finished = false
            var sum = 0.0; var samples = 0; var windowStart = 0L
            var peak = 0f; var windowCount = 0
            val peaks = mutableListOf<Float>()
            var lastOutput = System.nanoTime()
            while (!finished) {
                currentCoroutineContext().ensureActive()
                if ((System.nanoTime() - lastOutput) / 1_000_000 > 15_000) error("El decodificador no respondió")
                if (!inputEnded) {
                    val index = codec.dequeueInputBuffer(10_000)
                    if (index >= 0) {
                        val buffer = codec.getInputBuffer(index)!!
                        val length = extractor.readSampleData(buffer, 0)
                        if (length < 0) { codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputEnded = true }
                        else { codec.queueInputBuffer(index, 0, length, extractor.sampleTime, 0); extractor.advance() }
                    }
                }
                when (val index = codec.dequeueOutputBuffer(info, 10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val output = codec.outputFormat
                        rate = output.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = output.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        encoding = if (output.containsKey(MediaFormat.KEY_PCM_ENCODING)) output.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
                        check(encoding == AudioFormat.ENCODING_PCM_16BIT || encoding == AudioFormat.ENCODING_PCM_FLOAT) { "Formato PCM no compatible" }
                    }
                    else -> if (index >= 0) {
                        lastOutput = System.nanoTime()
                        try {
                            val buffer = codec.getOutputBuffer(index)!!.order(ByteOrder.LITTLE_ENDIAN)
                            buffer.position(info.offset); buffer.limit(info.offset + info.size)
                            val bytesPerSample = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
                            var n = 0
                            while (buffer.remaining() >= bytesPerSample && !finished) {
                                val value = if (bytesPerSample == 4) buffer.float.coerceIn(-1f, 1f) else buffer.short / 32768f
                                val time = info.presentationTimeUs / 1000 + n * 1000L / (rate * channels)
                                if (samples == 0) windowStart = time
                                sum += value * value; samples++; n++; peak = maxOf(peak, abs(value))
                                if (samples >= rate * channels / 50) {
                                    gate.window(sqrt(sum / samples), windowStart)
                                    sum = 0.0; samples = 0; windowCount++
                                    if (windowCount % 5 == 0) { peaks += peak; peak = 0f }
                                }
                                finished = time >= 120_000 || (gate.startMs != null && time > gate.startMs!! + 2500)
                            }
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) finished = true
                        } finally { codec.releaseOutputBuffer(index, false) }
                    }
                }
            }
            Result(gate.startMs ?: 0L, peaks.joinToString(",") { java.lang.String.format(java.util.Locale.US, "%.3f", it) }, gate.startMs != null)
        } finally { decoder?.let { runCatching { it.stop() }; it.release() }; extractor.release() }
    }
}
