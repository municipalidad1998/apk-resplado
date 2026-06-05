package com.streamvault.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.streamvault.data.model.*
import com.streamvault.databinding.ActivityMainBinding
import com.streamvault.ui.player.PlayerActivity
import com.streamvault.ui.sources.AddSourceActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val vm: HomeViewModel by viewModels()
    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNav()
        setupSearch()
        observeData()
    }

    override fun onResume() {
        super.onResume()
        // Recargar cada vez que volvemos (por si se agregó una fuente)
        vm.refresh()
        updateTabUI()
    }

    private fun setupNav() {
        binding.btnLive.setOnClickListener { currentTab = 0; updateTabUI(); showCurrentTab() }
        binding.btnMovies.setOnClickListener { currentTab = 1; updateTabUI(); showCurrentTab() }
        binding.btnSeries.setOnClickListener { currentTab = 2; updateTabUI(); showCurrentTab() }
        binding.btnSources.setOnClickListener {
            startActivity(Intent(this, AddSourceActivity::class.java))
        }
    }

    private fun setupSearch() {
        binding.fabSearch.setOnClickListener {
            binding.searchBar.visibility =
                if (binding.searchBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        binding.searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) { filterContent(s.toString()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        binding.swipeRefresh.setOnRefreshListener { vm.refresh() }
    }

    private fun observeData() {
        vm.loading.observe(this) {
            binding.swipeRefresh.isRefreshing = it
            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
        vm.channels.observe(this) { if (currentTab == 0) showChannels(it) }
        vm.movies.observe(this) { if (currentTab == 1) showMovies(it) }
        vm.series.observe(this) { if (currentTab == 2) showSeries(it) }
        vm.error.observe(this) { it?.let { msg -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show() } }
        vm.toast.observe(this) { it?.let { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() } }
        vm.sources.observe(this) { srcs ->
            val txt = if (srcs.isEmpty()) "Sin fuentes — toca +" else "${srcs.size} fuente(s)"
            binding.tvSourceCount.text = txt
        }
    }

    private fun updateTabUI() {
        binding.btnLive.isSelected = currentTab == 0
        binding.btnMovies.isSelected = currentTab == 1
        binding.btnSeries.isSelected = currentTab == 2
    }

    private fun showCurrentTab() {
        when (currentTab) {
            0 -> vm.channels.value?.let { showChannels(it) }
            1 -> vm.movies.value?.let { showMovies(it) }
            2 -> vm.series.value?.let { showSeries(it) }
        }
    }

    private fun showChannels(list: List<Channel>) {
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = ChannelAdapter(list) { ch ->
            startActivity(Intent(this, PlayerActivity::class.java).apply { putExtra("channel", ch) })
        }
        setEmpty(list.isEmpty(), "Sin canales en vivo.\nVerifica tu lista M3U o Xtream.")
    }

    private fun showMovies(list: List<Movie>) {
        binding.recycler.layoutManager = GridLayoutManager(this, 3)
        binding.recycler.adapter = MovieAdapter(list) { m ->
            val ch = Channel(id = m.id, name = m.name, url = m.streamUrl, logo = m.cover)
            startActivity(Intent(this, PlayerActivity::class.java).apply { putExtra("channel", ch) })
        }
        setEmpty(list.isEmpty(), "Sin peliculas.\nRequiere fuente Xtream Codes.")
    }

    private fun showSeries(list: List<Series>) {
        binding.recycler.layoutManager = GridLayoutManager(this, 3)
        binding.recycler.adapter = SeriesAdapter(list)
        setEmpty(list.isEmpty(), "Sin series.\nRequiere fuente Xtream Codes.")
    }

    private fun setEmpty(isEmpty: Boolean, msg: String) {
        binding.tvEmpty.text = msg
        binding.tvEmpty.visibility = if (isEmpty && vm.loading.value != true) View.VISIBLE else View.GONE
    }

    private fun filterContent(q: String) {
        when (currentTab) {
            0 -> showChannels((vm.channels.value ?: emptyList()).filter { it.name.contains(q, true) })
            1 -> showMovies((vm.movies.value ?: emptyList()).filter { it.name.contains(q, true) })
            2 -> showSeries((vm.series.value ?: emptyList()).filter { it.name.contains(q, true) })
        }
    }
}
