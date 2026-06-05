package com.streamvault.util
import com.streamvault.data.model.Channel
import java.io.BufferedReader
import java.io.StringReader
import java.util.UUID
object M3UParser {
    fun parse(content: String, sourceId: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val reader = BufferedReader(StringReader(content))
        var line: String?
        var currentMeta: Map<String, String>? = null
        while (reader.readLine().also { line = it } != null) {
            val l = line!!.trim()
            when {
                l.startsWith("#EXTINF:") -> currentMeta = parseExtInf(l)
                l.isNotEmpty() && !l.startsWith("#") && currentMeta != null -> {
                    channels.add(Channel(
                        id = UUID.randomUUID().toString(),
                        name = currentMeta!!["name"] ?: "Canal",
                        url = l,
                        logo = currentMeta!!["tvg-logo"],
                        group = currentMeta!!["group-title"],
                        sourceId = sourceId,
                        type = detectType(currentMeta!!["group-title"])
                    ))
                    currentMeta = null
                }
            }
        }
        return channels
    }
    private fun parseExtInf(line: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        Regex(""",(.+)$""").find(line)?.let { result["name"] = it.groupValues[1].trim() }
        Regex("""([\w-]+)="([^"]*)"""").findAll(line).forEach { result[it.groupValues[1]] = it.groupValues[2] }
        return result
    }
    private fun detectType(group: String?): String {
        val g = group?.lowercase() ?: ""
        return when {
            g.contains("movie") || g.contains("pelicula") || g.contains("film") -> "MOVIE"
            g.contains("serie") || g.contains("show") -> "SERIES"
            else -> "LIVE"
        }
    }
}
