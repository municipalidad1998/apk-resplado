package com.streamvault.provider

import com.streamvault.network.NetState
import com.streamvault.online.OnlineRepository
import com.streamvault.online.OnlineResult
import com.streamvault.online.ResultValidator
import com.streamvault.search.SmartSearch

/**
 * Everything that comes from the internet. It is queried only when there is a connection, it
 * validates the metadata before handing a result over, and it ranks with the same rules as the
 * phone library so the best answer is the first one in both lists.
 */
class OnlineMusicProvider(
    private val repository: OnlineRepository,
    private val net: () -> NetState
) : MusicProvider {

    override val source = MusicSource.ONLINE
    override val label = "Música online"
    override fun available(): Boolean = net().online

    override suspend fun search(query: String, limit: Int): List<SearchHit> {
        val clean = query.trim()
        if (clean.length < 2) return emptyList()
        if (!net().online) throw ProviderUnavailableException("Sin conexión a Internet")
        val validated = ResultValidator.clean(repository.search(clean, limit))
        if (validated.isEmpty()) return emptyList()
        return SmartSearch.rankItems(validated, clean) { result ->
            SmartSearch.Candidate(result.id, result.title, result.artist, result.album, result.source, 0, (result.durationSeconds * 1000).toLong())
        }.map { result -> result.toHit(repository.labelOf(result.source)) }.take(limit)
    }

    private fun OnlineResult.toHit(providerLabel: String) = SearchHit(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = (durationSeconds * 1000).toLong(),
        cover = cover,
        source = MusicSource.ONLINE,
        origin = providerLabel,
        online = this
    )
}
