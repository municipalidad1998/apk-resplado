package com.streamvault.provider

import com.streamvault.data.Track
import com.streamvault.online.OnlineResult

/**
 * Where a song really lives. The two sources are kept apart everywhere: they are searched
 * separately, ranked separately and shown separately, and an online song is never turned into
 * a "local" file (or the other way round).
 */
enum class MusicSource(val key: String, val label: String, val badge: String) {
    LOCAL("local", "Teléfono", "Archivo local"),
    ONLINE("online", "Internet", "Internet")
}

/**
 * One search result, tagged with its source. The UI builds one list per source, so a hit can
 * never be shown as a local file when it comes from the internet.
 */
data class SearchHit(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val cover: String?,
    val source: MusicSource,
    /** Human readable origin: the folder on the phone, or the name of the online provider. */
    val origin: String,
    val score: Int = 0,
    val plays: Int = 0,
    val track: Track? = null,
    val online: OnlineResult? = null
) {
    /** A local and an online hit with the same title are two different rows, never merged. */
    val key: String get() = "${source.key}-$id"
}

/** Thrown when a provider cannot answer: no connection, no permission, invalid metadata. */
class ProviderUnavailableException(message: String) : Exception(message)

/**
 * A place where music can be found. The search engine only knows this interface, so the phone
 * library and any online catalogue are interchangeable and testable in isolation.
 */
interface MusicProvider {
    val source: MusicSource
    val label: String
    /** True when the provider can answer right now (the phone always can, the internet may not). */
    fun available(): Boolean
    suspend fun search(query: String, limit: Int = 40): List<SearchHit>
}
