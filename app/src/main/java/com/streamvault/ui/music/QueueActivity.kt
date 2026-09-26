package com.streamvault.ui.music

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.streamvault.R
import com.streamvault.data.local.LocalSong
import com.streamvault.playback.MusicPlayerService
import com.streamvault.playback.PlaybackStateHolder

/**
 * 📋 Cola de reproducción: reproducir ahora, reproducir después,
 * agregar a cola, eliminar, reordenar y vaciar.
 * Funciona con música local y online (MediaController compartido).
 */
class QueueActivity : AppCompatActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var queue: MutableList<LocalSong> = mutableListOf()
    private lateinit var adapter: QueueAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_queue)
        findViewById<View>(R.id.btnBackQueue).setOnClickListener { finish() }

        queue = PlaybackStateHolder.queue.toMutableList()
        adapter = QueueAdapter(
            queue = queue,
            currentIndex = { controller?.currentMediaItemIndex ?: -1 },
            onPlay = { pos ->
                controller?.let { it.seekTo(pos, 0L); it.play() }
                refresh()
            },
            onMenu = { pos, v -> showTrackMenu(pos, v) }
        )
        findViewById<RecyclerView>(R.id.rvQueue).apply {
            layoutManager = LinearLayoutManager(this@QueueActivity)
            adapter = this@QueueActivity.adapter
        }
        findViewById<Button>(R.id.btnClearQueue).setOnClickListener {
            controller?.clearMediaItems()
            queue.clear()
            PlaybackStateHolder.queue = emptyList()
            refresh()
        }
    }

    private fun showTrackMenu(pos: Int, anchor: View) {
        val s = queue.getOrNull(pos) ?: return
        val opts = arrayOf(
            "▶ Reproducir ahora", "⏭ Reproducir después",
            "⬆ Subir", "⬇ Bajar", "🗑 Quitar de la cola"
        )
        AlertDialog.Builder(this).setTitle(s.title).setItems(opts) { _, which ->
            when (which) {
                0 -> { controller?.let { it.seekTo(pos, 0L); it.play() } }
                1 -> moveTrack(pos, (controller?.currentMediaItemIndex ?: 0) + 1)
                2 -> moveTrack(pos, pos - 1)
                3 -> moveTrack(pos, pos + 1)
                4 -> {
                    queue.removeAt(pos)
                    controller?.removeMediaItem(pos)
                }
            }
            PlaybackStateHolder.queue = queue
            refresh()
        }.show()
    }

    private fun moveTrack(from: Int, to: Int) {
        if (to !in queue.indices || from !in queue.indices) return
        val item = queue.removeAt(from)
        queue.add(to, item)
        controller?.moveMediaItem(from, to)
    }

    private fun refresh() {
        adapter.notifyDataSetChanged()
        findViewById<TextView>(R.id.tvQueueCount).text = "${queue.size} canción(es) en cola"
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, MusicPlayerService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(object : Player.Listener {
                override fun onMediaItemTransition(item: androidx.media3.common.MediaItem?, reason: Int) { refresh() }
            })
            refresh()
        }, MoreExecutors.directExecutor())
    }

    override fun onStop() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        super.onStop()
    }

    class QueueAdapter(
        private val queue: List<LocalSong>,
        private val currentIndex: () -> Int,
        private val onPlay: (Int) -> Unit,
        private val onMenu: (Int, View) -> Unit
    ) : RecyclerView.Adapter<QueueAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val art: ImageView = v.findViewById(R.id.ivSongArt)
            val title: TextView = v.findViewById(R.id.tvSongTitle)
            val artist: TextView = v.findViewById(R.id.tvSongArtist)
            val dur: TextView = v.findViewById(R.id.tvSongDuration)
            val menu: TextView = v.findViewById(R.id.tvSongMenu)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_song, p, false))

        override fun getItemCount() = queue.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val s = queue[pos]
            h.title.text = s.title
            h.artist.text = s.artist
            val sec = s.duration / 1000
            h.dur.text = "%d:%02d".format(sec / 60, sec % 60)
            h.menu.visibility = View.VISIBLE
            val playing = pos == currentIndex()
            h.title.setTextColor(if (playing) 0xFF8A7CFF.toInt() else 0xFFF5F5FA.toInt())
            Glide.with(h.art).load(s.albumArtUri)
                .placeholder(R.drawable.ic_channel).error(R.drawable.ic_channel).into(h.art)
            h.itemView.setOnClickListener { onPlay(h.bindingAdapterPosition) }
            h.menu.setOnClickListener { onMenu(h.bindingAdapterPosition, it) }
        }
    }
}
