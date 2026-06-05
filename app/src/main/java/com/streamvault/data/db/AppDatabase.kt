package com.streamvault.data.db
import androidx.room.*
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
@Database(entities = [Channel::class, SavedSource::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
    abstract fun sourcesDao(): SourcesDao
}
