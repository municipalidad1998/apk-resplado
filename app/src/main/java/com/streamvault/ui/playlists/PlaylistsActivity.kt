package com.streamvault.ui.playlists

import android.content.Intent
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
                            openPlaylist(playlist)
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

    /** Abre la playlist y reproduce sus pistas (solo fuente LOCAL). */
    private fun openPlaylist(playlist: PlaylistEntity) {
        lifecycleScope.launch {
            dao.tracks(playlist.id).collectLatest { tracks ->
                if (playlist.source != "LOCAL") {
                    Toast.makeText(this@PlaylistsActivity,
                        "Playlist ${playlist.name}: ${tracks.size} elementos (${playlist.source})",
                        Toast.LENGTH_SHORT).show()
                    return@collectLatest
                }
                if (tracks.isEmpty()) {
                    Toast.makeText(this@PlaylistsActivity, "Playlist vacía", Toast.LENGTH_SHORT).show()
                    return@collectLatest
                }
                val songs = tracks.map { t ->
                    com.streamvault.data.local.LocalSong(
                        id = 0, title = t.title, artist = t.artist ?: "Desconocido", album = "",
                        duration = t.duration, contentUri = t.uri, albumArtUri = t.artUri
                    )
                }
                startActivity(
                    Intent(this@PlaylistsActivity, com.streamvault.ui.music.PlayerMusicActivity::class.java).apply {
                        putParcelableArrayListExtra(
                            com.streamvault.ui.music.PlayerMusicActivity.EXTRA_QUEUE, ArrayList(songs)
                        )
                        putExtra(com.streamvault.ui.music.PlayerMusicActivity.EXTRA_INDEX, 0)
                    }
                )
                return@collectLatest
            }
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
