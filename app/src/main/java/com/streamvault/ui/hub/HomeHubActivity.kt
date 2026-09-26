package com.streamvault.ui.hub

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.streamvault.R
import com.streamvault.StreamVaultApp
import com.streamvault.playback.MusicPlayerService
import com.streamvault.playback.PlaybackStateHolder
import com.streamvault.ui.home.MainActivity
import com.streamvault.ui.music.LocalMusicActivity
import com.streamvault.ui.music.PlayerMusicActivity
import com.streamvault.ui.settings.SettingsActivity
import com.streamvault.ui.telegram.TelegramCloudActivity
import com.streamvault.ui.telegram.TelegramConfigActivity
import com.streamvault.ui.web.WebHubActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 🏠 Home premium de "Reproductor de Música Denilson".
 * - Saludo según la hora + buscador + "Escuchado recientemente".
 * - Accesos: Música local, YouTube, YouTube Music, Telegram, IPTV…
 * - Barra inferior: ⌂ Inicio | 🔎 Buscar | 📚 Biblioteca | 🎵 Playlists | ▶ YouTube
 * - Mini reproductor sobre la barra de navegación.
 * Todas las fuentes siguen siendo INDEPENDIENTES.
 */
class HomeHubActivity : AppCompatActivity() {

    data class HubItem(val icon: String, val title: String, val action: () -> Unit)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_hub)

        // Saludo según la hora
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        findViewById<TextView>(R.id.tvGreeting).text = when (hour) {
            in 5..11 -> "Buenos días ☀️"
            in 12..18 -> "Buenas tardes 🌤"
            else -> "Buenas noches 🌙"
        }

        setupHomeContent()
        setupBottomNav()
        setupMiniPlayer()

        // Indicador de red 🟢🟡🔴
        lifecycleScope.launch {
            com.streamvault.network.NetworkMonitor.status.collectLatest { st ->
                findViewById<TextView>(R.id.tvNetStatus).text = when (st) {
                    com.streamvault.network.NetworkMonitor.Status.WIFI -> "🟢 Wi-Fi"
                    com.streamvault.network.NetworkMonitor.Status.ONLINE -> "🟢 En línea"
                    com.streamvault.network.NetworkMonitor.Status.SLOW -> "🟡 Conexión lenta"
                    com.streamvault.network.NetworkMonitor.Status.OFFLINE -> "🔴 Sin conexión"
                }
            }
        }
    }

    private fun setupHomeContent() {
        findViewById<View>(R.id.btnSearchEntry).setOnClickListener {
            startActivity(Intent(this, LocalMusicActivity::class.java))
        }

        // Secciones (fuentes independientes, nunca mezcladas)
        val items = listOf(
            HubItem("📁", "Mi Música\n(local)") {
                startActivity(Intent(this, LocalMusicActivity::class.java))
            },
            HubItem("🎵", "YouTube\nMusic") {
                startActivity(Intent(this, WebHubActivity::class.java).apply {
                    putExtra(WebHubActivity.EXTRA_URL, WebHubActivity.URL_YOUTUBE_MUSIC)
                    putExtra(WebHubActivity.EXTRA_TITLE, "YouTube Music")
                })
            },
            HubItem("▶️", "YouTube") {
                startActivity(Intent(this, WebHubActivity::class.java).apply {
                    putExtra(WebHubActivity.EXTRA_URL, WebHubActivity.URL_YOUTUBE)
                    putExtra(WebHubActivity.EXTRA_TITLE, "YouTube")
                })
            },
            HubItem("❤️", "Tus\nfavoritos") {
                startActivity(Intent(this, LocalMusicActivity::class.java).apply {
                    putExtra(LocalMusicActivity.EXTRA_MODE, LocalMusicActivity.MODE_FAVORITES)
                })
            },
            HubItem("🎶", "Tus\nplaylists") {
                startActivity(Intent(this, com.streamvault.ui.playlists.PlaylistsActivity::class.java))
            },
            HubItem("☁", "Telegram\nCloud") {
                val configured = getSharedPreferences("telegram_prefs", MODE_PRIVATE)
                    .getString("api_id", "")?.isNotBlank() == true
                startActivity(
                    Intent(
                        this,
                        if (configured) TelegramCloudActivity::class.java else TelegramConfigActivity::class.java
                    )
                )
            },
            HubItem("📺", "TV en vivo\n(IPTV)") {
                startActivity(Intent(this, MainActivity::class.java))
            },
            HubItem("🎬", "Videos\nlocales") {
                startActivity(Intent(this, LocalMusicActivity::class.java).apply {
                    putExtra(LocalMusicActivity.EXTRA_MODE, LocalMusicActivity.MODE_VIDEOS)
                })
            },
            HubItem("🖼", "Fotos") {
                startActivity(Intent(this, LocalMusicActivity::class.java).apply {
                    putExtra(LocalMusicActivity.EXTRA_MODE, LocalMusicActivity.MODE_PHOTOS)
                })
            },
            HubItem("📄", "Documentos") {
                startActivity(Intent(this, LocalMusicActivity::class.java).apply {
                    putExtra(LocalMusicActivity.EXTRA_MODE, LocalMusicActivity.MODE_DOCS)
                })
            },
            HubItem("🕒", "Historial") {
                startActivity(Intent(this, LocalMusicActivity::class.java).apply {
                    putExtra(LocalMusicActivity.EXTRA_MODE, LocalMusicActivity.MODE_HISTORY)
                })
            },
            HubItem("⚙", "Ajustes") {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        )
        val rv = findViewById<RecyclerView>(R.id.rvHub)
        rv.layoutManager = GridLayoutManager(this, 3)
        rv.adapter = HubAdapter(items)

        // Escuchado recientemente (historial local)
        val rvRecent = findViewById<RecyclerView>(R.id.rvRecent)
        rvRecent.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        lifecycleScope.launch {
            StreamVaultApp.db.historyDao().recent().collectLatest { hist ->
                val local = hist.filter { it.source == "LOCAL" }.distinctBy { it.uri }.take(20)
                findViewById<TextView>(R.id.tvRecentHeader).visibility =
                    if (local.isEmpty()) View.GONE else View.VISIBLE
                rvRecent.adapter = RecentAdapter(local.map {
                    com.streamvault.data.local.LocalSong(
                        id = 0, title = it.title, artist = it.artist ?: "Desconocido",
                        album = "", duration = it.durationMs, contentUri = it.uri, albumArtUri = it.artUri
                    )
                })
            }
        }
    }

    private fun setupBottomNav() {
        val tabs = mapOf(
            R.id.tabHome to {},
            R.id.tabSearch to {
                startActivity(Intent(this, LocalMusicActivity::class.java))
            },
            R.id.tabLibrary to {
                startActivity(Intent(this, LocalMusicActivity::class.java))
            },
            R.id.tabPlaylists to {
                startActivity(Intent(this, com.streamvault.ui.playlists.PlaylistsActivity::class.java))
            },
            R.id.tabYouTube to {
                startActivity(Intent(this, WebHubActivity::class.java).apply {
                    putExtra(WebHubActivity.EXTRA_URL, WebHubActivity.URL_YOUTUBE)
                    putExtra(WebHubActivity.EXTRA_TITLE, "YouTube")
                })
            }
        )
        tabs.forEach { (id, action) ->
            findViewById<TextView>(id).setOnClickListener { v ->
                tabs.keys.forEach { findViewById<TextView>(it).setTextColor(getColor(R.color.den_text_muted)) }
                (v as TextView).setTextColor(getColor(R.color.den_accent))
                v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(120)
                    .withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(120).start() }
                    .start()
                action()
            }
        }
        findViewById<TextView>(R.id.tabHome).setTextColor(getColor(R.color.den_accent))
    }

    // ---------------- 🎵 Mini reproductor ----------------

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private fun setupMiniPlayer() {
        findViewById<View>(R.id.miniPlayer).setOnClickListener {
            startActivity(Intent(this, PlayerMusicActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnMiniPlay).setOnClickListener {
            controller?.let { if (it.isPlaying) it.pause() else it.play() }
        }
    }

    private val miniListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            findViewById<ImageButton>(R.id.btnMiniPlay).setImageResource(
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
        }
        override fun onMediaItemTransition(item: androidx.media3.common.MediaItem?, reason: Int) {
            refreshMini()
        }
    }

    private fun refreshMini() {
        val s = PlaybackStateHolder.queue.getOrNull(controller?.currentMediaItemIndex ?: -1)
        val has = (controller?.mediaItemCount ?: 0) > 0 && s != null
        findViewById<View>(R.id.miniPlayer).visibility = if (has) View.VISIBLE else View.GONE
        s ?: return
        findViewById<TextView>(R.id.tvMiniTitle).text = s.title
        findViewById<TextView>(R.id.tvMiniArtist).text = s.artist
        Glide.with(this).load(s.albumArtUri)
            .placeholder(R.drawable.ic_channel).error(R.drawable.ic_channel)
            .into(findViewById(R.id.ivMiniArt))
        miniListener.onIsPlayingChanged(controller?.isPlaying ?: false)
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, MusicPlayerService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(miniListener)
            refreshMini()
        }, MoreExecutors.directExecutor())
    }

    override fun onStop() {
        controller?.removeListener(miniListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        super.onStop()
    }

    // ---------------- Adapters ----------------

    class HubAdapter(private val items: List<HubItem>) : RecyclerView.Adapter<HubAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: TextView = v.findViewById(R.id.tvHubIcon)
            val title: TextView = v.findViewById(R.id.tvHubTitle)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_hub, p, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = items[pos]
            h.icon.text = item.icon
            h.title.text = item.title
            h.itemView.setOnClickListener {
                it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(90)
                    .withEndAction { it.animate().scaleX(1f).scaleY(1f).setDuration(90).start() }
                    .start()
                item.action()
            }
        }
    }

    class RecentAdapter(private val songs: List<com.streamvault.data.local.LocalSong>) :
        RecyclerView.Adapter<RecentAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val art: ImageView = v.findViewById(R.id.ivRecentArt)
            val title: TextView = v.findViewById(R.id.tvRecentTitle)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_recent, p, false))

        override fun getItemCount() = songs.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val s = songs[pos]
            h.title.text = s.title
            Glide.with(h.art).load(s.albumArtUri)
                .placeholder(R.drawable.ic_channel).error(R.drawable.ic_channel).into(h.art)
            h.itemView.setOnClickListener {
                it.context.startActivity(Intent(it.context, PlayerMusicActivity::class.java).apply {
                    putParcelableArrayListExtra(PlayerMusicActivity.EXTRA_QUEUE, ArrayList(songs))
                    putExtra(PlayerMusicActivity.EXTRA_INDEX, pos)
                })
            }
        }
    }
}
