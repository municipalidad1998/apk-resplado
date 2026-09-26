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
        assertEquals("Imagine Dragons - Believer", provider.prettyTitle("Imagine_Dragons_-_Believer.mp3"))
        assertEquals("Concierto 01", provider.prettyTitle("folder/Concierto 01.flac"))
    }

    @Test fun parsesTheSearchResponse() {
        val json = """
            {"response":{"numFound":2,"docs":[
              {"identifier":"gd1977-05-08","title":"Grateful Dead","creator":"Grateful Dead","year":"1977","downloads":12345},
              {"identifier":"netlabel-ep","title":["Netlabel EP"],"creator":["Some Artist","Other"],"downloads":10}
            ]}}
        """.trimIndent()
        val items = provider.parseSearch(json)
        assertEquals(2, items.size)
        // Most downloaded first, and arrays are unwrapped to their first value.
        assertEquals("gd1977-05-08", items[0].identifier)
        assertEquals("Grateful Dead", items[0].creator)
        assertEquals("Some Artist", items[1].creator)
        assertEquals("Netlabel EP", items[1].title)
    }

    @Test fun parsesAnItemIntoStreams() {
        val json = """
            {"metadata":{"licenseurl":"https://creativecommons.org/licenses/by/4.0/"},"files":[
              {"name":"cover.jpg","format":"JPEG","size":"1000"},
              {"name":"track1.flac","format":"Flac","size":"30000000","length":"254.5","bitrate":"4608000"},
              {"name":"track1.mp3","format":"128Kbps MP3","size":"4000000","length":"254.5","bitrate":"128000"},
              {"name":"tiny.mp3","format":"VBR MP3","length":"4"}
            ]}
        """.trimIndent()
        val item = ArchiveProvider.ArchiveItem("item", "Álbum", "Autor", "2020", "Rock", 5)
        val results = provider.parseItem(item, json)
        assertEquals(2, results.size) // the artwork and the 4 second file are dropped
        assertEquals("track1", results[0].title)
        assertEquals("Autor", results[0].artist)
        assertEquals("Álbum", results[0].album)
        assertEquals(254.5f, results[0].durationSeconds, 0.01f)
        assertEquals(StreamFormat.FLAC, results[0].streams.first().format)
        assertTrue(results[0].streams.first().url.startsWith("https://archive.org/download/item/"))
        assertTrue(results[0].license.contains("creativecommons"))
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
