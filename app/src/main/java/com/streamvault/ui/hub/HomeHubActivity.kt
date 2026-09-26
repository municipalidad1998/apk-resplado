package com.streamvault.ui.hub

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.streamvault.R
import com.streamvault.ui.home.MainActivity
import android.content.ComponentName
import android.widget.ImageButton
import android.widget.ImageView
import com.bumptech.glide.Glide
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.streamvault.playback.MusicPlayerService
import com.streamvault.playback.PlaybackStateHolder
import com.streamvault.ui.music.LocalMusicActivity
import com.streamvault.ui.settings.SettingsActivity
import com.streamvault.ui.telegram.TelegramConfigActivity
import com.streamvault.ui.web.WebHubActivity

/**
 * Menú principal del centro multimedia.
 * Cada sección abre una biblioteca INDEPENDIENTE:
 * 📱 archivos locales | ▶ YouTube Web | 🎵 YouTube Music Web | ☁ Telegram Cloud.
 * Nunca se mezclan las fuentes entre sí.
 */
class HomeHubActivity : AppCompatActivity() {

    data class HubItem(val icon: String, val title: String, val action: () -> Unit)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_hub)

        val items = listOf(
            HubItem("📺", "TV en vivo (IPTV)") {
                startActivity(Intent(this, MainActivity::class.java))
            },
            HubItem("📱", "Música del teléfono") {
                startActivity(Intent(this, LocalMusicActivity::class.java))
            },
            HubItem("▶️", "YouTube") {
                startActivity(Intent(this, WebHubActivity::class.java).apply {
                    putExtra(WebHubActivity.EXTRA_URL, WebHubActivity.URL_YOUTUBE)
                    putExtra(WebHubActivity.EXTRA_TITLE, "YouTube")
                })
            },
            HubItem("🎵", "YouTube Music") {
                startActivity(Intent(this, WebHubActivity::class.java).apply {
                    putExtra(WebHubActivity.EXTRA_URL, WebHubActivity.URL_YOUTUBE_MUSIC)
                    putExtra(WebHubActivity.EXTRA_TITLE, "YouTube Music")
                })
            },
            HubItem("🎬", "Videos") {
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
            HubItem("☁", "Telegram Cloud") {
                val configured = getSharedPreferences("telegram_prefs", MODE_PRIVATE)
                    .getString("api_id", "")?.isNotBlank() == true
                startActivity(
                    Intent(
                        this,
                        if (configured) com.streamvault.ui.telegram.TelegramCloudActivity::class.java
                        else TelegramConfigActivity::class.java
                    )
                )
            },
            HubItem("🎶", "Playlists") {
                startActivity(Intent(this, com.streamvault.ui.playlists.PlaylistsActivity::class.java))
            },
            HubItem("❤️", "Favoritos") {
                startActivity(Intent(this, LocalMusicActivity::class.java).apply {
                    putExtra(LocalMusicActivity.EXTRA_MODE, LocalMusicActivity.MODE_FAVORITES)
                })
            },
            HubItem("🕒", "Historial") {
                startActivity(Intent(this, LocalMusicActivity::class.java).apply {
                    putExtra(LocalMusicActivity.EXTRA_MODE, LocalMusicActivity.MODE_HISTORY)
                })
            },
            HubItem("⚙", "Configuración") {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        )

        val rv = findViewById<RecyclerView>(R.id.rvHub)
        rv.layoutManager = GridLayoutManager(this, 2)
        rv.adapter = HubAdapter(items)

        setupMiniPlayer()
    }

    // ---------------- 🎵 Mini reproductor ----------------

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private fun setupMiniPlayer() {
        val mini = findViewById<View>(R.id.miniPlayer)
        val btnPlay = findViewById<ImageButton>(R.id.btnMiniPlay)
        mini.setOnClickListener {
            startActivity(Intent(this, com.streamvault.ui.music.PlayerMusicActivity::class.java))
        }
        btnPlay.setOnClickListener {
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
        val c = controller ?: return
        val has = c.mediaItemCount > 0
        val s = PlaybackStateHolder.queue.getOrNull(c.currentMediaItemIndex)
        findViewById<View>(R.id.miniPlayer).visibility = if (has && s != null) View.VISIBLE else View.GONE
        s ?: return
        findViewById<TextView>(R.id.tvMiniTitle).text = s.title
        findViewById<TextView>(R.id.tvMiniArtist).text = s.artist
        Glide.with(this).load(s.albumArtUri)
            .placeholder(R.drawable.ic_channel).error(R.drawable.ic_channel)
            .into(findViewById(R.id.ivMiniArt))
        miniListener.onIsPlayingChanged(c.isPlaying)
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
            h.itemView.setOnClickListener { item.action() }
        }
    }
}
