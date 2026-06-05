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
    val loading = MutableLiveData(false)
    val loadingProgress = MutableLiveData(0) // 0-100
    val loadingText = MutableLiveData("")
    val error = MutableLiveData<String?>()
    val toast = MutableLiveData<String?>()

    private var allChannels = listOf<Channel>()
    private var currentCategory: String? = null

    fun refresh() {
        val srcs = repo.getSources()
        sources.value = srcs
        if (srcs.isEmpty()) {
            reset(); return
        }
        loadAll(srcs)
    }

    fun deleteSource(id: String) {
        repo.deleteSource(id)
        toast.value = "Fuente eliminada"
        refresh()
    }

    fun filterByCategory(cat: String?) {
        currentCategory = cat
        channels.value = if (cat == null) allChannels
        else allChannels.filter { (it.group ?: "").equals(cat, ignoreCase = true) }
    }

    private fun reset() {
        allChannels = emptyList()
        channels.value = emptyList()
        movies.value = emptyList()
        series.value = emptyList()
        categories.value = emptyList()
    }

    private fun loadAll(srcs: List<SavedSource>) {
        viewModelScope.launch {
            loading.value = true
            loadingProgress.value = 0
            error.value = null
            reset()

            val total = srcs.size
            srcs.forEachIndexed { idx, src ->
                loadingText.value = "Descargando lista${if (total > 1) " ${idx+1}/$total" else ""}..."
                try {
                    // Cargar canales primero (más importante)
                    val ch = withContext(Dispatchers.IO) { repo.loadChannels(src) }
                    if (ch.isNotEmpty()) {
                        allChannels = allChannels + ch
                        // Aplicar filtro actual si hay uno
                        channels.value = if (currentCategory == null) allChannels
                            else allChannels.filter { (it.group ?: "").equals(currentCategory, ignoreCase = true) }
                        // Actualizar categorías
                        val cats = allChannels
                            .mapNotNull { it.group?.trim() }
                            .filter { it.isNotEmpty() }
                            .distinct()
                            .sorted()
                        categories.value = cats
                        loadingText.value = "${allChannels.size} canales cargados"
                    }
                    loadingProgress.value = ((idx + 1) * 70 / total)

                    // Cargar películas y series en paralelo
                    val mvJob = async(Dispatchers.IO) { repo.loadMovies(src) }
                    val srJob = async(Dispatchers.IO) { repo.loadSeries(src) }
                    val mv = mvJob.await()
                    val sr = srJob.await()
                    if (mv.isNotEmpty()) movies.value = (movies.value ?: emptyList()) + mv
                    if (sr.isNotEmpty()) series.value = (series.value ?: emptyList()) + sr
                    loadingProgress.value = ((idx + 1) * 100 / total)

                } catch (e: Exception) {
                    val msg = when {
                        e.message?.contains("abort") == true -> "Conexión interrumpida. Reintentando..."
                        e.message?.contains("timeout") == true -> "Tiempo agotado descargando lista"
                        else -> "Error: ${e.message}"
                    }
                    error.value = msg
                }
            }
            loading.value = false
            loadingText.value = ""
            if (allChannels.isEmpty()) {
                error.value = "No se cargaron canales. Desliza hacia abajo para reintentar."
            }
        }
    }
}
