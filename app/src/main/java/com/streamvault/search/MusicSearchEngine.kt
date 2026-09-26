package com.streamvault.search

import com.streamvault.provider.MusicProvider
import com.streamvault.provider.MusicSource
import com.streamvault.provider.ProviderUnavailableException
import com.streamvault.provider.SearchHit

/** Which list the search screen is showing. TODOS keeps the two sources visibly apart. */
enum class SearchTab(val label: String) { PHONE("Teléfono"), ONLINE("Online"), ALL("Todos") }

/**
 * The answer to one query, with the two sources separated from the start. The engine never
 * merges them into a single ranked list: the phone and the internet each keep their own results,
 * and the screen decides how to show them (one tab each, or both with their own headers).
 */
data class SearchOutcome(
    val query: String = "",
    val local: List<SearchHit> = emptyList(),
    val online: List<SearchHit> = emptyList(),
    val searching: Boolean = false,
    val onlineError: String? = null,
    val offline: Boolean = false
) {
    val isEmpty: Boolean get() = local.isEmpty() && online.isEmpty()
    fun of(source: MusicSource): List<SearchHit> = if (source == MusicSource.LOCAL) local else online
}

/**
 * Orchestrates the providers: the phone is always searched first and works offline; the internet
 * is only asked when there is a connection, and its failures never hide the local results.
 */
class MusicSearchEngine(
    private val local: LocalMusicProvider,
    private val online: OnlineMusicProvider
) {

    /** Only the phone. Used by the library screen, where online music must not appear. */
    suspend fun localHits(query: String, limit: Int = 200): List<SearchHit> =
        runCatching { local.search(query, limit) }.getOrDefault(emptyList())

    suspend fun search(query: String, limit: Int = 40): SearchOutcome {
        val clean = query.trim()
        if (clean.length < 2) return SearchOutcome(clean)
        // 1. The phone: no network, no permission beyond the library, always answered.
        val localHits = runCatching { local.search(clean, limit) }.getOrDefault(emptyList())
        // 2. The internet: only when it is there, and never replacing the local answer.
        if (!online.available()) {
            return SearchOutcome(clean, local = localHits, offline = true,
                onlineError = "Sin conexión a Internet. Tu música del teléfono sigue disponible.")
        }
        return try {
            SearchOutcome(clean, local = localHits, online = online.search(clean, limit))
        } catch (unavailable: ProviderUnavailableException) {
            SearchOutcome(clean, local = localHits, offline = true, onlineError = unavailable.message)
        } catch (error: Exception) {
            SearchOutcome(clean, local = localHits, onlineError = error.message ?: "No se pudo buscar en Internet")
        }
    }
}
