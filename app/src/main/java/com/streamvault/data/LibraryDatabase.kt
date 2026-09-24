package com.streamvault.data

import androidx.room.*
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tracks", indices = [Index("title"), Index("lastPlayed"), Index("addedAt")])
data class Track(
    @PrimaryKey val id: String,
    val uri: String,
    val fileName: String,
    val title: String,
    val artist: String = "Artista desconocido",
    val album: String = "Sin álbum",
    val genre: String = "Sin género",
    val folder: String = "",
    val durationMs: Long = 0,
    val size: Long = 0,
    val date: Long = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val cover: String? = null,
    val customName: String = "",
    val notes: String = "",
    val tags: String = "",
    val source: String = "music",
    val favorite: Boolean = false,
    val plays: Int = 0,
    val lastPlayed: Long = 0,
    val detectedOffsetMs: Long = 0,
    val manualOffsetMs: Long? = null,
    val analysisKey: String = "",
    val waveform: String = "",
    val hidden: Boolean = false,
    val available: Boolean = true
) {
    val displayName: String get() = customName.ifBlank { title }
    fun offset(enabled: Boolean): Long = (manualOffsetMs ?: if (enabled) detectedOffsetMs else 0)
        .coerceIn(0, (durationMs - 100).coerceAtLeast(0))
}

@Entity(tableName = "locations", indices = [Index("trackId"), Index("root")])
data class AudioLocation(@PrimaryKey val uri: String, val trackId: String, val root: String,
                         val size: Long, val modified: Long, val seen: String)
