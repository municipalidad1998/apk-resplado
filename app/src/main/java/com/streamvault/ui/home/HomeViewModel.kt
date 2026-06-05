package com.streamvault.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.streamvault.data.model.*
import com.streamvault.data.repository.MainRepository
import kotlinx.coroutines.*

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("streamvault", Context.MODE_PRIVATE)
    val repo = MainRepository(prefs)

    val channels = MutableLiveData<List<Channel>>(emptyList())
    val movies = MutableLiveData<List<Movie>>(emptyList())
    val series = MutableLiveData<List<Series>>(emptyList())
    val sources = MutableLiveData<List<SavedSource>>(emptyList())
    val categories = MutableLiveData<List<String>>(emptyList())
    val selectedCategory = MutableLiveData<String?>(null)
    val loading = MutableLiveData(false)
    val loadingText = MutableLiveData("Cargando...")
    val error = MutableLiveData<String?>()
    val toast = MutableLiveData<String?>()

    private var allChannels = listOf<Channel>()

    fun refresh() {
        val srcs = repo.getSources()
        sources.value = srcs
        if (srcs.isEmpty()) {
            channels.value = emptyList()
            movies.value = emptyList()
            series.value = emptyList()
            categories.value = emptyList()
            return
        }
        loadAll(srcs)
    }

    fun deleteSource(id: String) {
        repo.deleteSource(id)
        toast.value = "Fuente eliminada"
        refresh()
    }

    fun filterByCategory(cat: String?) {
        selectedCategory.value = cat
        channels.value = if (cat == null) allChannels
        else allChannels.filter { (it.group ?: "GENERAL").equals(cat, ignoreCase = true) }
    }

    private fun loadAll(srcs: List<SavedSource>) {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            allChannels = emptyList()
            channels.value = emptyList()

            try {
                val allCh = mutableListOf<Channel>()
                val allMv = mutableListOf<Movie>()
                val allSr = mutableListOf<Series>()

                srcs.forEachIndexed { idx, src ->
                    loadingText.value = "Cargando fuente ${idx+1}/${srcs.size}: ${src.name}..."
                    try {
                        val ch = withContext(Dispatchers.IO) { repo.loadChannels(src) }
                        val mv = withContext(Dispatchers.IO) { repo.loadMovies(src) }
                        val sr = withContext(Dispatchers.IO) { repo.loadSeries(src) }
                        allCh.addAll(ch)
                        allMv.addAll(mv)
                        allSr.addAll(sr)
                        // Mostrar canales inmediatamente al tenerlos
                        if (allCh.isNotEmpty()) {
                            allChannels = allCh.toList()
                            channels.value = allChannels
                            // Construir categorías
                            val cats = allCh
                                .mapNotNull { it.group?.trim() }
                                .filter { it.isNotEmpty() }
                                .distinct()
                                .sorted()
                            categories.value = cats
                        }
                    } catch (e: Exception) {
                        error.value = "Error en '${src.name}': ${e.message}"
                    }
                }
                movies.value = allMv
                series.value = allSr

            } catch (e: Exception) {
                error.value = "Error general: ${e.message}"
            }
            loading.value = false
            loadingText.value = "Cargando..."
        }
    }
}
