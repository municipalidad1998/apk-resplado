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
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Measures how loud a file actually is by decoding real PCM, the same way the silence detector does.
 *
 * The result is the gated BS.1770 loudness (LUFS) of a window of the track, measured with the
 * K-weighting curve and with an estimate of the true peak, so the gain applied at playback can be
 * limited before it distorts. The window keeps the analysis fast, so a track that is quiet at the
 * start and loud at the end is measured from the region it actually samples.
 */
class LoudnessAnalyzer(private val context: Context) {

    /** [lufs] is the gated BS.1770 loudness and [peakDb] the estimated true peak in dBTP. */
    data class Result(val lufs: Float, val peakDb: Float, val seconds: Float)

    suspend fun measure(uri: String, startMs: Long = 0, windowSeconds: Int = DEFAULT_WINDOW_SECONDS): Result = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(context, Uri.parse(uri), null)
            val trackIndex = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("No hay una pista de audio compatible")
            extractor.selectTrack(trackIndex)
            if (startMs > 0) runCatching { extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC) }
            val format = extractor.getTrackFormat(trackIndex)
            val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            decoder = codec
            codec.configure(format, null, null, 0)
            codec.start()
            var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var encoding = AudioFormat.ENCODING_PCM_16BIT
            val info = MediaCodec.BufferInfo()
            var inputEnded = false
            var finished = false
            var meter: LoudnessMeter? = null
            var firstUs = -1L
            var lastUs = 0L
            var lastOutput = System.nanoTime()
            val limitUs = windowSeconds * 1_000_000L
            while (!finished) {
                currentCoroutineContext().ensureActive()
                if ((System.nanoTime() - lastOutput) / 1_000_000 > 15_000) error("El decodificador no respondió")
                if (!inputEnded) {
                    val index = codec.dequeueInputBuffer(10_000)
                    if (index >= 0) {
                        val buffer = codec.getInputBuffer(index)!!
                        val length = extractor.readSampleData(buffer, 0)
                        if (length < 0) {
                            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnded = true
                        } else {
                            codec.queueInputBuffer(index, 0, length, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                when (val index = codec.dequeueOutputBuffer(info, 10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val output = codec.outputFormat
                        rate = output.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = output.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        encoding = if (output.containsKey(MediaFormat.KEY_PCM_ENCODING)) output.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
                        check(encoding == AudioFormat.ENCODING_PCM_16BIT || encoding == AudioFormat.ENCODING_PCM_FLOAT) { "Formato PCM no compatible" }
                        // A new rate or layout needs a new meter: the K-weighting depends on the rate.
                        meter = LoudnessMeter(rate, channels)
                    }
                    else -> if (index >= 0) {
                        lastOutput = System.nanoTime()
                        try {
                            val buffer = codec.getOutputBuffer(index)!!.order(ByteOrder.LITTLE_ENDIAN)
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            if (firstUs < 0) firstUs = info.presentationTimeUs
                            lastUs = info.presentationTimeUs
                            val bytesPerSample = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
                            val active = meter ?: LoudnessMeter(rate, channels).also { meter = it }
                            val chunk = FloatArray(buffer.remaining() / bytesPerSample)
                            var index = 0
                            while (buffer.remaining() >= bytesPerSample) {
                                chunk[index++] = if (bytesPerSample == 4) buffer.float.coerceIn(-1f, 1f) else buffer.short / 32768f
                            }
                            active.feed(if (index == chunk.size) chunk else chunk.copyOfRange(0, index))
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) finished = true
                            else if (lastUs - firstUs >= limitUs) finished = true
                        } finally {
                            codec.releaseOutputBuffer(index, false)
                        }
                    }
                }
            }
            val measurement = meter?.result() ?: error("No se pudo decodificar audio para medir el volumen")
            if (measurement.seconds <= 0.2f) error("No se pudo decodificar audio para medir el volumen")
            Result(lufs = measurement.lufs, peakDb = measurement.truePeakDbTp, seconds = measurement.seconds)
        } finally {
            decoder?.let { runCatching { it.stop() }; it.release() }
            extractor.release()
        }
    }

    private fun toDb(value: Float): Float = if (value <= 0f) SILENCE_DB else (20f * log10(value)).coerceIn(SILENCE_DB, 0f)


    companion object {
        const val DEFAULT_WINDOW_SECONDS = 90
        const val SILENCE_DB = -70f
    }
}
