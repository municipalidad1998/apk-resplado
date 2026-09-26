package com.streamvault.scanner

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.streamvault.LuminaApp
import com.streamvault.data.AudioLocation
import com.streamvault.data.Track
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class AudioScanner(private val app: LuminaApp, private val progress: suspend (Int, String) -> Unit) {
    private val dao = app.library
    private val resolver = app.contentResolver
    private val generation = UUID.randomUUID().toString()
    private var count = 0
    private val warnings = mutableListOf<String>()
    private val excluded = app.preferences.state.value.excludedFolders.lines().map { it.trim() }.filter { it.isNotEmpty() }
    suspend fun scan(): List<String> = app.scanMutex.withLock { scanInternal() }
    private suspend fun scanInternal(): List<String> {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(app, permission) == PackageManager.PERMISSION_GRANTED) {
            val volumes = if (Build.VERSION.SDK_INT >= 29) MediaStore.getExternalVolumeNames(app) else setOf("external")
            for (volume in volumes) {
                val root = "media:$volume"
                try { mediaStore(volume, root); dao.prune(root, generation) }
                catch (e: Exception) { currentCoroutineContext().ensureActive(); warnings += "No se pudo leer $volume: ${e.localizedMessage}" }
            }
        }
        for (tree in app.preferences.roots()) {
            try {
                val uri = Uri.parse(tree)
                walk(uri, DocumentsContract.getTreeDocumentId(uri), tree, "")
                dao.prune(tree, generation)
            } catch (e: Exception) { currentCoroutineContext().ensureActive(); warnings += "Carpeta sin acceso. Vuelve a seleccionarla: $tree" }
        }
        dao.reconcile()
        progress(count, if (warnings.isEmpty()) "Biblioteca actualizada" else warnings.joinToString("\n"))
        return warnings
    }
    private suspend fun mediaStore(volume: String, root: String) {
        val collection = if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.getContentUri(volume) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val pathColumn = if (Build.VERSION.SDK_INT >= 29) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA
        val projection = arrayOf("_id", "_display_name", "_size", "date_modified", "duration", "title", "artist", "album", pathColumn)
        val cursor = resolver.query(collection, projection, null, null, "_id ASC") ?: error("Proveedor multimedia no disponible")
        cursor.use { c ->
            while (c.moveToNext()) {
                currentCoroutineContext().ensureActive()
                val path = c.getString(8).orEmpty().let { if (Build.VERSION.SDK_INT < 29) it.substringBeforeLast('/') else it }
                if (isExcluded(path)) continue
                ingest(ContentUris.withAppendedId(collection, c.getLong(0)), root, c.getString(1).orEmpty(), path,
                    c.getLong(2), c.getLong(3), c.getLong(4), c.getString(5), c.getString(6), c.getString(7))
            }
        }
    }
    private suspend fun walk(tree: Uri, parent: String, root: String, path: String) {
        currentCoroutineContext().ensureActive()
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent)
        val columns = arrayOf("document_id", "_display_name", "mime_type", "_size", "last_modified")
        val rows = mutableListOf<Array<String>>()
        (resolver.query(children, columns, null, null, null) ?: error("No se pudo consultar carpeta")).use { c ->
            while (c.moveToNext()) rows += arrayOf(c.getString(0), c.getString(1).orEmpty(), c.getString(2).orEmpty(), c.getLong(3).toString(), c.getLong(4).toString())
        }
        val localCover = rows.firstOrNull { it[1].lowercase() in setOf("cover.jpg", "folder.jpg", "album.jpg", "cover.png", "folder.png") }
            ?.let { DocumentsContract.buildDocumentUriUsingTree(tree, it[0]) }
        for (row in rows) {
            val (id, name, mime, size, modified) = row
            if (isExcluded("$path/$name")) continue
            if (mime == DocumentsContract.Document.MIME_TYPE_DIR) walk(tree, id, root, "$path/$name")
            else if (mime.startsWith("audio/") || name.substringAfterLast('.', "").lowercase() in EXTENSIONS) {
                ingest(DocumentsContract.buildDocumentUriUsingTree(tree, id), root, name, path.ifBlank { DocumentsContract.getTreeDocumentId(tree) },
                    size.toLong(), modified.toLong() / 1000, localCover = localCover)
            }
        }
    }
    private fun isExcluded(path: String) = excluded.any { path.contains(it, ignoreCase = true) }
    private suspend fun ingest(uri: Uri, root: String, name: String, folder: String, size: Long, modified: Long,
                               duration: Long = 0, title: String? = null, artist: String? = null, album: String? = null, localCover: Uri? = null) {
        val old = dao.location(uri.toString())
        if (old != null && old.size == size && old.modified == modified && modified > 0) {
            dao.seen(uri.toString(), generation)
            count++; if (count % 25 == 0) progress(count, "Escaneando biblioteca…")
            return
        }
        try {
            val hash = MessageDigest.getInstance("SHA-256")
            (resolver.openInputStream(uri) ?: error("No se puede abrir el audio")).use { input ->
                val buffer = ByteArray(128 * 1024)
                while (true) { currentCoroutineContext().ensureActive(); val n = input.read(buffer); if (n < 0) break; hash.update(buffer, 0, n) }
            }
            val id = hash.digest().joinToString("") { "%02x".format(it) }
            if (dao.track(id) == null) {
                var track = Track(id, uri.toString(), name, title?.takeUnless { it.isBlank() } ?: name.substringBeforeLast('.'),
                    artist = clean(artist, "Artista desconocido"), album = clean(album, "Sin álbum"), folder = folder,
                    durationMs = duration, size = size, date = dateFromName(name) ?: modified * 1000,
                    source = classify(name, folder))
                val metadata = MediaMetadataRetriever()
                try {
                    metadata.setDataSource(app, uri)
                    track = track.copy(
                        title = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() } ?: track.title,
                        artist = clean(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST), track.artist),
                        album = clean(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM), track.album),
                        genre = clean(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE), "Sin género"),
                        durationMs = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: duration,
                        trackNumber = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.substringBefore('/')?.toIntOrNull() ?: 0,
                        discNumber = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)?.substringBefore('/')?.toIntOrNull() ?: 0,
                        cover = app.artwork.embedded(id, metadata.embeddedPicture) ?: localCover?.let { app.artwork.import(id, it) })
                } catch (_: Exception) { /* Unsupported decoder: still index the original, never discard it. */ }
                finally { metadata.release() }
                dao.insert(track)
            }
            dao.location(AudioLocation(uri.toString(), id, root, size, modified, generation))
            count++; if (count % 5 == 0) progress(count, "Escaneando biblioteca…")
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            if (old != null) dao.seen(uri.toString(), generation)
            if (warnings.size < 20) warnings += "$name: ${e.localizedMessage ?: "No se pudo leer"}"
        }
    }
    companion object {
        val EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "wma", "amr", "aiff", "3gp", "mp4", "mka", "mid", "midi")
        fun clean(value: String?, fallback: String) = value?.takeUnless { it.isBlank() || it == "<unknown>" } ?: fallback
        fun classify(name: String, folder: String): String = when {
            folder.contains("WhatsApp", true) || Regex("^(AUD|PTT)-\\d{8}-WA", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "whatsapp"
            folder.contains("record", true) || folder.contains("grabacion", true) || folder.contains("grabación", true) -> "recording"
            else -> "music"
        }
        fun dateFromName(name: String): Long? = runCatching {
            val date = Regex("(?:AUD|PTT)-(\\d{4})(\\d{2})(\\d{2})-WA", RegexOption.IGNORE_CASE).find(name) ?: return null
            LocalDate.of(date.groupValues[1].toInt(), date.groupValues[2].toInt(), date.groupValues[3].toInt()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull()
    }
}
