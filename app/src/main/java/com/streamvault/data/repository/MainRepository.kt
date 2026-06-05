package com.streamvault.data.repository

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.streamvault.data.model.*
import com.streamvault.util.M3UParser
import com.streamvault.util.ParsedContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class MainRepository(private val prefs: SharedPreferences) {
    private val gson = Gson()
    private val xtream = XtreamRepository()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun getSources(): List<SavedSource> {
        return try {
            val json = prefs.getString("sources", "[]") ?: "[]"
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

    // Carga todo separado (canales, películas, series) desde M3U
    suspend fun loadAll(source: SavedSource): ParsedContent = withContext(Dispatchers.IO) {
        when (source.type) {
            "M3U_URL" -> {
                val url = source.m3uUrl ?: return@withContext ParsedContent(emptyList(), emptyList(), emptyList())
                val content = fetchWithRetry(url)
                if (content.isBlank()) return@withContext ParsedContent(emptyList(), emptyList(), emptyList())
                M3UParser.parseAll(content, source.id)
            }
            "XTREAM" -> {
                val base = source.xtreamUrl ?: ""
                val user = source.xtreamUser ?: ""
                val pass = source.xtreamPass ?: ""
                val ch = xtream.getLiveChannels(base, user, pass, source.id)
                val mv = xtream.getMovies(base, user, pass, source.id)
                val sr = xtream.getSeries(base, user, pass, source.id)
                ParsedContent(ch, mv, sr)
            }
            else -> ParsedContent(emptyList(), emptyList(), emptyList())
        }
    }

    // Mantener métodos separados para compatibilidad con PlayerActivity
    suspend fun loadChannels(source: SavedSource): List<Channel> = loadAll(source).channels

    private fun fetchWithRetry(url: String, retries: Int = 3): String {
        var lastError: Exception? = null
        repeat(retries) { attempt ->
            try {
                val req = Request.Builder().url(url)
                    .addHeader("User-Agent", "StreamVault/1.0").build()
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
        throw lastError ?: Exception("No se pudo descargar")
    }
}
