package com.streamvault.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

/**
 * Proveedor EXCLUSIVO de música local. Usa únicamente MediaStore del
 * dispositivo. Jamás consulta Internet, YouTube ni APIs musicales.
 */
class LocalMusicProvider(private val context: Context) {

    private val supportedExt = listOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus")

    suspend fun scanAll(): List<LocalSong> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<LocalSong>()
        val collection: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Audio.Media.ARTIST} ASC, ${MediaStore.Audio.Media.ALBUM} ASC, ${MediaStore.Audio.Media.TRACK} ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (c.moveToNext()) {
                val path = c.getString(dataCol) ?: continue
                val ext = path.substringAfterLast('.', "").lowercase()
                if (ext !in supportedExt) continue
                val id = c.getLong(idCol)
                val albumId = c.getLong(albumIdCol)
                val rawTrack = c.getInt(trackCol)
                // MediaStore codifica track como DDDD + TTTT (disco + pista)
                val disc = if (rawTrack >= 1000) rawTrack / 1000 else null
                val track = if (rawTrack >= 1000) rawTrack % 1000 else rawTrack.takeIf { it > 0 }
                val art: String? = if (albumId > 0)
                    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
                else null
                songs.add(
                    LocalSong(
                        id = id,
                        title = c.getString(titleCol) ?: "Desconocido",
                        artist = c.getString(artistCol)?.takeIf { it != "<unknown>" } ?: "Artista desconocido",
                        album = c.getString(albumCol) ?: "Álbum desconocido",
                        duration = c.getLong(durCol),
                        year = c.getInt(yearCol).takeIf { it > 0 },
                        track = track,
                        disc = disc,
                        contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString(),
                        albumArtUri = art
                    )
                )
            }
        }
        songs
    }

    companion object {
        /** Normaliza: minúsculas, sin acentos, espacios compactados. */
        fun norm(s: String): String {
            val n = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            return n.replace("\\p{M}".toRegex(), "").replace("\\s+".toRegex(), " ").trim()
        }

        /**
         * Búsqueda 100% LOCAL: exacta, por artista/título/álbum, parcial,
         * insensible a mayúsculas y acentos, con tolerancia básica a errores
         * (distancia de edición <= 2 por token). Nunca devuelve resultados
         * aleatorios ni contenido online.
         */
        fun search(songs: List<LocalSong>, query: String): List<LocalSong> {
            val q = norm(query)
            if (q.length < 2) return emptyList()
            val qTokens = q.split(" ")
            return songs.filter { s ->
                val hayTitle = norm(s.title)
                val hayArtist = norm(s.artist)
                val hayAlbum = norm(s.album)
                // Coincidencia exacta o parcial por subcadena
                val direct = hayTitle.contains(q) || hayArtist.contains(q) || hayAlbum.contains(q) ||
                        "$hayArtist $hayTitle".contains(q) || "$hayTitle $hayArtist".contains(q)
                if (direct) return@filter true
                // Coincidencia por tokens con fuzzy básico (<=2 ediciones)
                val hayTokens = (hayTitle + " " + hayArtist + " " + hayAlbum).split(" ")
                qTokens.all { qt ->
                    hayTokens.any { ht -> ht.startsWith(qt) || levenshtein(qt, ht) <= if (qt.length > 5) 2 else 1 }
                }
            }
        }

        fun levenshtein(a: String, b: String): Int {
            if (a == b) return 0
            if (a.isEmpty()) return b.length
            if (b.isEmpty()) return a.length
            var prev = IntArray(b.length + 1) { it }
            for (i in 1..a.length) {
                val cur = IntArray(b.length + 1)
                cur[0] = i
                for (j in 1..b.length) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
                }
                prev = cur
            }
            return prev[b.length]
        }
    }
}
