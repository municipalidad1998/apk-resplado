package com.streamvault.data.db

import androidx.room.*

/**
 * Metadatos de archivos en ☁ Telegram Cloud.
 * Un archivo eliminado de la PC/teléfono SIGUE apareciendo aquí
 * (solo depende de Telegram, nunca del dispositivo).
 */
@Entity(tableName = "telegram_files")
data class TelegramFileEntity(
    @PrimaryKey val fileUid: String,          // SHA-256 del archivo completo
    val originalName: String,
    val localPath: String?,                    // puede ser null si ya no existe en el dispositivo
    val category: String,                      // PHOTO | VIDEO | MUSIC | DOC | ARCHIVE
    val sizeBytes: Long,
    val sha256: String,
    val chatId: Long = 0,                      // canal/grupo privado de almacenamiento
    val uploadedAt: Long = System.currentTimeMillis(),
    val deletedFromLocal: Boolean = false
)

@Entity(
    tableName = "telegram_chunks",
    foreignKeys = [ForeignKey(
        entity = TelegramFileEntity::class,
        parentColumns = ["fileUid"],
        childColumns = ["fileUid"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("fileUid")]
)
data class TelegramChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileUid: String,
    val chunkIndex: Int,
    val telegramMessageId: Long = 0,           // message_id en el canal de respaldo
    val telegramFileId: String = "",           // file_id de TDLib/MTProto
    val sha256: String,
    val sizeBytes: Long,
    val uploaded: Boolean = false
)

@Dao
interface TelegramFilesDao {
    @androidx.room.Query("SELECT * FROM telegram_files ORDER BY uploadedAt DESC")
    fun all(): kotlinx.coroutines.flow.Flow<List<TelegramFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(f: TelegramFileEntity)
    @Query("DELETE FROM telegram_files WHERE fileUid = :uid") suspend fun delete(uid: String)
    @Query("SELECT * FROM telegram_files WHERE fileUid = :uid") suspend fun byId(uid: String): TelegramFileEntity?

    @Query("SELECT * FROM telegram_chunks WHERE fileUid = :uid ORDER BY chunkIndex ASC")
    suspend fun chunks(uid: String): List<TelegramChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertChunk(c: TelegramChunkEntity)
}
