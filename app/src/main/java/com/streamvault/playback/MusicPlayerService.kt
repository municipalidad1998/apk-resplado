package com.streamvault.playback

import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Servicio de reproducción de música LOCAL en primer plano.
 * - MediaSession expone controles a bloqueo, notificaciones,
 *   auriculares, Bluetooth y vehículo (⏮ ▶ ⏭ con metadatos).
 * - Funciona con pantalla apagada, teléfono bloqueado, sin Internet.
 * - Crossfade configurable (fade out + fade in) sin modificar archivos.
 * - Normalización de volumen (LUFS) compuesta con la envolvente de fade.
 */
class MusicPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var baseGain = 1f
    private var envelope = 1f
    private var fadingOut = false
    private var fadingIn = false

    private fun crossfadeMs(): Long =
        getSharedPreferences("app_settings", MODE_PRIVATE).getInt("crossfade_sec", 0) * 1000L

    private fun applyVolume() {
        if (::player.isInitialized) player.volume = (baseGain * envelope).coerceIn(0f, 1f)
    }

    /** Envolvente de crossfade: fade-out al final de la pista y fade-in al inicio. */
    private val fadeRunnable = object : Runnable {
        override fun run() {
            val p = player
            val fade = crossfadeMs()
            if (!p.isPlaying || fade <= 0) {
                if (envelope != 1f && !fadingIn) { envelope = 1f; applyVolume() }
                handler.postDelayed(this, 500)
                return
            }
            val dur = p.duration
            val pos = p.currentPosition
            if (dur > 0) {
                val remaining = dur - pos
                when {
                    // Fade-out en los últimos segundos
                    remaining in 1..fade -> {
                        fadingOut = true
                        envelope = (remaining.toFloat() / fade).coerceIn(0f, 1f)
                        applyVolume()
                    }
                    // Fade-in tras el cambio de pista
                    fadingOut || fadingIn -> {
                        fadingOut = false
                        fadingIn = true
                        envelope = (pos.toFloat() / fade).coerceIn(0f, 1f)
                        applyVolume()
                        if (envelope >= 1f) fadingIn = false
                    }
                }
            }
            handler.postDelayed(this, 100)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val audioAttrs = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttrs, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                LoudnessNormalizer.applyTrackGain(this@MusicPlayerService, player, mediaItem?.mediaId)
                baseGain = player.volume
                if (crossfadeMs() > 0) { envelope = 0f; fadingIn = true; applyVolume() }
            }
        })
        mediaSession = MediaSession.Builder(this, player).build()
        handler.post(fadeRunnable)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        handler.removeCallbacks(fadeRunnable)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
