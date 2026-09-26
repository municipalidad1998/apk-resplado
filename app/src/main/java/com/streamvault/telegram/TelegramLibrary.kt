package com.streamvault.telegram

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

/**
 * Registry of music that came from Telegram: message id, file id and hash, so a song stays in the
 * library with its metadata even when the file is no longer on the device.
 *
 * Streaming it back from Telegram itself needs an MTProto client and the user's own API
 * credentials; this table keeps every field ready for that integration and meanwhile keeps the
 * metadata and the link to the local copy.
 */
@Entity(tableName = "telegram_files")
data class TelegramFile(
    @PrimaryKey val messageId: Long,
    val fileId: String = "",
    val name: String,
    val artist: String = "Artista desconocido",
    val album: String = "Sin álbum",
    val durationMs: Long = 0,
    val size: Long = 0,
    val hash: String = "",
    val trackId: String? = null,
    val date: Long = System.currentTimeMillis()
)

@Dao
interface TelegramDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(files: List<TelegramFile>)
    @Query("SELECT * FROM telegram_files ORDER BY date DESC") fun all(): Flow<List<TelegramFile>>
    @Query("SELECT COUNT(*) FROM telegram_files") fun count(): Flow<Int>
    @Query("SELECT * FROM telegram_files WHERE name = :name AND size = :size LIMIT 1") suspend fun find(name: String, size: Long): TelegramFile?
    @Query("UPDATE telegram_files SET trackId = :trackId WHERE messageId = :messageId") suspend fun link(messageId: Long, trackId: String?)
}

/**
 * Reads the official Telegram Desktop export ("result.json"): every audio message becomes an
 * entry with its ids, name, performer, title, duration and size.
 */
object TelegramImport {

    data class Summary(val saved: Int, val linked: Int, val total: Int)

    fun parse(json: String): List<TelegramFile> {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyList()
        val messages = root.optJSONArray("messages") ?: return emptyList()
        val files = mutableListOf<TelegramFile>()
        for (index in 0 until messages.length()) {
            val message = messages.optJSONObject(index) ?: continue
            if (message.optString("type") != "message") continue
            val file = message.optString("file")
            if (file.isBlank()) continue
            val name = file.substringAfterLast('/')
            if (name.isBlank()) continue
            val isAudio = message.optString("media_type") == "audio_file" ||
                message.optString("mime_type").startsWith("audio") ||
                name.substringAfterLast('.').lowercase() in AUDIO_EXTENSIONS
            if (!isAudio) continue
            val messageId = message.optLong("id")
            if (messageId == 0L) continue
            files += TelegramFile(
                messageId = messageId,
                fileId = message.optString("file_id").ifBlank { message.optJSONObject("file")?.optString("id").orEmpty() },
                name = name,
                artist = message.optString("performer").ifBlank { "Artista desconocido" },
                album = message.optString("album").ifBlank { "Telegram" },
                durationMs = (message.opt("duration_seconds")?.toString()?.toFloatOrNull() ?: 0f).times(1000).toLong(),
                size = message.optString("file_size").toLongOrNull() ?: message.optString("size").toLongOrNull() ?: 0L,
                hash = message.optString("hash").ifBlank { "" },
                date = runCatching { java.time.Instant.parse(message.optString("date_unixtime").let { if (it.isBlank()) "1970-01-01T00:00:00Z" else java.time.Instant.ofEpochSecond(it.toLong()).toString() }).toEpochMilli() }.getOrDefault(System.currentTimeMillis())
            )
        }
        return files
    }

    private val AUDIO_EXTENSIONS = setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus", "oga", "m4b", "aiff")
}
