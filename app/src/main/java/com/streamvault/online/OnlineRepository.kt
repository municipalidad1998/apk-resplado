package com.streamvault.online

import com.streamvault.LuminaApp
import com.streamvault.data.Track
import com.streamvault.network.NetState
import java.security.MessageDigest

/**
 * Turns provider results into real tracks in the library, so online music plays through the same
 * Media3 session, queue, favourites and playlists as the local FLAC files.
 */
class OnlineRepository(private val app: LuminaApp) {

    val providers: List<OnlineProvider> = listOf(ArchiveProvider())

    suspend fun search(query: String, limit: Int = 40): List<OnlineResult> {
        val clean = query.trim()
        if (clean.length < 2) return emptyList()
        return providers.firstOrNull { it.canStream }?.search(clean, limit).orEmpty()
    }

    /** Name of the provider that served a result, so the row can say where it comes from. */
    fun labelOf(key: String): String = providers.firstOrNull { it.key == key }?.label ?: "Internet"

    /**
     * Resolves a result into a playable track and stores it. Online tracks keep
     * `source = "online"` so the scanner never marks them as missing files.
     */
    suspend fun track(result: OnlineResult, quality: Quality, net: NetState): Track {
        val wifiOnly = app.preferences.state.value.wifiOnly
        val mobileData = app.preferences.state.value.mobileData
        val stream = QualityChooser.choose(result.streams, quality, net, wifiOnly, mobileData)
            ?: error(if (!net.online) "Sin conexión: conéctate a Internet para escuchar música online."
            else if (wifiOnly && net.metered) "Tienes activado «Solo Wi‑Fi» y ahora usas datos móviles."
            else "No se pudo obtener un stream de audio para esta canción.")
        val id = "on-" + sha(stream.url)
        val existing = app.library.track(id)
        val track = Track(
            id = id,
            uri = stream.url,
            fileName = result.title,
            title = result.title,
            artist = result.artist,
            album = result.album,
            genre = if (result.license.isBlank()) "Música libre" else "Música libre · licencia",
            folder = "Internet Archive",
            durationMs = (result.durationSeconds * 1000).toLong(),
            size = stream.sizeBytes,
            date = System.currentTimeMillis(),
            cover = result.cover,
            source = "online",
            analysisKey = "online"
        )
        if (existing == null) app.library.insert(track) else app.library.update(existing.copy(uri = stream.url, durationMs = track.durationMs, cover = result.cover ?: existing.cover))
        return app.library.track(id) ?: track
    }

    private fun sha(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }.take(32)
}
