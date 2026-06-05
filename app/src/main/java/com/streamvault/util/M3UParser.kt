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
        var currentMeta = mutableMapOf<String, String>()

        while (reader.readLine().also { line = it } != null) {
            val l = line!!.trim()
            when {
                l.startsWith("#EXTINF:") -> currentMeta = parseExtInf(l)
                l.isNotEmpty() && !l.startsWith("#") && currentMeta.isNotEmpty() -> {
                    val name  = currentMeta["name"] ?: "Canal"
                    val group = currentMeta["group-title"]?.trim() ?: ""
                    val logo  = currentMeta["tvg-logo"]
                    val type  = detectType(group, name, l)

                    // SOLO agregar canales EN VIVO aquí
                    // VOD/series se ignoran en la lista M3U (son para Xtream)
                    channels.add(Channel(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        url = l,
                        logo = logo,
                        group = group.ifEmpty { "GENERAL" },
                        sourceId = sourceId,
                        type = type
                    ))
                    currentMeta = mutableMapOf()
                }
            }
        }
        return channels
    }

    private fun parseExtInf(line: String): MutableMap<String, String> {
        val result = mutableMapOf<String, String>()
        // Nombre del canal (último elemento después de la coma)
        val commaIdx = line.lastIndexOf(',')
        if (commaIdx >= 0 && commaIdx < line.length - 1) {
            result["name"] = line.substring(commaIdx + 1).trim()
        }
        // Atributos key="value"
        Regex("""([\w-]+)="([^"]*)"""").findAll(line).forEach {
            result[it.groupValues[1]] = it.groupValues[2]
        }
        return result
    }

    // Solo detecta tipo para mostrar badge - TODOS van a LIVE en M3U
    private fun detectType(group: String, name: String, url: String): String {
        val g = group.lowercase()
        val n = name.lowercase()
        return when {
            g.contains("vod") || g.contains("movie") || g.contains("pelicula") ||
            g.contains("film") || n.contains("[movie]") -> "MOVIE"
            g.contains("serie") || g.contains("series") || g.contains("show") ||
            n.contains("[series]") -> "SERIES"
            else -> "LIVE"
        }
    }
}
