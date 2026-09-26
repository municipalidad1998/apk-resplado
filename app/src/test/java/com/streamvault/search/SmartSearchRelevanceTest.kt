package com.streamvault.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The cases the user reported. These are the queries that used to bring back unrelated artists,
 * so they are asserted one by one: exact artist first, no lookalikes, no promotion of a name
 * that only shares a word.
 */
class SmartSearchRelevanceTest {

    private fun c(title: String, artist: String, album: String = "", plays: Int = 0) =
        SmartSearch.Candidate("${title}_$artist", title, artist, album, "local", plays)

    private fun ranked(query: String, candidates: List<SmartSearch.Candidate>) = SmartSearch.rank(candidates, query)

    private val discs = listOf(
        c("Tu Poeta", "Alex Campos"),
        c("Al Taller del Maestro", "Alex Campos"),
        c("Sueño de Morir", "Alex Campos"),
        c("Algo Más", "Alexander Acha"),
        c("Campos Verdes", "Otro Cantante"),
        c("Alex", "Otro Cantante")
    )

    @Test fun searchingAnArtistOnlyShowsThatArtist() {
        val result = ranked("Alex Campos", discs)
        assertEquals(3, result.size)
        assertTrue(result.all { it.candidate.artist == "Alex Campos" })
    }

    @Test fun similarNamesAreNotPromoted() {
        val result = ranked("Alex Campos", discs)
        assertTrue(result.none { it.candidate.artist == "Alexander Acha" })
        assertTrue(result.none { it.candidate.title == "Alex" })
        assertTrue(result.none { it.candidate.title == "Campos Verdes" })
    }

    @Test fun laIbiBeatsItsLookalikes() {
        val candidates = listOf(
            c("Canción Uno", "La IBI"),
            c("Canción Dos", "La IBI"),
            c("La IBI (en vivo)", "La IBI"),
            c("Ibi Town", "Otro Grupo"),
            c("IBIS", "Otro Grupo")
        )
        val result = ranked("La IBI", candidates)
        assertEquals(3, result.size)
        assertTrue(result.all { it.candidate.artist == "La IBI" })
        assertTrue(result.none { it.candidate.title == "Ibi Town" })
        assertTrue(result.none { it.candidate.title == "IBIS" })
    }

    @Test fun theArticleIsOptionalForBothSides() {
        // The file says "IBI", the user writes "La IBI": the same artist.
        val result = ranked("La IBI", listOf(c("Canción", "IBI"), c("Ibi Town", "Otro Grupo")))
        assertEquals(1, result.size)
        assertEquals("Canción", result.first().candidate.title)
    }

    @Test fun artistAndTitleTogetherIsTheExactSong() {
        val result = ranked("Tu poeta Alex Campos", discs + c("Tu Poeta", "Otro Cantante"))
        assertEquals("Alex Campos", result.first().candidate.artist)
        assertEquals("Tu Poeta", result.first().candidate.title)
        assertTrue(result.first().score >= SmartSearch.EXACT_ARTIST_TITLE)
        // The same title by somebody else is not shown above, and not at all next to the exact one.
        assertEquals(1, result.size)
    }

    @Test fun aSongQueryKeepsItsOwnVersionsOnly() {
        val candidates = listOf(
            c("Al Taller del Maestro", "Alex Campos"),
            c("Al Taller del Maestro (en vivo)", "Alex Campos"),
            c("Otra Canción", "Alex Campos")
        )
        val result = ranked("Al taller del maestro", candidates)
        assertEquals("Al Taller del Maestro", result.first().candidate.title)
        assertEquals(2, result.size)
        assertTrue(result.any { it.candidate.title.contains("en vivo") })
    }

    @Test fun aSongWrittenWithTheArtistFirstAlsoWorks() {
        val result = ranked("Alex Campos - Tu Poeta", discs)
        assertEquals("Tu Poeta", result.first().candidate.title)
        assertEquals("Alex Campos", result.first().candidate.artist)
    }

    @Test fun typosStillFindTheArtistWhenNothingIsExact() {
        val result = ranked("alex campo", listOf(c("Sueño de Morir", "Alex Campos")))
        assertTrue(result.isNotEmpty())
        assertEquals("Alex Campos", result.first().candidate.artist)
    }

    @Test fun creditedArtistsStayBelowTheExactOne() {
        val candidates = listOf(
            c("Mi Sueño", "Alex Campos, Marcos Witt"),
            c("Tu Poeta", "Alex Campos")
        )
        val result = ranked("Alex Campos", candidates)
        // Exact first; the collaboration is only offered when nothing exact exists.
        assertEquals("Alex Campos", result.first().candidate.artist)
    }

    @Test fun oneSharedWordIsNotEnough() {
        // "Alex" alone must not answer a search for "Alex Campos".
        val result = ranked("Alex Campos", listOf(c("Alex", "Otro Cantante")))
        assertTrue(result.isEmpty())
    }
}
