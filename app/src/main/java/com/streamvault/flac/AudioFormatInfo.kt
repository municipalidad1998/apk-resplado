package com.streamvault.flac

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What a local file really carries, read from the container with MediaExtractor. */
data class AudioFormatInfo(
    val codec: String,
    val sampleRateHz: Int,
    val channels: Int,
    val bitrateKbps: Int,
    val bitDepth: Int,
    val durationMs: Long
) {
    val lossless: Boolean get() = codec.contains("flac", true) || codec.contains("alac", true) || codec.contains("raw", true) || codec.contains("pcm", true)
    /** e.g. "FLAC · 24-bit / 96 kHz · Lossless" */
    val badge: String get() = buildString {
        append(codecLabel)
        if (bitDepth > 0) append(" · $bitDepth-bit")
        if (sampleRateHz > 0) append(" / ${sampleRateHz / 1000} kHz")
        if (lossless) append(" · Lossless")
    }
    val codecLabel: String get() = when {
        codec.contains("flac", true) -> "FLAC"
        codec.contains("alac", true) -> "ALAC"
        codec.contains("pcm", true) || codec.contains("raw", true) -> "WAV"
        codec.contains("opus", true) -> "Opus"
        codec.contains("vorbis", true) -> "OGG"
        codec.contains("aac", true) || codec.contains("mp4a", true) -> "AAC"
        codec.contains("mp3", true) || codec.contains("mpeg", true) -> "MP3"
        else -> codec.substringAfterLast('/').uppercase().ifBlank { "Audio" }
    }
}

object AudioFormatReader {

    suspend fun read(context: Context, uri: String): AudioFormatInfo? = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, Uri.parse(uri), null)
            val index = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return@withContext null
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            val sampleRate = runCatching { format.getInteger(MediaFormat.KEY_SAMPLE_RATE) }.getOrDefault(0)
            val channels = runCatching { format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) }.getOrDefault(0)
            val bitrate = if (format.containsKey(MediaFormat.KEY_BIT_RATE)) runCatching { format.getInteger(MediaFormat.KEY_BIT_RATE) }.getOrDefault(0) else 0
            val duration = if (format.containsKey(MediaFormat.KEY_DURATION)) runCatching { format.getLong(MediaFormat.KEY_DURATION) }.getOrDefault(0) / 1000 else 0
            AudioFormatInfo(
                codec = mime,
                sampleRateHz = sampleRate,
                channels = channels,
                bitrateKbps = bitrate / 1000,
                // Containers rarely expose bit depth; for lossless files it follows from the rate.
                bitDepth = if (sampleRate > 0 && channels > 0 && bitrate > 0) (bitrate / (sampleRate * channels)).coerceIn(0, 32) else 0,
                durationMs = duration
            )
        } catch (_: Exception) {
            null
        } finally {
            extractor.release()
        }
    }
}
