package com.streamvault.data.repository
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.streamvault.data.model.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit
class XtreamRepository {
    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    private val gson = Gson()
    fun getLiveChannels(base: String, user: String, pass: String, srcId: String): List<Channel> = try {
        gson.fromJson(fetch("$base/get.php?username=$user&password=$pass&action=get_live_streams"), JsonArray::class.java).map {
            val o = it.asJsonObject
            val sid = o.get("stream_id")?.asString ?: UUID.randomUUID().toString()
            val ext = o.get("container_extension")?.asString ?: "ts"
            Channel(id=sid, name=o.get("name")?.asString?:"Canal", url="$base/live/$user/$pass/$sid.$ext",
                logo=o.get("stream_icon")?.asString, group=o.get("category_id")?.asString, sourceId=srcId, type="LIVE")
        }
    } catch (e: Exception) { emptyList() }
    fun getMovies(base: String, user: String, pass: String, srcId: String): List<Movie> = try {
        gson.fromJson(fetch("$base/get.php?username=$user&password=$pass&action=get_vod_streams"), JsonArray::class.java).map {
            val o = it.asJsonObject
            val sid = o.get("stream_id")?.asString ?: UUID.randomUUID().toString()
            val ext = o.get("container_extension")?.asString ?: "mp4"
            Movie(id=sid, name=o.get("name")?.asString?:"Película", cover=o.get("stream_icon")?.asString,
                rating=o.get("rating")?.asString, streamUrl="$base/movie/$user/$pass/$sid.$ext", sourceId=srcId)
        }
    } catch (e: Exception) { emptyList() }
    fun getSeries(base: String, user: String, pass: String, srcId: String): List<Series> = try {
        gson.fromJson(fetch("$base/get.php?username=$user&password=$pass&action=get_series"), JsonArray::class.java).map {
            val o = it.asJsonObject
            Series(id=o.get("series_id")?.asString?:UUID.randomUUID().toString(), name=o.get("name")?.asString?:"Serie",
                cover=o.get("cover")?.asString, plot=o.get("plot")?.asString, genre=o.get("genre")?.asString, sourceId=srcId)
        }
    } catch (e: Exception) { emptyList() }
    private fun fetch(url: String): String {
        val req = Request.Builder().url(url).build()
        return client.newCall(req).execute().use { it.body?.string() ?: "[]" }
    }
}
