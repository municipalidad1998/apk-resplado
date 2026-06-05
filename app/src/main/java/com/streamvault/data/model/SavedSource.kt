package com.streamvault.data.model
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
@Entity(tableName = "saved_sources")
data class SavedSource(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val m3uUrl: String? = null,
    val localFilePath: String? = null,
    val xtreamUrl: String? = null,
    val xtreamUser: String? = null,
    val xtreamPass: String? = null
)
