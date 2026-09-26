package com.streamvault.provider

import androidx.sqlite.db.SimpleSQLiteQuery
import com.streamvault.LuminaApp
import com.streamvault.data.Track
import com.streamvault.search.SmartSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The phone library. It never touches the network: without Wi-Fi or data the local music keeps
 * working exactly the same, which is the whole point of keeping the sources apart.
 *
 * Only files that physically exist are returned (`available = 1`), so a song whose file was
 * deleted never appears as if it could be played.
 */
class LocalMusicProvider(private val app: LuminaApp) : MusicProvider {

    override val source = MusicSource.LOCAL
    override val label = "Música del teléfono"
    override fun available(): Boolean = true

    /** Cheap SQL pre-filter: it only has to bring candidates, the ranking decides the order. */
    suspend fun candidates(query: String, limit: Int = 800): List<Track> {
        val tokens = SmartSearch.normalize(query).split(' ').filter { it.length > 1 }.distinct().take(6)
        if (tokens.isEmpty()) return emptyList()
        val where = tokens.joinToString(" OR ") {
            "(title LIKE ? ESCAPE '\\' OR customName LIKE ? ESCAPE '\\' OR artist LIKE ? ESCAPE '\\'" +
                " OR album LIKE ? ESCAPE '\\' OR albumArtist LIKE ? ESCAPE '\\' OR fileName LIKE ? ESCAPE '\\')"
        }
        val args = tokens.flatMap { token -> List(6) { "%${escape(token)}%" } }
        // source != 'online': a stream from the internet is not a file on the phone, ever.
        val sql = "SELECT * FROM tracks WHERE hidden = 0 AND available = 1 AND source != 'online' AND ($where) LIMIT $limit"
        return withContext(Dispatchers.IO) { app.library.candidates(SimpleSQLiteQuery(sql, args.toTypedArray())) }
    }

    override suspend fun search(query: String, limit: Int): List<SearchHit> {
        val clean = query.trim()
        if (clean.length < 2) return emptyList()
        val rows = candidates(clean)
        if (rows.isEmpty()) return emptyList()
        return SmartSearch.rankItems(rows, clean) { track ->
            SmartSearch.Candidate(track.id, track.displayName, track.artist, track.album, "local", track.plays, track.durationMs)
        }.map { it.toHit() }.take(limit)
    }

    private fun Track.toHit() = SearchHit(
        id = id,
        title = displayName,
        artist = artist,
        album = album,
        durationMs = durationMs,
        cover = cover,
        source = MusicSource.LOCAL,
        origin = folder.ifBlank { "Archivo local" },
        plays = plays,
        track = this
    )

    private fun escape(token: String) = token.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
