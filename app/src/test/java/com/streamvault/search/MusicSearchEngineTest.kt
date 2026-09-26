package com.streamvault.search

import com.streamvault.provider.MusicProvider
import com.streamvault.provider.MusicSource
import com.streamvault.provider.ProviderUnavailableException
import com.streamvault.provider.SearchHit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The engine keeps the two sources apart: the phone is always answered, the internet is only
 * asked when it exists, and a failure online never hides the local results.
 */
class MusicSearchEngineTest {

    private fun hit(id: String, title: String, artist: String, source: MusicSource) =
        SearchHit(id, title, artist, "", 100_000L, null, source, "origen")

    private fun fake(source: MusicSource, hits: List<SearchHit>, boom: Boolean = false, available: Boolean = true) =
        object : MusicProvider {
            override val source = source
            override val label = "fake"
            override fun available() = available
            override suspend fun search(query: String, limit: Int): List<SearchHit> {
                if (boom) throw ProviderUnavailableException("Sin conexión a Internet")
                return hits
            }
        }

    @Test fun localAndOnlineAreNeverMerged() = runBlocking {
        val engine = MusicSearchEngine(
            fake(MusicSource.LOCAL, listOf(hit("1", "Tu Poeta", "Alex Campos", MusicSource.LOCAL))),
            fake(MusicSource.ONLINE, listOf(hit("2", "Tu Poeta", "Alex Campos", MusicSource.ONLINE)))
        )
        val outcome = engine.search("tu poeta")
        assertEquals(1, outcome.local.size)
        assertEquals(1, outcome.online.size)
        assertEquals(MusicSource.LOCAL, outcome.local.first().source)
        assertEquals(MusicSource.ONLINE, outcome.online.first().source)
        // The same song in both worlds is two rows, never one.
        assertTrue(outcome.local.first().key != outcome.online.first().key)
    }

    @Test fun withoutConnectionOnlyThePhoneAnswers() = runBlocking {
        val engine = MusicSearchEngine(
            fake(MusicSource.LOCAL, listOf(hit("1", "Canción", "La IBI", MusicSource.LOCAL))),
            fake(MusicSource.ONLINE, emptyList(), available = false),
        )
        val outcome = engine.search("la ibi")
        assertTrue(outcome.offline)
        assertEquals(1, outcome.local.size)
        assertTrue(outcome.online.isEmpty())
    }

    @Test fun anOnlineFailureKeepsTheLocalResults() = runBlocking {
        val engine = MusicSearchEngine(
            fake(MusicSource.LOCAL, listOf(hit("1", "Canción", "La IBI", MusicSource.LOCAL))),
            fake(MusicSource.ONLINE, emptyList(), boom = true),
        )
        val outcome = engine.search("la ibi")
        assertEquals(1, outcome.local.size)
        assertTrue(outcome.online.isEmpty())
        assertTrue(outcome.onlineError!!.contains("Internet"))
    }

    @Test fun aShortQueryDoesNothing() = runBlocking {
        val engine = MusicSearchEngine(fake(MusicSource.LOCAL, emptyList()), fake(MusicSource.ONLINE, emptyList()))
        assertTrue(engine.search("a").isEmpty)
    }
}
