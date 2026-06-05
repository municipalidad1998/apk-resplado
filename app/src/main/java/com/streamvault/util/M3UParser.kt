package com.streamvault.util

import com.streamvault.data.model.Channel
import com.streamvault.data.model.Movie
import com.streamvault.data.model.Series
import java.io.BufferedReader
import java.io.StringReader
import java.util.UUID

data class ParsedContent(
    val channels: List<Channel>,
    val movies: List<Movie>,
    val series: List<Series>
)

object M3UParser {

    // Palabras clave que indican PELÍCULA en group-title
    private val MOVIE_KEYWORDS = listOf(
        "movie", "pelicula", "película", "peliculas", "películas",
        "film", "films", "cine", "vod", "movies", "estrenos",
        "estreno", "accion", "comedia", "drama", "terror", "horror",
        "thriller", "animacion", "animación", "documental", "documentales"
    )

    // Palabras clave que indican SERIE
    private val SERIES_KEYWORDS = listOf(
        "serie", "series", "show", "shows", "novela", "novelas",
        "telenovela", "temporada", "season", "episodio", "episode",
        "anime", "cartoon", "caricatura", "caricaturas"
    )

    // Palabras clave que indican canal EN VIVO
    private val LIVE_KEYWORDS = listOf(
        "live", "en vivo", "canal", "canales", "tv", "news",
        "noticias", "deportes", "sport", "music", "musica",
        "infantil", "kids", "entertainment"
    )

    fun parseAll(content: String, sourceId: String): ParsedContent {
        val channels = mutableListOf<Channel>()
        val movies   = mutableListOf<Movie>()
        val series   = mutableListOf<Series>()

        val reader = BufferedReader(StringReader(content))
        var line: String?
        var meta = mutableMapOf<String, String>()

        while (reader.readLine().also { line = it } != null) {
            val l = line!!.trim()
            when {
                l.startsWith("#EXTINF:") -> meta = parseExtInf(l)
                l.isNotEmpty() && !l.startsWith("#") && meta.isNotEmpty() -> {
                    val name  = meta["name"]?.trim() ?: "Sin nombre"
                    val group = meta["group-title"]?.trim() ?: ""
                    val logo  = meta["tvg-logo"]?.trim()
                    val type  = detectType(group, name, l)
                    val id    = UUID.randomUUID().toString()

                    when (type) {
                        "MOVIE" -> movies.add(Movie(
                            id = id, name = name, cover = logo,
                            streamUrl = l, sourceId = sourceId,
                            genre = group
                        ))
                        "SERIES" -> series.add(Series(
                            id = id, name = name, cover = logo,
                            sourceId = sourceId, genre = group
                        ))
                        else -> channels.add(Channel(
                            id = id, name = name, url = l,
                            logo = logo,
                            group = group.ifEmpty { "GENERAL" },
                            sourceId = sourceId, type = "LIVE"
                        ))
                    }
                    meta = mutableMapOf()
                }
            }
        }
        return ParsedContent(channels, movies, series)
    }

    // Mantener también parse() para compatibilidad
    fun parse(content: String, sourceId: String): List<Channel> =
        parseAll(content, sourceId).channels

    private fun parseExtInf(line: String): MutableMap<String, String> {
        val result = mutableMapOf<String, String>()
        val commaIdx = line.lastIndexOf(',')
        if (commaIdx >= 0 && commaIdx < line.length - 1) {
            result["name"] = line.substring(commaIdx + 1).trim()
        }
        Regex("""([\w-]+)="([^"]*)"""").findAll(line).forEach {
            result[it.groupValues[1]] = it.groupValues[2]
        }
        return result
    }

    fun detectType(group: String, name: String, url: String): String {
        val g = group.lowercase().trim()
        val n = name.lowercase().trim()
        val u = url.lowercase()

        // Chequeo por URL primero (más confiable)
        when {
            u.contains("/movie/") -> return "MOVIE"
            u.contains("/series/") || u.contains("/serie/") -> return "SERIES"
        }

        // Chequeo exacto del group-title
        if (g.isNotEmpty()) {
            // Serie primero (más específico)
            if (SERIES_KEYWORDS.any { g == it || g.startsWith("$it ") || g.startsWith("$it|") ||
                    g.contains(" $it") || g.contains("|$it") }) return "SERIES"
            // Película
            if (MOVIE_KEYWORDS.any { g == it || g.startsWith("$it ") || g.startsWith("$it|") ||
                    g.contains(" $it") || g.contains("|$it") }) return "MOVIE"
        }

        // Chequeo por nombre
        if (n.contains("(") && n.contains(")") && Regex("""\(\d{4}\)""").containsMatchIn(n))
            return "MOVIE" // tiene año entre paréntesis = película

        return "LIVE"
    }
}
