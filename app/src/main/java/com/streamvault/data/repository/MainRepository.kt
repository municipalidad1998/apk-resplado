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

    // Timeout generoso para listas M3U grandes (hasta 50MB)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)   // 5 minutos para listas enormes
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun getSources(): List<SavedSource> {
        val json = prefs.getString("sources", "[]") ?: "[]"
        return try {
            gson.fromJson(json, object : TypeToken<List<SavedSource>>() {}.type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    fun saveSource(source: SavedSource) {
        val list = getSources().toMutableList()
        list.removeIf { it.id == source.id }
        list.add(source)
        prefs.edit().putString("sources", gson.toJson(list)).apply()
    }

    fun deleteSource(id: String) {
        prefs.edit().putString("sources", gson.toJson(getSources().filter { it.id != id })).apply()
    }

    suspend fun loadChannels(source: SavedSource): List<Channel> = withContext(Dispatchers.IO) {
        when (source.type) {
            "M3U_URL" -> {
                val url = source.m3uUrl ?: return@withContext emptyList()
                val content = fetchWithRetry(url)
                if (content.isBlank()) return@withContext emptyList()
                M3UParser.parse(content, source.id)
            }
            "M3U_FILE" -> {
                // leer archivo local
                emptyList()
            }
            "XTREAM" -> {
                xtream.getLiveChannels(
                    source.xtreamUrl ?: "", source.xtreamUser ?: "",
                    source.xtreamPass ?: "", source.id
                )
            }
            else -> emptyList()
        }
    }

    suspend fun loadMovies(source: SavedSource): List<Movie> = withContext(Dispatchers.IO) {
        if (source.type != "XTREAM") return@withContext emptyList()
        xtream.getMovies(source.xtreamUrl ?: "", source.xtreamUser ?: "", source.xtreamPass ?: "", source.id)
    }

    suspend fun loadSeries(source: SavedSource): List<Series> = withContext(Dispatchers.IO) {
        if (source.type != "XTREAM") return@withContext emptyList()
        xtream.getSeries(source.xtreamUrl ?: "", source.xtreamUser ?: "", source.xtreamPass ?: "", source.id)
    }

    private fun fetchWithRetry(url: String, retries: Int = 3): String {
        var lastError: Exception? = null
        repeat(retries) { attempt ->
            try {
                val req = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "StreamVault/1.0")
                    .build()
                val body = client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
                    resp.body?.string() ?: ""
                }
                if (body.isNotBlank()) return body
            } catch (e: Exception) {
                lastError = e
                if (attempt < retries - 1) Thread.sleep(2000)
            }
        }
        throw lastError ?: Exception("No se pudo descargar la lista")
    }
}
