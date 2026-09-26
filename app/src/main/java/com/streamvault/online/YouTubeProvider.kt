package com.streamvault.online

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * YouTube, using **only** what YouTube allows a third party app to do:
 *
 *  - Search with the official **YouTube Data API v3** (the user supplies their own key, the app
 *    never ships one and never scrapes the website).
 *  - Play with the **official embedded player** (IFrame API) inside the app. That player shows
 *    the ads YouTube decides to show: this app does not block, hide or skip them, and it does not
 *    play YouTube in the background, because both things need YouTube Premium.
 *
 * There is no audio stream to hand over to ExoPlayer: extracting one would mean breaking
 * YouTube's terms, so these results carry an `embedUrl` instead of a list of streams.
 */
class YouTubeProvider(private val apiKey: () -> String) : OnlineProvider {

    override val key = "youtube"
    override val label = "YouTube · reproductor oficial"
    override val canStream = false
    override val configured: Boolean get() = apiKey().isNotBlank()
    override val hint: String? get() =
        "Para buscar en YouTube falta tu clave de YouTube Data API v3 (Ajustes → Música online)."

    override suspend fun search(query: String, limit: Int): List<OnlineResult> = withContext(Dispatchers.IO) {
        val credential = apiKey().trim()
        if (credential.isBlank()) return@withContext emptyList()
        val found = runCatching { findVideos(query, limit.coerceAtMost(20), credential) }.getOrDefault(emptyList())
        if (found.isEmpty()) return@withContext emptyList()
        val seconds = runCatching { durations(found.map { it.id }, credential) }.getOrDefault(emptyMap())
        found.map { video ->
            OnlineResult(
                id = "yt-${video.id}",
                title = video.title,
                artist = video.channel.ifBlank { ResultValidator.UNKNOWN_ARTIST },
                album = ALBUM_LABEL,
                cover = video.thumbnail,
                durationSeconds = (seconds[video.id] ?: 0L).toFloat(),
                streams = emptyList(),
                source = this@YouTubeProvider.key,
                license = LICENSE_NOTE,
                embedUrl = embedUrl(video.id)
            )
        }
    }

    internal data class Video(val id: String, val title: String, val channel: String, val thumbnail: String?)

    internal suspend fun findVideos(query: String, maxResults: Int, key: String): List<Video> {
        val url = "https://www.googleapis.com/youtube/v3/search" +
            "?part=snippet&type=video&videoCategoryId=$MUSIC_CATEGORY" +
            "&maxResults=$maxResults&q=" + URLEncoder.encode(query, "UTF-8") + "&key=" + URLEncoder.encode(key, "UTF-8")
        val body = get(url) ?: return emptyList()
        return parseSearch(body)
    }

    /** Pure parsing of the search response: testable without network or key. */
    internal fun parseSearch(body: String): List<Video> {
        val items = JSONObject(body).optJSONArray("items") ?: return emptyList()
        val list = mutableListOf<Video>()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val id = item.optJSONObject("id")?.optString("videoId").orEmpty()
            if (id.isBlank()) continue
            val snippet = item.optJSONObject("snippet")
            val thumbnails = snippet?.optJSONObject("thumbnails")
            val thumbnail = listOf("medium", "high", "standard", "default")
                .firstNotNullOfOrNull { thumbnails?.optJSONObject(it)?.optString("url")?.takeIf { url -> url.startsWith("https://") } }
            list += Video(
                id = id,
                title = snippet?.optString("title").orEmpty().replace(Regex("\\s+"), " ").trim(),
                channel = snippet?.optString("channelTitle").orEmpty(),
                thumbnail = thumbnail
            )
        }
        return list
    }

    /** Second official call: the search response does not carry the length of each video. */
    internal suspend fun durations(ids: List<String>, key: String): Map<String, Long> {
        if (ids.isEmpty()) return emptyMap()
        val url = "https://www.googleapis.com/youtube/v3/videos?part=contentDetails" +
            "&id=" + URLEncoder.encode(ids.joinToString(","), "UTF-8") + "&key=" + URLEncoder.encode(key, "UTF-8")
        val body = get(url) ?: return emptyMap()
        return parseDurations(body)
    }

    internal fun parseDurations(body: String): Map<String, Long> {
        val items = JSONObject(body).optJSONArray("items") ?: return emptyMap()
        val map = mutableMapOf<String, Long>()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val id = item.optString("id")
            val duration = item.optJSONObject("contentDetails")?.optString("duration").orEmpty()
            if (id.isNotBlank()) map[id] = isoDuration(duration)
        }
        return map
    }

    /** ISO 8601 length used by YouTube: PT3M25S, PT1H2M3S, P1DT2H. */
    internal fun isoDuration(value: String): Long {
        val match = Regex("^P(?:(\\d+)D)?T?(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+(?:\\.\\d+)?)S)?$").find(value) ?: return 0L
        val (days, hours, minutes, seconds) = match.destructured
        val secs = seconds.toDoubleOrNull()?.toLong() ?: 0L
        return (days.toLongOrNull() ?: 0L) * 86_400 +
            (hours.toLongOrNull() ?: 0L) * 3_600 +
            (minutes.toLongOrNull() ?: 0L) * 60 +
            secs
    }

    /** The official embed player. No ad blocking, no background audio, no stream extraction. */
    internal fun embedUrl(videoId: String): String =
        "https://www.youtube.com/embed/$videoId?autoplay=1&rel=0&modestbranding=1&playsinline=1"

    private fun get(url: String): String? = runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 12_000
            connection.readTimeout = 15_000
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    companion object {
        const val MUSIC_CATEGORY = 10
        const val ALBUM_LABEL = "YouTube"
        const val LICENSE_NOTE = "Reproductor oficial de YouTube"
        fun watchUrl(videoId: String) = "https://www.youtube.com/watch?v=$videoId"
        fun musicUrl(videoId: String) = "https://music.youtube.com/watch?v=$videoId"
    }
}
