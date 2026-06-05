package com.streamvault.data.model
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
@Parcelize
@Entity(tableName = "favorites")
data class Channel(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,
    val sourceId: String = "",
    val type: String = "LIVE"
) : Parcelable
@Parcelize
data class Movie(val id: String, val name: String, val cover: String? = null,
    val plot: String? = null, val genre: String? = null, val rating: String? = null,
    val streamUrl: String = "", val sourceId: String = "") : Parcelable
@Parcelize
data class Series(val id: String, val name: String, val cover: String? = null,
    val plot: String? = null, val genre: String? = null, val rating: String? = null,
    val sourceId: String = "") : Parcelable
