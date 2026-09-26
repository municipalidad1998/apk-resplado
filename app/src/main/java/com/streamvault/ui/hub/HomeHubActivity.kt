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
                startActivity(Intent(this, TelegramConfigActivity::class.java))
            },
            HubItem("🎶", "Playlists") {
                startActivity(Intent(this, LocalMusicActivity::class.java).apply {
                    putExtra(LocalMusicActivity.EXTRA_MODE, LocalMusicActivity.MODE_PLAYLISTS)
                })
            },
            HubItem("⚙", "Configuración") {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        )

        val rv = findViewById<RecyclerView>(R.id.rvHub)
        rv.layoutManager = GridLayoutManager(this, 2)
        rv.adapter = HubAdapter(items)
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
