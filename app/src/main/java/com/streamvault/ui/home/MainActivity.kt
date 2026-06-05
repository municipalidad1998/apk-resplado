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
        setupNav(); setupSearch(); observeData(); vm.loadSources(); vm.loadAll(); updateTab()
    }
    override fun onResume() { super.onResume(); vm.loadSources() }
    private fun setupNav() {
        binding.btnLive.setOnClickListener { currentTab=0; updateTab() }
        binding.btnMovies.setOnClickListener { currentTab=1; updateTab() }
        binding.btnSeries.setOnClickListener { currentTab=2; updateTab() }
        binding.btnSources.setOnClickListener { startActivity(Intent(this, AddSourceActivity::class.java)) }
    }
    private fun setupSearch() {
        binding.fabSearch.setOnClickListener {
            binding.searchBar.visibility = if (binding.searchBar.visibility==View.VISIBLE) View.GONE else View.VISIBLE
        }
        binding.searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) { filterContent(s.toString()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        binding.swipeRefresh.setOnRefreshListener { vm.loadAll() }
    }
    private fun observeData() {
        vm.loading.observe(this) { binding.swipeRefresh.isRefreshing=it; binding.progressBar.visibility=if(it) View.VISIBLE else View.GONE }
        vm.channels.observe(this) { if (currentTab==0) showChannels(it) }
        vm.movies.observe(this) { if (currentTab==1) showMovies(it) }
        vm.series.observe(this) { if (currentTab==2) showSeries(it) }
        vm.error.observe(this) { it?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() } }
        vm.sources.observe(this) { binding.tvSourceCount.text = "${it.size} fuente(s)" }
    }
    private fun updateTab() {
        binding.btnLive.isSelected=currentTab==0; binding.btnMovies.isSelected=currentTab==1; binding.btnSeries.isSelected=currentTab==2
        when(currentTab) { 0->vm.channels.value?.let{showChannels(it)}; 1->vm.movies.value?.let{showMovies(it)}; 2->vm.series.value?.let{showSeries(it)} }
    }
    private fun showChannels(list: List<Channel>) {
        binding.recycler.layoutManager=LinearLayoutManager(this)
        binding.recycler.adapter=ChannelAdapter(list) { ch -> startActivity(Intent(this,PlayerActivity::class.java).apply{putExtra("channel",ch)}) }
        binding.tvEmpty.visibility=if(list.isEmpty()) View.VISIBLE else View.GONE
    }
    private fun showMovies(list: List<Movie>) {
        binding.recycler.layoutManager=GridLayoutManager(this,3)
        binding.recycler.adapter=MovieAdapter(list) { m -> val ch=Channel(id=m.id,name=m.name,url=m.streamUrl,logo=m.cover); startActivity(Intent(this,PlayerActivity::class.java).apply{putExtra("channel",ch)}) }
        binding.tvEmpty.visibility=if(list.isEmpty()) View.VISIBLE else View.GONE
    }
    private fun showSeries(list: List<Series>) {
        binding.recycler.layoutManager=GridLayoutManager(this,3)
        binding.recycler.adapter=SeriesAdapter(list)
        binding.tvEmpty.visibility=if(list.isEmpty()) View.VISIBLE else View.GONE
    }
    private fun filterContent(q: String) {
        when(currentTab) {
            0->showChannels((vm.channels.value?:emptyList()).filter{it.name.contains(q,true)})
            1->showMovies((vm.movies.value?:emptyList()).filter{it.name.contains(q,true)})
            2->showSeries((vm.series.value?:emptyList()).filter{it.name.contains(q,true)})
        }
    }
}
