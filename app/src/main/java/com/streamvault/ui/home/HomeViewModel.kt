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

    private var allChannels   = listOf<Channel>()
    private var currentCategory: String? = null

    fun refresh(forceReload: Boolean = false) {
        val srcs = repo.getSources()
        sources.value = srcs
        if (srcs.isEmpty()) { reset(); return }

        // Si ya tenemos datos en cache y no se fuerza recarga, usar cache
        if (!forceReload && ChannelCache.isLoaded()) {
            allChannels = ChannelCache.get()
            channels.value = allChannels
            buildCategories(allChannels)
            return
        }
        loadAll(srcs)
    }

    fun deleteSource(id: String) {
        repo.deleteSource(id)
        ChannelCache.clear()
        toast.value = "Fuente eliminada"
        refresh(forceReload = true)
    }

    fun filterByCategory(cat: String?) {
        currentCategory = cat
        channels.value = if (cat == null) allChannels
        else allChannels.filter { it.group.equals(cat, ignoreCase = true) }
    }

    private fun reset() {
        allChannels = emptyList()
        channels.value = emptyList()
        movies.value = emptyList()
        series.value = emptyList()
        categories.value = emptyList()
        ChannelCache.clear()
    }

    private fun buildCategories(list: List<Channel>) {
        val cats = list
            .mapNotNull { it.group?.trim() }
            .filter { it.isNotEmpty() }
            .groupBy { it }
            .entries
            .sortedByDescending { it.value.size } // más canales primero
            .map { it.key }
        categories.value = cats
    }

    private fun loadAll(srcs: List<SavedSource>) {
        viewModelScope.launch {
            loading.value = true
            loadingText.value = "Descargando lista..."
            error.value = null
            allChannels = emptyList()
            channels.value = emptyList()

            val allCh = mutableListOf<Channel>()
            val allMv = mutableListOf<Movie>()
            val allSr = mutableListOf<Series>()

            srcs.forEachIndexed { idx, src ->
                val label = if (srcs.size > 1) "${idx+1}/${srcs.size}: " else ""
                loadingText.value = "Cargando ${label}${src.name}..."
                try {
                    val ch = withContext(Dispatchers.IO) { repo.loadChannels(src) }
                    allCh.addAll(ch)
                    // Mostrar inmediatamente
                    if (allCh.isNotEmpty()) {
                        allChannels = allCh.toList()
                        ChannelCache.set(allChannels) // guardar en cache
                        val filtered = if (currentCategory == null) allChannels
                            else allChannels.filter { it.group.equals(currentCategory, true) }
                        channels.value = filtered
                        buildCategories(allChannels)
                        loadingText.value = "${allChannels.size} canales"
                    }
                    // Películas/Series solo de Xtream
                    if (src.type == "XTREAM") {
                        val mv = withContext(Dispatchers.IO) { repo.loadMovies(src) }
                        val sr = withContext(Dispatchers.IO) { repo.loadSeries(src) }
                        allMv.addAll(mv)
                        allSr.addAll(sr)
                    }
                } catch (e: Exception) {
                    val msg = when {
                        e.message?.contains("abort",true) == true ||
                        e.message?.contains("reset",true) == true ->
                            "Conexión interrumpida. Desliza para reintentar."
                        e.message?.contains("timeout",true) == true ->
                            "Tiempo agotado. Verifica tu conexión."
                        else -> "Error: ${e.message?.take(80)}"
                    }
                    error.value = msg
                }
            }

            movies.value = allMv
            series.value = allSr
            loading.value = false
            loadingText.value = ""

            if (allChannels.isEmpty()) {
                error.value = "No se cargaron canales.\nDesliza hacia abajo para reintentar."
            }
        }
    }
}