@Entity(tableName = "playlists")
data class Playlist(@PrimaryKey val id: String, val name: String, val description: String = "", val cover: String? = null)
@Entity(tableName = "playlist_entries", primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [ForeignKey(entity = Playlist::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Track::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("trackId")])
data class PlaylistEntry(val playlistId: String, val trackId: String, val position: Int)
@Entity(tableName = "queue")
data class QueueEntry(@PrimaryKey val position: Int, val trackId: String)

@Dao
interface LibraryDao {
    @Query("SELECT * FROM tracks WHERE hidden = 0 AND available = 1 AND (:sort != 'recent' OR lastPlayed > 0) AND (:source = '' OR source = :source) AND (:favorite = 0 OR favorite = 1) AND (:folder = '' OR folder = :folder) AND (:artist = '' OR artist = :artist) AND (:album = '' OR album = :album) AND (:genre = '' OR genre = :genre) AND (title LIKE :query ESCAPE '\\' OR customName LIKE :query ESCAPE '\\' OR artist LIKE :query ESCAPE '\\' OR album LIKE :query ESCAPE '\\' OR genre LIKE :query ESCAPE '\\' OR folder LIKE :query ESCAPE '\\' OR tags LIKE :query ESCAPE '\\' OR fileName LIKE :query ESCAPE '\\') ORDER BY CASE WHEN :sort = 'recent' THEN lastPlayed WHEN :sort = 'added' THEN addedAt ELSE 0 END DESC, title COLLATE NOCASE")
    fun page(query: String, source: String, favorite: Boolean, folder: String, artist: String, album: String, genre: String, sort: String): PagingSource<Int, Track>
    @Query("SELECT * FROM tracks WHERE hidden = 0 AND available = 1 ORDER BY lastPlayed DESC, addedAt DESC LIMIT 20")
    fun home(): Flow<List<Track>>
    @Query("SELECT * FROM tracks WHERE hidden = 0 AND available = 1 AND favorite = 1 ORDER BY title LIMIT 20")
    fun favorites(): Flow<List<Track>>
    @Query("SELECT * FROM tracks WHERE hidden = 0 AND available = 1 AND source = :source ORDER BY addedAt DESC LIMIT 20")
    fun source(source: String): Flow<List<Track>>
    @Query("SELECT * FROM tracks WHERE hidden = 0 AND available = 1 ORDER BY addedAt DESC LIMIT 20")
    fun added(): Flow<List<Track>>
    @Query("SELECT COUNT(*) FROM tracks WHERE hidden = 0 AND available = 1") fun count(): Flow<Int>
    @Query("SELECT * FROM tracks WHERE id = :id") suspend fun track(id: String): Track?
    @Query("SELECT * FROM tracks WHERE id = :id") fun observeTrack(id: String): Flow<Track?>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(track: Track): Long
    @Update suspend fun update(track: Track)
    @Query("UPDATE tracks SET favorite = NOT favorite WHERE id = :id") suspend fun favorite(id: String)
    @Query("UPDATE tracks SET plays = plays + 1, lastPlayed = :time WHERE id = :id") suspend fun played(id: String, time: Long)
    @Query("UPDATE tracks SET hidden = 1 WHERE id = :id") suspend fun hide(id: String)
    @Query("UPDATE tracks SET hidden = 0") suspend fun unhideAll()
    @Query("UPDATE tracks SET detectedOffsetMs = :offset, waveform = :waveform, analysisKey = :key WHERE id = :id")
    suspend fun analysis(id: String, offset: Long, waveform: String, key: String)
    @Query("UPDATE tracks SET manualOffsetMs = :offset WHERE id = :id") suspend fun manualOffset(id: String, offset: Long?)
    @Query("SELECT * FROM tracks WHERE hidden = 0 AND available = 1 AND analysisKey != :key LIMIT 25") suspend fun pendingAnalysis(key: String): List<Track>
    @Query("SELECT * FROM locations WHERE uri = :uri") suspend fun location(uri: String): AudioLocation?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun location(location: AudioLocation)
    @Query("UPDATE locations SET seen = :seen WHERE uri = :uri") suspend fun seen(uri: String, seen: String)
    @Query("DELETE FROM locations WHERE root = :root AND seen != :seen") suspend fun prune(root: String, seen: String)
    @Query("DELETE FROM locations WHERE root = :root") suspend fun forgetRoot(root: String)
    @Query("SELECT * FROM locations WHERE trackId = :id") suspend fun locations(id: String): List<AudioLocation>
    @Query("UPDATE tracks SET available = EXISTS(SELECT 1 FROM locations WHERE trackId = tracks.id), uri = COALESCE((SELECT uri FROM locations WHERE trackId = tracks.id LIMIT 1), uri)")
    suspend fun reconcile()
    @Query("SELECT DISTINCT folder FROM tracks WHERE hidden = 0 AND available = 1 ORDER BY folder") fun folders(): Flow<List<String>>
    @Query("SELECT DISTINCT artist FROM tracks WHERE hidden = 0 AND available = 1 ORDER BY artist") fun artists(): Flow<List<String>>
    @Query("SELECT DISTINCT album FROM tracks WHERE hidden = 0 AND available = 1 ORDER BY album") fun albums(): Flow<List<String>>
    @Query("SELECT DISTINCT genre FROM tracks WHERE hidden = 0 AND available = 1 ORDER BY genre") fun genres(): Flow<List<String>>
    @Query("SELECT * FROM playlists ORDER BY name") fun playlists(): Flow<List<Playlist>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun playlist(playlist: Playlist)
    @Query("DELETE FROM playlists WHERE id = :id") suspend fun deletePlaylist(id: String)
    @Query("SELECT tracks.* FROM tracks INNER JOIN playlist_entries ON tracks.id = trackId WHERE playlistId = :id AND hidden = 0 AND available = 1 ORDER BY position")
    fun playlistTracks(id: String): Flow<List<Track>>
    @Query("SELECT tracks.* FROM tracks INNER JOIN playlist_entries ON tracks.id = trackId WHERE playlistId = :id AND hidden = 0 AND available = 1 ORDER BY position")
    suspend fun getPlaylistTracks(id: String): List<Track>
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_entries WHERE playlistId = :id") suspend fun nextPosition(id: String): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun entry(entry: PlaylistEntry)
    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlist AND trackId = :track") suspend fun removeEntry(playlist: String, track: String)
    @Query("DELETE FROM playlist_entries WHERE playlistId = :id") suspend fun clearPlaylist(id: String)
    @Transaction suspend fun addToPlaylist(playlist: String, track: String) { entry(PlaylistEntry(playlist, track, nextPosition(playlist))) }
    @Transaction suspend fun reorderPlaylist(id: String, tracks: List<String>) {
        clearPlaylist(id); tracks.forEachIndexed { i, t -> entry(PlaylistEntry(id, t, i)) }
    }
    @Query("SELECT * FROM tracks WHERE album = :album AND hidden = 0 AND available = 1 ORDER BY title") suspend fun albumTracks(album: String): List<Track>
    @Query("SELECT tracks.* FROM tracks INNER JOIN queue ON tracks.id = trackId WHERE available = 1 AND hidden = 0 ORDER BY position") suspend fun savedQueue(): List<Track>
    @Query("DELETE FROM queue") suspend fun clearQueue()
    @Insert suspend fun queue(entries: List<QueueEntry>)
    @Transaction suspend fun saveQueue(ids: List<String>) { clearQueue(); queue(ids.mapIndexed { i, id -> QueueEntry(i, id) }) }
}

@Database(entities = [Track::class, AudioLocation::class, Playlist::class, PlaylistEntry::class, QueueEntry::class], version = 1, exportSchema = true)
abstract class LibraryDatabase : RoomDatabase() { abstract fun library(): LibraryDao }

fun searchPattern(value: String) = "%${value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")}%"
