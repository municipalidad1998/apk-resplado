package com.streamvault.online

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Provider backed by the Internet Archive: public JSON endpoints, no API key, no account, and the
 * files are hosted with licences that allow redistribution (Live Music Archive, netlabels, public
 * domain). It is a real catalogue, but it is NOT the commercial catalogue: major-label hits are
 * not there, and no legal free API can offer them.
 */
class ArchiveProvider : OnlineProvider {

    override val key = "archive"
    override val label = "Internet Archive · música libre"
    override val canStream = true

    override suspend fun search(query: String, limit: Int): List<OnlineResult> = withContext(Dispatchers.IO) {
        val items = runCatching { searchItems(query, limit.coerceAtMost(24)) }.getOrDefault(emptyList())
        if (items.isEmpty()) return@withContext emptyList()
        coroutineScope {
            items.take(8).map { item ->
                async(Dispatchers.IO) { runCatching { expand(item) }.getOrDefault(emptyList()) }
            }.awaitAll().flatten().take(limit)
        }
    }

    internal fun searchItems(query: String, rows: Int): List<ArchiveItem> {
        val q = URLEncoder.encode("$query AND mediatype:audio", "UTF-8")
        val url = "https://archive.org/advancedsearch.php?q=$q" +
            "&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=creator&fl%5B%5D=year&fl%5B%5D=downloads&fl%5B%5D=subject" +
            "&rows=$rows&page=1&output=json"
        val body = get(url) ?: return emptyList()
        val docs = JSONObject(body).optJSONObject("response")?.optJSONArray("docs") ?: return emptyList()
        val list = mutableListOf<ArchiveItem>()
        for (index in 0 until docs.length()) {
            val doc = docs.optJSONObject(index) ?: continue
            val identifier = doc.optString("identifier")
            if (identifier.isBlank()) continue
            list += ArchiveItem(
                identifier = identifier,
                title = doc.optString("title").ifBlank { identifier },
                creator = firstCreator(doc.opt("creator")),
                year = doc.optString("year"),
                subject = firstCreator(doc.opt("subject")),
                downloads = runCatching { doc.optInt("downloads") }.getOrDefault(0)
            )
        }
        return list.sortedByDescending { it.downloads }
    }

    internal fun expand(item: ArchiveItem): List<OnlineResult> {
        val body = get("https://archive.org/metadata/${item.identifier}") ?: return emptyList()
        val root = JSONObject(body)
        val metadata = root.optJSONObject("metadata")
        val license = metadata?.optString("licenseurl").orEmpty()
        val files = root.optJSONArray("files") ?: return emptyList()
        val results = mutableListOf<OnlineResult>()
        for (index in 0 until files.length()) {
            val file = files.optJSONObject(index) ?: continue
            val name = file.optString("name")
            if (name.isBlank()) continue
            val format = classify(name, file.optString("format"))
            if (format == null) continue
            val length = file.optString("length").toFloatOrNull() ?: 0f
            if (length < 20f) continue
            results += OnlineResult(
                id = "${item.identifier}/$name",
                title = prettyTitle(name),
                artist = item.creator.ifBlank { "Artista desconocido" },
                album = item.title,
                cover = "https://archive.org/services/img/${item.identifier}",
                durationSeconds = length,
                streams = listOf(
                    OnlineStream(
                        url = "https://archive.org/download/${item.identifier}/" + URLEncoder.encode(name, "UTF-8").replace("+", "%20"),
                        format = format,
                        bitrateKbps = bitrateOf(file.optString("bitrate"), file.optString("format")),
                        sizeBytes = file.optString("size").toLongOrNull() ?: 0L,
                        durationSeconds = length
                    )
                ),
                source = key,
                license = license
            )
        }
        return results
    }

    /** Only real audio, skipping artwork, playlists, checksums and derivatives of derivatives. */
    internal fun classify(name: String, format: String): StreamFormat? {
        val lower = name.lowercase()
        if (lower.endsWith(".afpk") || lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".gif")) return null
        return when {
            lower.endsWith(".flac") || format.contains("flac", true) -> StreamFormat.FLAC
            lower.endsWith(".wav") || format.contains("wave", true) -> StreamFormat.WAV
            lower.endsWith(".m4a") || lower.endsWith(".alac") || format.contains("alac", true) -> StreamFormat.ALAC
            lower.endsWith(".ogg") || lower.endsWith(".oga") || format.contains("vorbis", true) || format.contains("ogg", true) -> StreamFormat.OGG
            lower.endsWith(".opus") -> StreamFormat.OGG
            lower.endsWith(".mp3") || format.contains("mp3", true) -> StreamFormat.MP3
            lower.endsWith(".aac") || lower.endsWith(".mp4") || lower.endsWith(".m4b") -> StreamFormat.AAC
            else -> null
        }
    }

    internal fun bitrateOf(bitrate: String, format: String): Int {
        bitrate.toIntOrNull()?.let { if (it > 0) return it / 1000 }
        val parsed = Regex("(\\d{2,4})\\s*kbps", RegexOption.IGNORE_CASE).find(format)?.groupValues?.get(1)?.toIntOrNull()
        return parsed ?: 0
    }

    internal fun prettyTitle(name: String): String = name.substringAfterLast('/').substringBeforeLast('.')
        .replace('_', ' ').replace(Regex("\\s+"), " ").trim().ifBlank { name }

    private fun firstCreator(value: Any?): String = when (value) {
        is String -> value
        is org.json.JSONArray -> (0 until value.length()).mapNotNull { value.optString(it) }.firstOrNull().orEmpty()
        else -> ""
    }

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

    data class ArchiveItem(val identifier: String, val title: String, val creator: String, val year: String, val subject: String, val downloads: Int)
}
