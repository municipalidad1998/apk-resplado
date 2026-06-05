package com.streamvault.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.streamvault.data.model.*
import com.streamvault.data.repository.MainRepository
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("streamvault", Context.MODE_PRIVATE)
    val repo = MainRepository(prefs)

    val channels = MutableLiveData<List<Channel>>(emptyList())
    val movies = MutableLiveData<List<Movie>>(emptyList())
    val series = MutableLiveData<List<Series>>(emptyList())
    val sources = MutableLiveData<List<SavedSource>>(emptyList())
    val loading = MutableLiveData(false)
    val error = MutableLiveData<String?>()
    val toast = MutableLiveData<String?>()

    fun refresh() {
        val srcs = repo.getSources()
        sources.value = srcs
        if (srcs.isEmpty()) {
            channels.value = emptyList()
            movies.value = emptyList()
            series.value = emptyList()
            return
        }
        loadAll(srcs)
    }

    fun deleteSource(id: String) {
        repo.deleteSource(id)
        toast.value = "Fuente eliminada"
        refresh()
    }

    private fun loadAll(srcs: List<SavedSource>) {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            try {
                val ch = mutableListOf<Channel>()
                val mv = mutableListOf<Movie>()
                val sr = mutableListOf<Series>()
                for (src in srcs) {
                    try {
                        ch.addAll(repo.loadChannels(src))
                        mv.addAll(repo.loadMovies(src))
                        sr.addAll(repo.loadSeries(src))
                    } catch (e: Exception) {
                        error.value = "Error en '${src.name}': ${e.message}"
                    }
                }
                channels.value = ch
                movies.value = mv
                series.value = sr
                if (ch.isEmpty() && mv.isEmpty() && sr.isEmpty()) {
                    error.value = "No se cargó contenido. Verifica la URL o conexión."
                }
            } catch (e: Exception) {
                error.value = "Error al cargar: ${e.message}"
            }
            loading.value = false
        }
    }
}
