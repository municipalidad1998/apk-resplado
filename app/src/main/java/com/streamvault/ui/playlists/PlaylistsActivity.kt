package com.streamvault.ui.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.streamvault.R
import com.streamvault.StreamVaultApp
import com.streamvault.data.db.PlaylistEntity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 🎶 Playlists — separadas por fuente (LOCAL / YTMUSIC / TELEGRAM).
 * Las playlists locales funcionan sin Internet.
 */
class PlaylistsActivity : AppCompatActivity() {

    private val dao by lazy { StreamVaultApp.db.playlistDao() }

    companion object {
        val SOURCES = listOf("📱 Local" to "LOCAL", "🎵 YouTube Music" to "YTMUSIC", "☁ Telegram" to "TELEGRAM")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlists)
        findViewById<View>(R.id.btnBackPlaylists).setOnClickListener { finish() }

        // Selector de fuente (nunca se mezclan)
        val spSource = findViewById<Spinner>(R.id.spPlaylistSource)
        spSource.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, SOURCES.map { it.first })

        val rv = findViewById<RecyclerView>(R.id.rvPlaylists)
        rv.layoutManager = LinearLayoutManager(this)

        spSource.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val source = SOURCES[pos].second
                lifecycleScope.launch {
                    dao.bySource(source).collectLatest { list ->
                        rv.adapter = PlaylistAdapter(list) { playlist ->
                            Toast.makeText(
                                this@PlaylistsActivity,
                                "Playlist \"${playlist.name}\" (${playlist.source})",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        findViewById<TextView>(R.id.tvEmptyPlaylists).visibility =
                            if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.btnNewPlaylist).setOnClickListener {
            val input = EditText(this).apply { hint = "Nombre de la playlist" }
            AlertDialog.Builder(this)
                .setTitle("Nueva playlist (${SOURCES[spSource.selectedItemPosition].first})")
                .setView(input)
                .setPositiveButton("Crear") { _, _ ->
                    val name = input.text.toString().trim()
                    if (name.isNotEmpty()) {
                        lifecycleScope.launch {
                            dao.insert(PlaylistEntity(name = name, source = SOURCES[spSource.selectedItemPosition].second))
                        }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    class PlaylistAdapter(
        private val items: List<PlaylistEntity>,
        private val onClick: (PlaylistEntity) -> Unit
    ) : RecyclerView.Adapter<PlaylistAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: TextView = v.findViewById(R.id.tvHubIcon)
            val title: TextView = v.findViewById(R.id.tvHubTitle)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_hub, p, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val pl = items[pos]
            h.icon.text = "🎶"
            h.title.text = pl.name
            h.itemView.setOnClickListener { onClick(pl) }
        }
    }
}
