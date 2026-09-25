package com.streamvault

import android.app.Application
import androidx.room.Room
import com.streamvault.data.LibraryDatabase
import com.streamvault.settings.Preferences
import com.streamvault.artwork.ArtworkStore
import com.streamvault.processing.SeparationRegistry

class LuminaApp : Application() {
    val scanMutex = kotlinx.coroutines.sync.Mutex()
    val database by lazy { Room.databaseBuilder(this, LibraryDatabase::class.java, "lumina-library.db").addMigrations(LibraryDatabase.MIGRATION_1_2, LibraryDatabase.MIGRATION_2_3).build() }
    val analysis by lazy { com.streamvault.analysis.AnalysisRepository(this) }
    val library get() = database.library()
    val preferences by lazy { Preferences(this) }
    val artwork by lazy { ArtworkStore(this) }
    val separation by lazy { SeparationRegistry() }
}
