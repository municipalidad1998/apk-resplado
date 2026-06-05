package com.streamvault
import android.app.Application
import androidx.room.Room
import com.streamvault.data.db.AppDatabase
class StreamVaultApp : Application() {
    companion object { lateinit var db: AppDatabase }
    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, AppDatabase::class.java, "streamvault.db").build()
    }
}
