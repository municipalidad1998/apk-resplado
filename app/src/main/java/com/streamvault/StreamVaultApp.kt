package com.streamvault
import android.app.Application
import androidx.room.Room
import com.streamvault.data.db.AppDatabase
class StreamVaultApp : Application() {
    companion object {
        lateinit var db: AppDatabase
        lateinit var telegramClient: com.streamvault.data.cloud.TelegramClient
    }
    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, AppDatabase::class.java, "streamvault.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()
        telegramClient = com.streamvault.data.cloud.TelegramClient(this)
        telegramClient.start()
    }
}
