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
    val channels = MutableLiveData<List<Channel>>()
    val movies = MutableLiveData<List<Movie>>()
    val series = MutableLiveData<List<Series>>()
    val sources = MutableLiveData<List<SavedSource>>()
    val loading = MutableLiveData<Boolean>()
    val error = MutableLiveData<String?>()
    fun loadSources() { sources.value = repo.getSources() }
    fun loadAll() {
        viewModelScope.launch {
            loading.value = true; error.value = null
            try {
                val srcs = repo.getSources()
                val ch = mutableListOf<Channel>(); val mv = mutableListOf<Movie>(); val sr = mutableListOf<Series>()
                srcs.forEach { s -> ch.addAll(repo.loadChannels(s)); mv.addAll(repo.loadMovies(s)); sr.addAll(repo.loadSeries(s)) }
                channels.value = ch; movies.value = mv; series.value = sr
            } catch (e: Exception) { error.value = e.message }
            loading.value = false
        }
    }
}
