package com.streamvault.ui.player
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.streamvault.data.model.Channel
import com.streamvault.databinding.ActivityPlayerBinding
class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private lateinit var channel: Channel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        channel = intent.getParcelableExtra("channel")!!
        binding.tvTitle.text = channel.name
        binding.btnBack.setOnClickListener { finish() }
        initPlayer()
    }
    private fun initPlayer() {
        val trackSelector = DefaultTrackSelector(this)
        // Buffer optimizado para IPTV sin trabas ni cortes
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 60_000, 2_500, 5_000)
            .build()
        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build().also { p ->
                binding.playerView.player = p
                p.setMediaItem(MediaItem.fromUri(channel.url))
                p.prepare()
                p.playWhenReady = true
                p.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        binding.tvError.visibility = View.VISIBLE
                        binding.tvError.text = "Error al reproducir: ${error.message}"
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                        binding.progressLoading.visibility = if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                    }
                })
            }
    }
    override fun onPause() { super.onPause(); player?.pause() }
    override fun onResume() { super.onResume(); player?.play() }
    override fun onDestroy() { super.onDestroy(); player?.release(); player = null }
}
