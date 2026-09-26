package com.streamvault.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Servicio de reproducción de música LOCAL en primer plano.
 * - MediaSession expone los controles a: pantalla de bloqueo, panel de
 *   notificaciones, auriculares, Bluetooth y vehículo (⏮ ▶ ⏭ con
 *   canción, artista, portada y progreso).
 * - Funciona con pantalla apagada, teléfono bloqueado, app cerrada y
 *   sin conexión a Internet.
 * - Maneja Audio Focus automáticamente (pausa si entra una llamada).
 * Solo reproduce fuentes locales; nunca contenido online.
 */
class MusicPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer
        private set

    override fun onCreate() {
        super.onCreate()
        val audioAttrs = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttrs, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true) // pausa al desconectar auriculares
            .setWakeMode(C.WAKE_MODE_LOCAL)    // mantiene CPU activa con pantalla apagada
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
        // Normalización de volumen (LUFS) aplicada por pista
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                LoudnessNormalizer.applyTrackGain(this@MusicPlayerService, player, mediaItem?.mediaId)
            }
        })
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
