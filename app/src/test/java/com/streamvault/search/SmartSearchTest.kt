package com.streamvault.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartSearchTest {

    private fun c(title: String, artist: String, album: String = "", plays: Int = 0) =
        SmartSearch.Candidate("${title}_$artist", title, artist, album, "local", plays)

    private fun top(query: String, candidates: List<SmartSearch.Candidate>) = SmartSearch.rank(candidates, query).firstOrNull()

    @Test fun normalizationIgnoresAccentsCaseAndPunctuation() {
        assertEquals("tu poeta", SmartSearch.normalize("Tú Poeta"))
        assertEquals("al taller del maestro", SmartSearch.normalize("Al Taller del Maestro!"))
        assertEquals("alex campos", SmartSearch.normalize("ALEX  CAMPOS"))
    }

    @Test fun splitsArtistAndTitle() {
        val dash = SmartSearch.parse("Alex Campos - Tu Poeta")
        assertEquals(listOf("alex campos", "tu poeta"), dash.parts)
        val byWord = SmartSearch.parse("Tu poeta de Alex Campos")
        assertEquals(listOf("tu poeta", "alex campos"), byWord.parts)
        val plain = SmartSearch.parse("Tu poeta Alex Campos")
        assertEquals(1, plain.parts.size)
    }

    @Test fun exactArtistAndTitleWins() {
        val candidates = listOf(
            c("Tu Poeta", "Alex Campos"),
            c("Tu Poeta", "Otro Cantante"),
            c("Solo Dios Sabe", "Alex Campos")
        )
        val ranked = SmartSearch.rank(candidates, "Tu poeta Alex Campos")
        assertEquals("Alex Campos", ranked.first().candidate.artist)
        assertEquals("Tu Poeta", ranked.first().candidate.title)
        assertTrue(ranked.first().score >= 60)
    }

    @Test fun artistAloneRanksTheArtistFirstAndDropsTheRest() {
        val candidates = listOf(
            c("Al Taller del Maestro", "Alex Campos"),
            c("Tu Poeta", "Alex Campos"),
            c("La IBI", "La IBI"),
            c("Reguetón de otro", "DJ Distinto")
        )
        val ranked = SmartSearch.rank(candidates, "Alex Campos")
        assertTrue(ranked.all { "alex campos" in SmartSearch.normalize(it.candidate.artist) })
        assertEquals(2, ranked.size)
    }

    @Test fun exactSongBeatsSimilarTitles() {
        val candidates = listOf(
            c("La IBI", "La IBI"),
            c("La IBI (en vivo)", "La IBI"),
            c("Ibi Town", "Otro")
        )
        val ranked = SmartSearch.rank(candidates, "La IBI")
        assertEquals("La IBI", ranked.first().candidate.title)
        // The live version and the exact artist stay; the unrelated song goes away.
        assertTrue(ranked.size >= 2)
        assertTrue(ranked.none { it.candidate.title == "Ibi Town" })
    }

    @Test fun toleratesTypos() {
        val ranked = SmartSearch.rank(listOf(c("Al Taller del Maestro", "Alex Campos")), "al taller del maestr")
        assertTrue(ranked.isNotEmpty())
        val withTypo = SmartSearch.rank(listOf(c("Tu Poeta", "Alex Campos")), "tu poetta")
        assertTrue(withTypo.isNotEmpty())
    }

    @Test fun collaborationsAreRecognisedButRankedBelowTheMainArtist() {
        val candidates = listOf(
            c("Tu Poeta", "Alex Campos"),
            c("Mi Sueño (feat. Alex Campos)", "Marcos Witt")
        )
        val ranked = SmartSearch.rank(candidates, "Alex Campos")
        assertEquals("Alex Campos", ranked.first().candidate.artist)
    }

    @Test fun nothingRelatedGivesNoResults() {
        val ranked = SmartSearch.rank(listOf(c("Bohemian Rhapsody", "Queen")), "alex campos")
        assertTrue(ranked.isEmpty())
    }

    @Test fun albumMatchesAreBelowSongs() {
        val ranked = SmartSearch.rank(listOf(c("Canción Uno", "Grupo", "Regreso"), c("Regreso", "Otro")), "Regreso")
        assertEquals("Regreso", ranked.first().candidate.title)
    }
}
