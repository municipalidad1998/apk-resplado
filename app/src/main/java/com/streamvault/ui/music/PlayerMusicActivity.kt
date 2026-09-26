package com.streamvault.ui.music

import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.streamvault.R
import com.streamvault.data.local.LocalSong
import com.streamvault.playback.MusicPlayerService

/**
 * Reproductor de música local a pantalla completa.
 * Sigue funcionando con pantalla apagada, teléfono bloqueado o app
 * en segundo plano gracias a MusicPlayerService (MediaSession),
 * con controles en bloqueo, notificaciones, auriculares y Bluetooth.
 */
class PlayerMusicActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QUEUE = "queue"
        const val EXTRA_INDEX = "index"
    }

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var queue: List<LocalSong> = emptyList()
    private var startIndex = 0
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var ivCover: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvArtist: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrent: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnPlay: ImageButton
    private lateinit var btnShuffle: ImageButton
    private lateinit var btnRepeat: ImageButton

    private val progressRunnable = object : Runnable {
        override fun run() {
            controller?.let { c ->
                val dur = c.duration
                if (dur > 0) {
                    seekBar.max = (dur / 1000).toInt()
                    seekBar.progress = (c.currentPosition / 1000).toInt()
                    tvCurrent.text = fmt(c.currentPosition)
                    tvTotal.text = fmt(dur)
                }
                updatePlayButton(c.isPlaying)
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_music)

        queue = intent.getParcelableArrayListExtra<LocalSong>(EXTRA_QUEUE) ?: emptyList()
        startIndex = intent.getIntExtra(EXTRA_INDEX, 0)

        ivCover = findViewById(R.id.ivCover)
        tvTitle = findViewById(R.id.tvNowTitle)
        tvArtist = findViewById(R.id.tvNowArtist)
        seekBar = findViewById(R.id.seekMusic)
        tvCurrent = findViewById(R.id.tvCurrentTime)
        tvTotal = findViewById(R.id.tvTotalTime)
        btnPlay = findViewById(R.id.btnPlayPause)
        btnShuffle = findViewById(R.id.btnShuffle)
        btnRepeat = findViewById(R.id.btnRepeat)
        findViewById<View>(R.id.btnClosePlayer).setOnClickListener { finish() }

        findViewById<ImageButton>(R.id.btnNext).setOnClickListener { controller?.seekToNextMediaItem() }
        findViewById<ImageButton>(R.id.btnPrev).setOnClickListener { controller?.seekToPreviousMediaItem() }
        btnPlay.setOnClickListener {
            controller?.let { if (it.isPlaying) it.pause() else it.play() }
        }
        btnShuffle.setOnClickListener {
            controller?.let {
                it.shuffleModeEnabled = !it.shuffleModeEnabled
                btnShuffle.alpha = if (it.shuffleModeEnabled) 1f else 0.4f
            }
        }
        btnRepeat.setOnClickListener {
            controller?.let {
                it.repeatMode = when (it.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                btnRepeat.alpha = if (it.repeatMode == Player.REPEAT_MODE_OFF) 0.4f else 1f
            }
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) controller?.seekTo(p * 1000L)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, MusicPlayerService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(playerListener)
            if (queue.isNotEmpty()) startQueue()
            handler.post(progressRunnable)
        }, MoreExecutors.directExecutor())
    }

    private fun startQueue() {
        val items = queue.map { s ->
            MediaItem.Builder()
                .setMediaId(s.contentUri)
                .setUri(s.contentUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .setAlbumTitle(s.album)
                        .build()
                )
                .build()
        }
        controller?.apply {
            setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
            prepare()
            play()
        }
        updateSongUI(queue.getOrNull(startIndex))
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val idx = controller?.currentMediaItemIndex ?: return
            updateSongUI(queue.getOrNull(idx))
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayButton(isPlaying)
        }
    }

    private fun updateSongUI(s: LocalSong?) {
        s ?: return
        tvTitle.text = s.title
        tvArtist.text = "${s.artist} · ${s.album}"
        Glide.with(this).load(s.albumArtUri)
            .placeholder(R.drawable.logo_circle_bg)
            .error(R.drawable.logo_circle_bg).into(ivCover)
    }

    private fun updatePlayButton(playing: Boolean) {
        btnPlay.setImageResource(if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
    }

    private fun fmt(ms: Long): String {
        val sec = ms / 1000
        return "%d:%02d".format(sec / 60, sec % 60)
    }

    override fun onStop() {
        handler.removeCallbacks(progressRunnable)
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        super.onStop()
    }
}
