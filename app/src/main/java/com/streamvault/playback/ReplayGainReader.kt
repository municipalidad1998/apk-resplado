package com.streamvault.playback

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

/**
 * Lector ligero de etiquetas ReplayGain (FLAC: Vorbis comments).
 * Busca REPLAYGAIN_TRACK_GAIN / REPLAYGAIN_ALBUM_GAIN en los primeros
 * bloques de metadatos del archivo. Nunca modifica el archivo.
 */
object ReplayGainReader {

    data class Gain(val trackGainDb: Float?, val albumGainDb: Float?)

    /** Devuelve las ganancias en dB o nulls si el archivo no tiene etiquetas. */
    suspend fun read(path: String): Gain = withContext(Dispatchers.IO) {
        try {
            val f = File(path)
            if (!f.exists()) return@withContext Gain(null, null)
            val head = ByteArray(256 * 1024)
            val n = FileInputStream(f).use { it.read(head) }
            if (n <= 0) return@withContext Gain(null, null)
            val text = String(head, 0, n, Charsets.ISO_8859_1)
            Gain(
                trackGainDb = extract(text, "replaygain_track_gain"),
                albumGainDb = extract(text, "replaygain_album_gain")
            )
        } catch (e: Exception) {
            Gain(null, null)
        }
    }

    private fun extract(text: String, tag: String): Float? {
        val idx = text.indexOf(tag, ignoreCase = true)
        if (idx < 0) return null
        val eq = text.indexOf('=', idx + tag.length)
        if (eq < 0 || eq > idx + tag.length + 40) return null
        val end = text.indexOf('\u0000', eq + 1).let { if (it < 0) eq + 12 else it }
        val value = text.substring(eq + 1, minOf(end, eq + 12))
            .replace("dB", "", ignoreCase = true).trim()
        return value.toFloatOrNull()
    }
}
