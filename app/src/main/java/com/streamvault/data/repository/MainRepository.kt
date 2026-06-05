package com.streamvault.data.repository
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.streamvault.data.model.*
import com.streamvault.util.M3UParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
class MainRepository(private val prefs: SharedPreferences) {
    private val gson = Gson()
    private val xtream = XtreamRepository()
    private val client = OkHttpClient.Builder().connectTimeout(30,TimeUnit.SECONDS).readTimeout(120,TimeUnit.SECONDS).build()
    fun getSources(): List<SavedSource> {
        val json = prefs.getString("sources","[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<SavedSource>>() {}.type) ?: emptyList()
    }
    fun saveSource(source: SavedSource) {
        val list = getSources().toMutableList().also { it.removeIf { s -> s.id == source.id }; it.add(source) }
        prefs.edit().putString("sources", gson.toJson(list)).apply()
    }
    fun deleteSource(id: String) {
        prefs.edit().putString("sources", gson.toJson(getSources().filter { it.id != id })).apply()
    }
    suspend fun loadChannels(source: SavedSource): List<Channel> = withContext(Dispatchers.IO) {
        when (source.type) {
            "M3U_URL" -> M3UParser.parse(fetch(source.m3uUrl ?: return@withContext emptyList()), source.id)
            "XTREAM" -> xtream.getLiveChannels(source.xtreamUrl?:"", source.xtreamUser?:"", source.xtreamPass?:"", source.id)
            else -> emptyList()
        }
    }
    suspend fun loadMovies(source: SavedSource): List<Movie> = withContext(Dispatchers.IO) {
        if (source.type != "XTREAM") return@withContext emptyList()
        xtream.getMovies(source.xtreamUrl?:"", source.xtreamUser?:"", source.xtreamPass?:"", source.id)
    }
    suspend fun loadSeries(source: SavedSource): List<Series> = withContext(Dispatchers.IO) {
        if (source.type != "XTREAM") return@withContext emptyList()
        xtream.getSeries(source.xtreamUrl?:"", source.xtreamUser?:"", source.xtreamPass?:"", source.id)
    }
    private fun fetch(url: String): String {
        val req = Request.Builder().url(url).build()
        return client.newCall(req).execute().use { it.body?.string() ?: "" }
    }
}
