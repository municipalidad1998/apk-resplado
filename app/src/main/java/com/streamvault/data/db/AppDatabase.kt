package com.streamvault.data.db

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.streamvault.data.model.Channel
import com.streamvault.data.model.SavedSource
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites") fun getAll(): Flow<List<Channel>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(channel: Channel)
    @Delete suspend fun delete(channel: Channel)
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)") suspend fun exists(id: String): Boolean
}

@Dao
interface SourcesDao {
    @Query("SELECT * FROM saved_sources") fun getAll(): Flow<List<SavedSource>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(source: SavedSource)
    @Delete suspend fun delete(source: SavedSource)
}

@Database(
    entities = [
        Channel::class, SavedSource::class,
        PlaylistEntity::class, PlaylistTrackEntity::class,
        MusicFavoriteEntity::class, PlayHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
    abstract fun sourcesDao(): SourcesDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun musicFavoritesDao(): MusicFavoritesDao
    abstract fun historyDao(): HistoryDao

    companion object {
        /**
         * Migración 1 → 2: crea las tablas nuevas SIN tocar los datos
         * existentes (favorites, saved_sources). Permite actualizar la APK
         * sin perder nada.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `playlists` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL)"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `playlist_tracks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `playlistId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT,
                        `uri` TEXT NOT NULL,
                        `artUri` TEXT,
                        `duration` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON DELETE CASCADE)"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_tracks_playlistId` ON `playlist_tracks` (`playlistId`)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `music_favorites` (
                        `uri` TEXT PRIMARY KEY NOT NULL,
                        `source` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT,
                        `album` TEXT,
                        `artUri` TEXT,
                        `duration` INTEGER NOT NULL,
                        `addedAt` INTEGER NOT NULL)"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `play_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `source` TEXT NOT NULL,
                        `uri` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT,
                        `artUri` TEXT,
                        `playedAt` INTEGER NOT NULL,
                        `progressMs` INTEGER NOT NULL,
                        `durationMs` INTEGER NOT NULL)"""
                )
            }
        }
    }
}
