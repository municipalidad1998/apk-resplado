package com.streamvault.online

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Online results are validated before they are shown: no title or no stream means no result. */
class ResultValidatorTest {

    private fun result(title: String, artist: String = "", url: String = "https://archive.org/download/x/a.mp3",
                       album: String = "", duration: Float = 180f, source: String = "archive") =
        OnlineResult("id-$title", title, artist, album, null, duration,
            listOf(OnlineStream(url, StreamFormat.MP3, 192, 44100, 1024, duration)), source, "")

    @Test fun dropsResultsWithoutAStream() {
        val broken = result("Sin audio").copy(streams = emptyList())
        assertTrue(ResultValidator.clean(listOf(broken)).isEmpty())
    }

    @Test fun dropsNonHttpUrls() {
        val broken = result("Raro", url = "file:///sdcard/a.mp3")
        assertTrue(ResultValidator.clean(listOf(broken)).isEmpty())
    }

    @Test fun dropsEmptyTitles() {
        assertTrue(ResultValidator.clean(listOf(result(" "), result("a"))).isEmpty())
    }

    @Test fun fillsTheArtistAndTheAlbum() {
        val cleaned = ResultValidator.clean(listOf(result("Canción")))
        assertEquals(1, cleaned.size)
        assertEquals(ResultValidator.UNKNOWN_ARTIST, cleaned.first().artist)
        assertEquals(ResultValidator.UNKNOWN_ALBUM, cleaned.first().album)
    }

    @Test fun everyResultCarriesIdTitleArtistAlbumDurationSource() {
        val cleaned = ResultValidator.clean(listOf(result("Canción", "Autor", album = "Disco")))
        val first = cleaned.first()
        assertTrue(first.id.isNotBlank())
        assertTrue(first.title.isNotBlank())
        assertTrue(first.artist.isNotBlank())
        assertTrue(first.album.isNotBlank())
        assertTrue(first.durationSeconds > 0f)
        assertTrue(first.source.isNotBlank())
        assertTrue(first.streams.isNotEmpty())
    }

    @Test fun repeatedResultsAreOnlyShownOnce() {
        val one = result("Canción")
        assertEquals(1, ResultValidator.clean(listOf(one, one.copy())).size)
    }
}
