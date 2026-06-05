package com.streamvault.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
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
        setupCategories()
        observeData()
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
        updateTabUI()
    }

    private fun setupNav() {
        binding.btnLive.setOnClickListener {
            currentTab = 0; updateTabUI()
            vm.filterByCategory(null)
            showCategoriesBar(true)
        }
        binding.btnMovies.setOnClickListener {
            currentTab = 1; updateTabUI()
            showCategoriesBar(false)
            vm.movies.value?.let { showMovies(it) }
        }
        binding.btnSeries.setOnClickListener {
            currentTab = 2; updateTabUI()
            showCategoriesBar(false)
            vm.series.value?.let { showSeries(it) }
        }
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

    private fun setupCategories() {
        binding.rvCategories.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun observeData() {
        vm.loading.observe(this) { loading ->
            binding.swipeRefresh.isRefreshing = loading
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.tvLoadingText.visibility = if (loading) View.VISIBLE else View.GONE
        }
        vm.loadingText.observe(this) { binding.tvLoadingText.text = it }
        vm.channels.observe(this) { if (currentTab == 0) showChannels(it) }
        vm.movies.observe(this) { if (currentTab == 1) showMovies(it) }
        vm.series.observe(this) { if (currentTab == 2) showSeries(it) }
        vm.categories.observe(this) { cats -> setupCategoryChips(cats) }
        vm.error.observe(this) { it?.let { msg -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show() } }
        vm.toast.observe(this) { it?.let { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() } }
        vm.sources.observe(this) { srcs ->
            binding.tvSourceCount.text = if (srcs.isEmpty()) "Sin fuentes" else "${srcs.size} fuente(s)"
        }
    }

    private fun setupCategoryChips(cats: List<String>) {
        val all = listOf("TODOS") + cats
        binding.rvCategories.adapter = CategoryAdapter(all, "TODOS") { cat ->
            if (cat == "TODOS") vm.filterByCategory(null)
            else vm.filterByCategory(cat)
        }
    }

    private fun showCategoriesBar(show: Boolean) {
        binding.categoriesBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun updateTabUI() {
        binding.btnLive.isSelected = currentTab == 0
        binding.btnMovies.isSelected = currentTab == 1
        binding.btnSeries.isSelected = currentTab == 2
    }

    private fun showChannels(list: List<Channel>) {
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = ChannelAdapter(list) { ch ->
            startActivity(Intent(this, PlayerActivity::class.java).apply { putExtra("channel", ch) })
        }
        setEmpty(list.isEmpty(), "Sin canales.\nVerifica tu URL o conexion.")
    }

    private fun showMovies(list: List<Movie>) {
        binding.recycler.layoutManager = GridLayoutManager(this, 3)
        binding.recycler.adapter = MovieAdapter(list) { m ->
            val ch = Channel(id = m.id, name = m.name, url = m.streamUrl, logo = m.cover)
            startActivity(Intent(this, PlayerActivity::class.java).apply { putExtra("channel", ch) })
        }
        setEmpty(list.isEmpty(), "Sin peliculas.\nRequiere Xtream Codes.")
    }

    private fun showSeries(list: List<Series>) {
        binding.recycler.layoutManager = GridLayoutManager(this, 3)
        binding.recycler.adapter = SeriesAdapter(list)
        setEmpty(list.isEmpty(), "Sin series.\nRequiere Xtream Codes.")
    }

    private fun setEmpty(isEmpty: Boolean, msg: String) {
        binding.tvEmpty.text = msg
        binding.tvEmpty.visibility =
            if (isEmpty && vm.loading.value != true) View.VISIBLE else View.GONE
    }

    private fun filterContent(q: String) {
        when (currentTab) {
            0 -> showChannels((vm.channels.value ?: emptyList()).filter { it.name.contains(q, true) })
            1 -> showMovies((vm.movies.value ?: emptyList()).filter { it.name.contains(q, true) })
            2 -> showSeries((vm.series.value ?: emptyList()).filter { it.name.contains(q, true) })
        }
    }
}

// Adapter para categorías horizontales
class CategoryAdapter(
    private val cats: List<String>,
    private var selected: String,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    inner class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

    override fun onCreateViewHolder(p: android.view.ViewGroup, t: Int): VH {
        val tv = TextView(p.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.MATCH_PARENT
            ).also { it.marginEnd = 8; it.marginStart = 4 }
            setPadding(20, 8, 20, 8)
            textSize = 12f
            maxLines = 1
        }
        return VH(tv)
    }

    override fun getItemCount() = cats.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val cat = cats[pos]
        h.tv.text = cat
        val isSelected = cat == selected
        h.tv.setTextColor(if (isSelected) 0xFFFFFFFF.toInt() else 0xFF8888AA.toInt())
        h.tv.setBackgroundResource(if (isSelected)
            com.streamvault.R.drawable.chip_selected
        else
            com.streamvault.R.drawable.chip_normal)
        h.tv.setOnClickListener {
            selected = cat
            notifyDataSetChanged()
            onClick(cat)
        }
    }
}
