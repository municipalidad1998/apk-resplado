package com.streamvault.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The YouTube button only hands the search to the official app: no ads are touched here. */
class YouTubeLinksTest {

    @Test fun buildsAQueryFromTheSong() {
        assertEquals("Canción Artista", YouTubeLinks.query("Canción", "Artista"))
        assertEquals("Canción", YouTubeLinks.query("Canción", "Artista desconocido"))
        assertEquals("Canción", YouTubeLinks.query("Canción", "  "))
    }

    @Test fun encodesSearchUrls() {
        val terms = YouTubeLinks.query("La Bicicleta", "Carlos Vives")
        assertTrue(YouTubeLinks.searchUrl(terms).startsWith("https://www.youtube.com/results?search_query="))
        assertFalse(YouTubeLinks.searchUrl(terms).contains(" "))
        assertTrue(YouTubeLinks.musicUrl(terms).startsWith("https://music.youtube.com/search?q="))
    }
}
