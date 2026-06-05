package com.streamvault.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.streamvault.data.model.*
import com.streamvault.data.repository.MainRepository
import com.streamvault.util.ChannelCache
import kotlinx.coroutines.*

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("streamvault", Context.MODE_PRIVATE)
    val repo = MainRepository(prefs)

    val channels    = MutableLiveData<List<Channel>>(emptyList())
    val movies      = MutableLiveData<List<Movie>>(emptyList())
    val series      = MutableLiveData<List<Series>>(emptyList())
    val sources     = MutableLiveData<List<SavedSource>>(emptyList())
    val categories  = MutableLiveData<List<String>>(emptyList())
    val loading     = MutableLiveData(false)
    val loadingText = MutableLiveData("")
    val error       = MutableLiveData<String?>()
    val toast       = MutableLiveData<String?>()

    private var allChannels = listOf<Channel>()
    private var allMovies   = listOf<Movie>()
    private var allSeries   = listOf<Series>()
    private var currentCategory: String? = null

    fun refresh(forceReload: Boolean = false) {
        val srcs = repo.getSources()
        sources.value = srcs
        if (srcs.isEmpty()) { reset(); return }
        if (!forceReload && ChannelCache.isLoaded()) {
            allChannels = ChannelCache.get()
            channels.value = applyFilter(allChannels)
            buildCategories(allChannels)
            return
        }
        loadAll(srcs)
    }

    fun deleteSource(id: String) {
        repo.deleteSource(id)
        ChannelCache.clear()
        refresh(forceReload = true)
    }

    fun filterByCategory(cat: String?) {
        currentCategory = cat
        channels.value = applyFilter(allChannels)
    }

    private fun applyFilter(list: List<Channel>): List<Channel> =
        if (currentCategory == null) list
        else list.filter { it.group.equals(currentCategory, ignoreCase = true) }

    private fun reset() {
        allChannels = emptyList(); allMovies = emptyList(); allSeries = emptyList()
        channels.value = emptyList(); movies.value = emptyList(); series.value = emptyList()
        categories.value = emptyList(); ChannelCache.clear()
    }

    private fun buildCategories(list: List<Channel>) {
        categories.value = list
            .mapNotNull { it.group?.trim() }
            .filter { it.isNotEmpty() }
            .groupBy { it }
            .entries.sortedByDescending { it.value.size }
            .map { it.key }
    }

    private fun loadAll(srcs: List<SavedSource>) {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            val chList = mutableListOf<Channel>()
            val mvList = mutableListOf<Movie>()
            val srList = mutableListOf<Series>()

            srcs.forEachIndexed { idx, src ->
                val label = if (srcs.size > 1) " ${idx+1}/${srcs.size}" else ""
                loadingText.value = "Descargando${label}: ${src.name}..."
                try {
                    val result = withContext(Dispatchers.IO) { repo.loadAll(src) }
                    chList.addAll(result.channels)
                    mvList.addAll(result.movies)
                    srList.addAll(result.series)

                    // Actualizar UI inmediatamente
                    allChannels = chList.toList()
                    allMovies   = mvList.toList()
                    allSeries   = srList.toList()
                    ChannelCache.set(allChannels)

                    channels.value = applyFilter(allChannels)
                    movies.value   = allMovies
                    series.value   = allSeries
                    buildCategories(allChannels)

                    val total = chList.size + mvList.size + srList.size
                    loadingText.value = "✓ ${chList.size} canales · ${mvList.size} películas · ${srList.size} series"
                } catch (e: Exception) {
                    error.value = when {
                        e.message?.contains("abort", true) == true -> "Conexión interrumpida. Desliza para reintentar."
                        e.message?.contains("timeout", true) == true -> "Tiempo agotado."
                        else -> "Error: ${e.message?.take(60)}"
                    }
                }
            }
            loading.value = false
            if (allChannels.isEmpty() && allMovies.isEmpty()) {
                loadingText.value = ""
                error.value = "Sin contenido. Verifica tu URL o conexión."
            }
        }
    }
}
