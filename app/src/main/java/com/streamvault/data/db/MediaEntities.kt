package com.streamvault.data.db

import androidx.room.*

/**
 * Playlists, favoritos e historial.
 * El campo `source` mantiene la SEPARACIÓN ABSOLUTA entre:
 * LOCAL (📱 teléfono), YOUTUBE, YTMUSIC y TELEGRAM.
 * Nunca se mezclan fuentes dentro de una playlist.
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val source: String, // LOCAL | YTMUSIC | TELEGRAM
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    foreignKeys = [ForeignKey(
        entity = PlaylistEntity::class,
        parentColumns = ["id"],
        childColumns = ["playlistId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("playlistId")]
)
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val title: String,
    val artist: String?,
    val uri: String,
    val artUri: String? = null,
    val duration: Long = 0L,
    val position: Int = 0
)

@Entity(tableName = "music_favorites")
data class MusicFavoriteEntity(
    @PrimaryKey val uri: String, // contentUri del archivo local
    val source: String = "LOCAL",
    val title: String,
    val artist: String?,
    val album: String? = null,
    val artUri: String? = null,
    val duration: Long = 0L,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val uri: String,
    val title: String,
    val artist: String? = null,
    val artUri: String? = null,
    val playedAt: Long = System.currentTimeMillis(),
    val progressMs: Long = 0L,
    val durationMs: Long = 0L
)

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists WHERE source = :source ORDER BY createdAt DESC")
    fun bySource(source: String): kotlinx.coroutines.flow.Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun all(): kotlinx.coroutines.flow.Flow<List<PlaylistEntity>>

    @Insert suspend fun insert(p: PlaylistEntity): Long
    @Delete suspend fun delete(p: PlaylistEntity)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    fun tracks(playlistId: Long): kotlinx.coroutines.flow.Flow<List<PlaylistTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTrack(t: PlaylistTrackEntity)
    @Delete suspend fun deleteTrack(t: PlaylistTrackEntity)
}

@Dao
interface MusicFavoritesDao {
    @Query("SELECT * FROM music_favorites WHERE source = :source ORDER BY addedAt DESC")
    fun bySource(source: String): kotlinx.coroutines.flow.Flow<List<MusicFavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(f: MusicFavoriteEntity)
    @Query("DELETE FROM music_favorites WHERE uri = :uri") suspend fun deleteByUri(uri: String)
    @Query("SELECT EXISTS(SELECT 1 FROM music_favorites WHERE uri = :uri)") suspend fun exists(uri: String): Boolean
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT 500")
    fun recent(): kotlinx.coroutines.flow.Flow<List<PlayHistoryEntity>>

    @Insert suspend fun insert(h: PlayHistoryEntity)
    @Query("DELETE FROM play_history") suspend fun clear()
}
