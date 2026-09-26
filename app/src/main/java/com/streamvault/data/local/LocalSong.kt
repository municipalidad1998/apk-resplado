package com.streamvault.data.local

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Canción física del teléfono (MediaStore). NUNCA se mezcla con
 * fuentes online (YouTube / YouTube Music / Telegram Cloud).
 */
@Parcelize
data class LocalSong(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val track: Int? = null,
    val disc: Int? = null,
    val duration: Long = 0L,
    val contentUri: String,
    val albumArtUri: String? = null,
    val path: String? = null
) : Parcelable {

    /** Etiqueta de calidad, p. ej. "FLAC • LOSSLESS" según la extensión. */
    val codecLabel: String
        get() = when (path?.substringAfterLast('.', "")?.lowercase()) {
            "flac" -> "FLAC • LOSSLESS"
            "wav" -> "WAV • SIN COMPRESIÓN"
            "opus" -> "OPUS"
            "ogg" -> "OGG"
            "m4a", "aac" -> "AAC"
            else -> ""
        }
}
