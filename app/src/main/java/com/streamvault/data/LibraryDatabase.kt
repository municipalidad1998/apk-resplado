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
    val albumArtist: String = "",
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
    val playbackEndMs: Long? = null,
    val crossfadeSeconds: Int? = null,
    val loudnessDb: Float? = null,
    val peakDb: Float? = null,
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val analysisKey: String = "",
    val waveform: String = "",
    val hidden: Boolean = false,
    val available: Boolean = true
) {
    val displayName: String get() = customName.ifBlank { title }
    val artworkKey: String get() = "$displayName|$artist|$album"
    fun offset(enabled: Boolean): Long {
        val offset = (manualOffsetMs ?: if (enabled) detectedOffsetMs else 0).coerceAtLeast(0)
        return if (durationMs > 0) offset.coerceAtMost((durationMs - 100).coerceAtLeast(0)) else offset
    }
}

@Entity(tableName = "locations", indices = [Index("trackId"), Index("root")])
data class AudioLocation(@PrimaryKey val uri: String, val trackId: String, val root: String,
                         val size: Long, val modified: Long, val seen: String)
@Entity(tableName = "playlists")
data class Playlist(@PrimaryKey val id: String, val name: String, val description: String = "", val cover: String? = null,
                    /** "local": solo archivos del teléfono. "online": solo Internet. "mixed": las dos. */
                    val kind: String = "local") {
    companion object { const val KIND_LOCAL = "local"; const val KIND_ONLINE = "online"; const val KIND_MIXED = "mixed" }
}
@Entity(tableName = "playlist_entries", primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [ForeignKey(entity = Playlist::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Track::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("trackId")])
data class PlaylistEntry(val playlistId: String, val trackId: String, val position: Int)
@Entity(tableName = "queue")
data class QueueEntry(@PrimaryKey val position: Int, val trackId: String)

/** Lean projection: the full playback queue never loads waveform blobs or image pixels. */
data class QueueRecord(val id: String, val uri: String, val title: String, val artist: String,
                       val album: String, val durationMs: Long, val cover: String?, val customName: String,
                       val detectedOffsetMs: Long, val manualOffsetMs: Long?, val playbackEndMs: Long?, val crossfadeSeconds: Int?,
                       val loudnessDb: Float? = null, val peakDb: Float? = null) {
    fun toTrack() = Track(id, uri, "", title, artist = artist, album = album, durationMs = durationMs,
        cover = cover, customName = customName, detectedOffsetMs = detectedOffsetMs, manualOffsetMs = manualOffsetMs,
        playbackEndMs = playbackEndMs, crossfadeSeconds = crossfadeSeconds, loudnessDb = loudnessDb, peakDb = peakDb)
}

@Dao
interface LibraryDao {
    @Query("SELECT * FROM tracks WHERE hidden = 0 AND available = 1 AND (:sort != 'recent' OR lastPlayed > 0) AND (:source = '' OR source = :source) AND (:favorite = 0 OR favorite = 1) AND (:folder = '' OR folder = :folder) AND (:artist = '' OR artist = :artist) AND (:album = '' OR album = :album) AND (:genre = '' OR genre = :genre) AND source != 'online' AND (title LIKE :query ESCAPE '\\' OR customName LIKE :query ESCAPE '\\' OR artist LIKE :query ESCAPE '\\' OR album LIKE :query ESCAPE '\\' OR genre LIKE :query ESCAPE '\\' OR folder LIKE :query ESCAPE '\\' OR tags LIKE :query ESCAPE '\\' OR fileName LIKE :query ESCAPE '\\') ORDER BY CASE WHEN :sort = 'recent' THEN lastPlayed WHEN :sort = 'added' THEN addedAt ELSE 0 END DESC, title COLLATE NOCASE")
    fun page(query: String, source: String, favorite: Boolean, folder: String, artist: String, album: String, genre: String, sort: String): PagingSource<Int, Track>
    @Query("SELECT id, uri, title, artist, album, durationMs, cover, customName, detectedOffsetMs, manualOffsetMs, playbackEndMs, crossfadeSeconds, loudnessDb, peakDb FROM tracks WHERE hidden = 0 AND available = 1 AND (:sort != 'recent' OR lastPlayed > 0) AND (:source = '' OR source = :source) AND (:favorite = 0 OR favorite = 1) AND (:folder = '' OR folder = :folder) AND (:artist = '' OR artist = :artist) AND (:album = '' OR album = :album) AND (:genre = '' OR genre = :genre) AND source != 'online' AND (title LIKE :query ESCAPE '\\' OR customName LIKE :query ESCAPE '\\' OR artist LIKE :query ESCAPE '\\' OR album LIKE :query ESCAPE '\\' OR genre LIKE :query ESCAPE '\\' OR folder LIKE :query ESCAPE '\\' OR tags LIKE :query ESCAPE '\\' OR fileName LIKE :query ESCAPE '\\') ORDER BY CASE WHEN :sort = 'recent' THEN lastPlayed WHEN :sort = 'added' THEN addedAt ELSE 0 END DESC, title COLLATE NOCASE")
    suspend fun playbackQueue(query: String, source: String, favorite: Boolean, folder: String, artist: String, album: String, genre: String, sort: String): List<QueueRecord>
    @Query("SELECT * FROM tracks WHERE hidden = 0 AND available = 1 AND source != 'online' ORDER BY lastPlayed DESC, addedAt DESC LIMIT 20")
    fun home(): Flow<List<Track>>
    @Query("SELECT * FROM tracks WHERE hidden = 0 AND available = 1 AND favorite = 1 ORDER BY title LIMIT 20")
    fun favorites(): Flow<List<Track>>
    @Query("SELECT * FROM tracks WHERE hidden = 0 AND available = 1 AND source = :source ORDER BY addedAt DESC LIMIT 20")
    fun source(source: String): Flow<List<Track>>
    @Query("SELECT * FROM tracks WHERE hidden = 0 AND available = 1 AND source != 'online' ORDER BY addedAt DESC LIMIT 20")
    fun added(): Flow<List<Track>>
    @Query("SELECT COUNT(*) FROM tracks WHERE hidden = 0 AND available = 1") fun count(): Flow<Int>
    @Query("SELECT COUNT(*) FROM tracks WHERE hidden = 0 AND available = 1 AND source != 'online'") fun localCount(): Flow<Int>
    @Query("SELECT * FROM tracks WHERE id = :id") suspend fun track(id: String): Track?
    @Query("SELECT * FROM tracks WHERE id = :id") fun observeTrack(id: String): Flow<Track?>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(track: Track): Long
    @Update suspend fun update(track: Track)
    @Query("UPDATE tracks SET durationMs = :duration WHERE id = :id AND durationMs <= 0")
    suspend fun discoveredDuration(id: String, duration: Long)
    @Query("UPDATE tracks SET favorite = NOT favorite WHERE id = :id") suspend fun favorite(id: String)
    @Query("UPDATE tracks SET plays = plays + 1, lastPlayed = :time WHERE id = :id") suspend fun played(id: String, time: Long)
    @Query("UPDATE tracks SET hidden = 1 WHERE id = :id") suspend fun hide(id: String)
    @Query("UPDATE tracks SET hidden = 0") suspend fun unhideAll()
    @Query("UPDATE tracks SET detectedOffsetMs = :offset, waveform = :waveform, analysisKey = :key WHERE id = :id")
    suspend fun analysis(id: String, offset: Long, waveform: String, key: String)
    @Query("UPDATE tracks SET manualOffsetMs = :offset WHERE id = :id") suspend fun manualOffset(id: String, offset: Long?)
    @Query("SELECT * FROM tracks WHERE hidden = 0 AND available = 1 AND analysisKey != :key AND analysisKey != 'failed:' || :key LIMIT 25") suspend fun pendingAnalysis(key: String): List<Track>
    @Query("UPDATE tracks SET playbackEndMs = :end, crossfadeSeconds = :seconds WHERE id = :id")
    suspend fun transition(id: String, end: Long?, seconds: Int?)
    @Query("UPDATE tracks SET loudnessDb = :db, peakDb = :peak WHERE id = :id")
    suspend fun loudness(id: String, db: Float?, peak: Float?)
    @Query("UPDATE tracks SET title = :title, artist = :artist, album = :album, genre = :genre, date = :year, trackNumber = :track, discNumber = :disc WHERE id = :id")
    suspend fun editMetadata(id: String, title: String, artist: String, album: String, genre: String, year: Long, track: Int, disc: Int)
    @RawQuery
    suspend fun candidates(query: androidx.sqlite.db.SupportSQLiteQuery): List<Track>
    @Query("SELECT * FROM tracks WHERE fileName = :name AND size = :size LIMIT 1") suspend fun byFile(name: String, size: Long): Track?
    @Query("SELECT tracks.id, uri, title, artist, album, durationMs, cover, customName, detectedOffsetMs, manualOffsetMs, playbackEndMs, crossfadeSeconds, loudnessDb, peakDb FROM tracks INNER JOIN queue ON tracks.id = trackId WHERE available = 1 AND hidden = 0 ORDER BY position")
    fun observeQueueCues(): Flow<List<QueueRecord>>
    @Query("SELECT * FROM locations WHERE uri = :uri") suspend fun location(uri: String): AudioLocation?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun location(location: AudioLocation)
    @Query("UPDATE locations SET seen = :seen WHERE uri = :uri") suspend fun seen(uri: String, seen: String)
    @Query("DELETE FROM locations WHERE root = :root AND seen != :seen") suspend fun prune(root: String, seen: String)
    @Query("DELETE FROM locations WHERE root = :root") suspend fun forgetRoot(root: String)
    @Query("SELECT * FROM locations WHERE trackId = :id") suspend fun locations(id: String): List<AudioLocation>
    @Query("UPDATE tracks SET available = CASE WHEN source = 'online' THEN 1 ELSE EXISTS(SELECT 1 FROM locations WHERE trackId = tracks.id) END, uri = CASE WHEN source = 'online' THEN uri ELSE COALESCE((SELECT uri FROM locations WHERE trackId = tracks.id LIMIT 1), uri) END")
    suspend fun reconcile()
    @Query("SELECT DISTINCT folder FROM tracks WHERE hidden = 0 AND available = 1 AND source != 'online' ORDER BY folder") fun folders(): Flow<List<String>>
    @Query("SELECT DISTINCT artist FROM tracks WHERE hidden = 0 AND available = 1 AND source != 'online' ORDER BY artist") fun artists(): Flow<List<String>>
    @Query("SELECT DISTINCT album FROM tracks WHERE hidden = 0 AND available = 1 AND source != 'online' ORDER BY album") fun albums(): Flow<List<String>>
    @Query("SELECT DISTINCT genre FROM tracks WHERE hidden = 0 AND available = 1 AND source != 'online' ORDER BY genre") fun genres(): Flow<List<String>>
    @Query("SELECT * FROM playlists ORDER BY name") fun playlists(): Flow<List<Playlist>>
    @Query("SELECT * FROM playlists ORDER BY name") suspend fun playlistsList(): List<Playlist>
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
    @Transaction suspend fun addToPlaylist(playlist: String, track: String) {
        entry(PlaylistEntry(playlist, track, nextPosition(playlist)))
        refreshKind(playlist)
    }

    /** Removing a song can turn a mixed playlist back into a local one. */
    @Transaction suspend fun detach(playlist: String, track: String) {
        removeEntry(playlist, track)
        refreshKind(playlist)
    }

    /**
     * The kind is derived from the songs it holds, never guessed: a playlist is online only if
     * every song comes from the internet, mixed when it has both, local when all are files.
     */
    @Query("UPDATE playlists SET kind = CASE " +
        "WHEN EXISTS(SELECT 1 FROM playlist_entries e INNER JOIN tracks t ON t.id = e.trackId WHERE e.playlistId = :id AND t.source = 'online') " +
        "AND EXISTS(SELECT 1 FROM playlist_entries e INNER JOIN tracks t ON t.id = e.trackId WHERE e.playlistId = :id AND t.source != 'online') THEN 'mixed' " +
        "WHEN EXISTS(SELECT 1 FROM playlist_entries e INNER JOIN tracks t ON t.id = e.trackId WHERE e.playlistId = :id AND t.source = 'online') THEN 'online' " +
        "ELSE 'local' END WHERE id = :id")
    suspend fun refreshKind(id: String)
    @Transaction suspend fun reorderPlaylist(id: String, tracks: List<String>) {
        clearPlaylist(id); tracks.forEachIndexed { i, t -> entry(PlaylistEntry(id, t, i)) }
    }
    @Query("SELECT * FROM tracks WHERE album = :album AND hidden = 0 AND available = 1 ORDER BY title") suspend fun albumTracks(album: String): List<Track>
    @Query("SELECT tracks.id, uri, title, artist, album, durationMs, cover, customName, detectedOffsetMs, manualOffsetMs, playbackEndMs, crossfadeSeconds, loudnessDb, peakDb FROM tracks INNER JOIN queue ON tracks.id = trackId WHERE available = 1 AND hidden = 0 ORDER BY position") suspend fun savedQueue(): List<QueueRecord>
    @Query("DELETE FROM queue") suspend fun clearQueue()
    @Insert suspend fun queue(entries: List<QueueEntry>)
    @Transaction suspend fun saveQueue(ids: List<String>) { clearQueue(); queue(ids.mapIndexed { i, id -> QueueEntry(i, id) }) }
}

@Database(entities = [Track::class, AudioLocation::class, Playlist::class, PlaylistEntry::class, QueueEntry::class, com.streamvault.telegram.TelegramFile::class], version = 5, exportSchema = true)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun library(): LibraryDao
    abstract fun telegram(): com.streamvault.telegram.TelegramDao
    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                addColumn(db, "tracks", "playbackEndMs", "INTEGER")
                addColumn(db, "tracks", "crossfadeSeconds", "INTEGER")
            }
        }
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                addColumn(db, "tracks", "loudnessDb", "REAL")
            }
        }
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                addColumn(db, "tracks", "peakDb", "REAL")
                addColumn(db, "tracks", "trackNumber", "INTEGER NOT NULL DEFAULT 0")
                addColumn(db, "tracks", "discNumber", "INTEGER NOT NULL DEFAULT 0")
                addColumn(db, "tracks", "albumArtist", "TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS telegram_files (messageId INTEGER NOT NULL PRIMARY KEY, fileId TEXT NOT NULL, name TEXT NOT NULL, artist TEXT NOT NULL, album TEXT NOT NULL, durationMs INTEGER NOT NULL, size INTEGER NOT NULL, hash TEXT NOT NULL, trackId TEXT, date INTEGER NOT NULL)")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Playlists remember whether they hold phone files, online songs or both.
                addColumn(db, "playlists", "kind", "TEXT NOT NULL DEFAULT 'local'")
            }
        }

        /**
         * ALTER TABLE fails if the column is already there, which happens with databases that were
         * touched by a previous build or by another tool. Checking first keeps the upgrade safe.
         */
        fun addColumn(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String, column: String, definition: String) {
            val present = db.query("PRAGMA table_info($table)").use { cursor ->
                val names = mutableListOf<String>()
                val index = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) names += cursor.getString(index)
                names
            }
            if (column !in present) db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
        }
    }
}

fun searchPattern(value: String) = "%${value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")}%"
