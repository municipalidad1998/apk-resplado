package com.streamvault.online

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The YouTube provider uses the official Data API v3 and the official embedded player, so its
 * parsing is tested here without any network or key.
 */
class YouTubeProviderTest {

    private val searchBody = """
        {"items":[
          {"id":{"kind":"youtube#video","videoId":"abc123"},
           "snippet":{"title":"Alex Campos - Tu Poeta","channelTitle":"Alex Campos",
             "thumbnails":{"default":{"url":"https://i.ytimg.com/vi/abc123/default.jpg"},
                           "medium":{"url":"https://i.ytimg.com/vi/abc123/mqdefault.jpg"}}}},
          {"id":{"videoId":"def456"},
           "snippet":{"title":"Otro tema","channelTitle":"Otro Canal","thumbnails":{}}},
          {"id":{"videoId":""},"snippet":{"title":"Sin id"}}
        ]}
    """.trimIndent()

    private val durationsBody = """
        {"items":[
          {"id":"abc123","contentDetails":{"duration":"PT3M25S"}},
          {"id":"def456","contentDetails":{"duration":"PT1H2M3S"}}
        ]}
    """.trimIndent()

    private fun provider(key: String = "") = YouTubeProvider { key }

    @Test fun parsesSearchResultsWithThumbnailAndChannel() {
        val videos = provider().parseSearch(searchBody)
        assertEquals(2, videos.size)
        assertEquals("abc123", videos.first().id)
        assertEquals("Alex Campos - Tu Poeta", videos.first().title)
        assertEquals("Alex Campos", videos.first().channel)
        assertEquals("https://i.ytimg.com/vi/abc123/mqdefault.jpg", videos.first().thumbnail)
        // A video without thumbnails is still a valid result.
        assertEquals(null, videos.last().thumbnail)
    }

    @Test fun parsesIso8601Durations() {
        val seconds = provider().parseDurations(durationsBody)
        assertEquals(205L, seconds["abc123"])
        assertEquals(3723L, seconds["def456"])
        assertEquals(45L, provider().isoDuration("PT45S"))
        assertEquals(0L, provider().isoDuration(""))
        assertEquals(90_000L, provider().isoDuration("P1DT1H"))
    }

    @Test fun embedUrlsUseHttpsAndTheOfficialPlayer() {
        val url = provider().embedUrl("abc123")
        assertTrue(url.startsWith("https://www.youtube.com/embed/abc123"))
        assertTrue(url.contains("playsinline=1"))
    }

    @Test fun withoutAKeyTheProviderIsSilentButExplainsWhy() {
        val empty = provider("")
        assertFalse(empty.configured)
        assertTrue(empty.hint!!.contains("YouTube"))
        val withKey = provider("AIza-test")
        assertTrue(withKey.configured)
        assertEquals(null, withKey.hint)
    }

    @Test fun itNeverClaimsToStreamAudio() {
        // No stream is extracted from YouTube: the official player is shown instead.
        assertFalse(provider("AIza-test").canStream)
    }

    @Test fun embedOnlyResultsPassValidation() {
        val result = OnlineResult("yt-abc", "Tu Poeta", "Alex Campos", "YouTube", null, 205f,
            emptyList(), "youtube", "", embedUrl = "https://www.youtube.com/embed/abc?autoplay=1")
        val cleaned = ResultValidator.clean(listOf(result))
        assertEquals(1, cleaned.size)
        assertTrue(cleaned.first().playable)
    }

    @Test fun aResultWithoutStreamAndWithoutEmbedIsDropped() {
        val result = OnlineResult("yt-abc", "Tu Poeta", "Alex Campos", "YouTube", null, 205f,
            emptyList(), "youtube", "")
        assertTrue(ResultValidator.clean(listOf(result)).isEmpty())
    }
}
