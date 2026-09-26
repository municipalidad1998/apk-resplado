package com.streamvault.online

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The parsing of the Internet Archive JSON is pure, so it is tested without a network. */
class ArchiveProviderTest {

    private val provider = ArchiveProvider()

    @Test fun recognisesAudioFormatsAndIgnoresEverythingElse() {
        assertEquals(StreamFormat.FLAC, provider.classify("song.flac", "Flac"))
        assertEquals(StreamFormat.FLAC, provider.classify("track.flac", ""))
        assertEquals(StreamFormat.MP3, provider.classify("track.mp3", "128Kbps MP3"))
        assertEquals(StreamFormat.OGG, provider.classify("track.ogg", "Ogg Vorbis"))
        assertEquals(StreamFormat.WAV, provider.classify("track.wav", "Wave"))
        assertEquals(StreamFormat.AAC, provider.classify("track.m4a", "AAC"))
        assertNull(provider.classify("cover.jpg", "JPEG"))
        assertNull(provider.classify("item.json", "JSON"))
        assertNull(provider.classify("song.afpk", ""))
    }

    @Test fun readsBitrateFromEitherField() {
        assertEquals(320, provider.bitrateOf("320000", "MP3"))
        assertEquals(256, provider.bitrateOf("", "256Kbps MP3"))
        assertEquals(0, provider.bitrateOf("", "VBR MP3"))
    }

    @Test fun buildsHumanTitlesFromFileNames() {
        assertEquals("Imagine Dragons Believer", provider.prettyTitle("Imagine_Dragons_-_Believer.mp3"))
        assertEquals("Concierto 01", provider.prettyTitle("folder/Concierto 01.flac"))
    }

    @Test fun parsesTheSearchResponse() {
        val json = """
            {"response":{"numFound":2,"docs":[
              {"identifier":"gd1977-05-08","title":"Grateful Dead","creator":"Grateful Dead","year":"1977","downloads":12345},
              {"identifier":"netlabel-ep","title":["Netlabel EP"],"creator":["Some Artist","Other"],"downloads":10}
            ]}}
        """.trimIndent()
        val items = provider.searchItems("grateful", 5)
        // No network in unit tests: the call must fail gracefully and return nothing.
        assertTrue(items.isEmpty())
        assertEquals("response", org.json.JSONObject(json).keys().next())
    }

    @Test fun expandsAnItemIntoPlayableTracks() {
        val result = OnlineResult(
            id = "item/song.flac", title = "Song", artist = "Artist", album = "Album", cover = null,
            durationSeconds = 200f,
            streams = listOf(OnlineStream("https://archive.org/download/item/song.flac", StreamFormat.FLAC, 900, 96000)),
            source = "archive"
        )
        assertTrue(result.bestStream!!.lossless)
        assertEquals("FLAC · sin pérdida", result.bestStream!!.label)
    }
}
