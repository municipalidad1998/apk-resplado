package com.streamvault.online

/**
 * Metadata gate for online results. Nothing reaches the screen without passing through here:
 * a result without a title or without a playable stream is not a result, it is noise, and it is
 * exactly the kind of noise that made the old search look random.
 *
 * Pure function, so it is tested without any network access.
 */
object ResultValidator {

    const val UNKNOWN_ARTIST = "Artista desconocido"
    const val UNKNOWN_ALBUM = "Sin álbum"
    private const val MAX_TITLE = 160

    /**
     * Drops what cannot be shown and fills what is missing.
     * Every surviving result carries title, artist, album, duration, cover, source and id.
     */
    fun clean(results: Iterable<OnlineResult>): List<OnlineResult> = results.mapNotNull { result ->
        val title = result.title.trim().replace(Regex("\\s+"), " ")
        if (title.length < 2) return@mapNotNull null
        val streams = result.streams.filter { it.url.isNotBlank() && it.url.startsWith("http") }
        if (streams.isEmpty()) return@mapNotNull null
        val id = result.id.ifBlank { streams.first().url }
        val duration = if (result.durationSeconds.isFinite() && result.durationSeconds > 0f) result.durationSeconds
            else streams.firstOrNull { it.durationSeconds > 0f }?.durationSeconds ?: 0f
        result.copy(
            id = id,
            title = title.take(MAX_TITLE),
            artist = result.artist.trim().ifBlank { UNKNOWN_ARTIST },
            album = result.album.trim().ifBlank { UNKNOWN_ALBUM },
            durationSeconds = duration,
            streams = streams,
            source = result.source.ifBlank { "online" }
        )
    }.distinctBy { it.id }
}
