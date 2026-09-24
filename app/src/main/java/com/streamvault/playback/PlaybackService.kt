@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.streamvault.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.streamvault.LuminaApp
import com.streamvault.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object PlaybackEvents { val error = MutableStateFlow<String?>(null); val mixing = MutableStateFlow(false); val audioSessionId = MutableStateFlow(0) }

/** Session, notification and audio focus outlive the Activity. No Activity holds an ExoPlayer. */
class PlaybackService : MediaSessionService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val app get() = application as LuminaApp
    private lateinit var active: ExoPlayer
    private lateinit var incoming: ExoPlayer
    private var session: MediaSession? = null
    private var preparingIndex = C.INDEX_UNSET
    private var fadeStartPosition = -1L
    private var fadeLength = 0L
    private var swapping = false
    private var restoring = false
    private var introAt = 0L
    private var lastSavedAt = 0L
    private val queueMutex = Mutex()
    private val attributes = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build()

    override fun onCreate() {
        super.onCreate()
        active = newPlayer(true); incoming = newPlayer(false)
        active.addListener(listener)
        val launch = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session = MediaSession.Builder(this, active).setSessionActivity(launch).build()
        scope.launch {
            restoring = true
            val queue = app.library.savedQueue()
            if (active.mediaItemCount == 0 && queue.isNotEmpty()) {
                val (index, position) = app.preferences.position()
                active.setMediaItems(queue.map { it.toTrack().mediaItem(app.preferences.state.value) }, index.coerceIn(queue.indices), position)
                active.prepare() // Restore paused. Never surprise the user on boot/open.
            }
            restoring = false
            val ids = (0 until active.mediaItemCount).map { active.getMediaItemAt(it).mediaId }
            queueMutex.withLock { app.library.saveQueue(ids) }
        }
        scope.launch {
            app.preferences.state.collect { settings ->
                cancelMix()
                active.repeatMode = settings.repeat
                active.shuffleModeEnabled = settings.shuffle
                active.pauseAtEndOfMediaItems = !settings.autoPlay
            }
        }
        scope.launch { while (isActive) { tick(); delay(40) } }
    }
    private fun newPlayer(focus: Boolean) = ExoPlayer.Builder(this).setSeekBackIncrementMs(app.preferences.state.value.skipSeconds * 1000L)
        .setSeekForwardIncrementMs(app.preferences.state.value.skipSeconds * 1000L).build().apply {
            setAudioAttributes(attributes, focus)
            setHandleAudioBecomingNoisy(focus)
            setWakeMode(C.WAKE_MODE_LOCAL)
        }
    private val listener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) { PlaybackEvents.audioSessionId.value = audioSessionId }
        override fun onPlayerError(error: PlaybackException) {
            cancelMix()
            PlaybackEvents.error.value = "No se pudo reproducir este archivo (${error.errorCodeName}). Comprueba el formato y el acceso a la carpeta."
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (swapping) return
            cancelMix()
            mediaItem ?: return
            introAt = mediaItem.offsetMs
            if (!restoring && active.currentPosition < mediaItem.offsetMs) active.seekTo(mediaItem.offsetMs)
            if (!restoring) scope.launch { app.library.played(mediaItem.mediaId, System.currentTimeMillis()) }
        }
        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            if (!swapping) { cancelMix(); introAt = -1 }
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying && !swapping) { cancelMix(); savePosition() }
        }
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (!restoring && !swapping && reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                cancelMix()
                val ids = (0 until active.mediaItemCount).map { active.getMediaItemAt(it).mediaId }
                scope.launch { queueMutex.withLock { app.library.saveQueue(ids) } }
            }
        }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            if (app.preferences.state.value.shuffle != shuffleModeEnabled) app.preferences.update { it.copy(shuffle = shuffleModeEnabled) }
        }
        override fun onRepeatModeChanged(repeatMode: Int) {
            if (app.preferences.state.value.repeat != repeatMode) app.preferences.update { it.copy(repeat = repeatMode) }
        }
    }
    private fun tick() {
        if (!active.isPlaying || swapping) return
        val item = active.currentMediaItem ?: return
        val duration = active.duration.takeIf { it > 0 } ?: return
        val position = active.currentPosition
        val settings = app.preferences.state.value
        if (System.currentTimeMillis() - lastSavedAt > 3000) { savePosition(); lastSavedAt = System.currentTimeMillis() }
        if (settings.crossfade == 0 || !settings.autoPlay || active.repeatMode == Player.REPEAT_MODE_ONE || !active.hasNextMediaItem()) {
            if (preparingIndex != C.INDEX_UNSET) cancelMix()
            val inGain = if (settings.fades && introAt >= 0) ((position - introAt) / 450f).coerceIn(0f, 1f) else 1f
            val outGain = if (settings.fades && !active.hasNextMediaItem()) ((duration - position) / 450f).coerceIn(0f, 1f) else 1f
            active.volume = minOf(inGain, outGain)
            return
        }
        val nextIndex = active.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET) return
        val next = active.getMediaItemAt(nextIndex)
        val requested = MixMath.duration(settings.crossfade * 1000L, duration - item.offsetMs, next.durationMs - next.offsetMs)
        if (requested <= 0) return
        val remaining = duration - position
        if (preparingIndex != C.INDEX_UNSET && preparingIndex != nextIndex) cancelMix()
        if (preparingIndex == C.INDEX_UNSET && remaining <= requested + 2500) {
            preparingIndex = nextIndex
            incoming.setMediaItems((0 until active.mediaItemCount).map(active::getMediaItemAt), nextIndex, next.offsetMs)
            incoming.repeatMode = active.repeatMode
            incoming.shuffleModeEnabled = active.shuffleModeEnabled
            incoming.pauseAtEndOfMediaItems = active.pauseAtEndOfMediaItems
            incoming.volume = 0f
            incoming.prepare()
        }
        if (preparingIndex != C.INDEX_UNSET && fadeStartPosition < 0 && remaining <= requested && incoming.playbackState == Player.STATE_READY) {
            fadeStartPosition = position
            // Leave a small safety margin so the outgoing player's auto-transition cannot race the swap.
            fadeLength = (remaining - 120).coerceAtLeast(1)
            incoming.play()
            PlaybackEvents.mixing.value = true
        }
        if (fadeStartPosition >= 0) {
            if (incoming.playerError != null || incoming.playbackState == Player.STATE_BUFFERING) { cancelMix(); return }
            val progress = (position - fadeStartPosition).toFloat() / fadeLength
            val (outGain, inGain) = MixMath.gains(progress)
            active.volume = outGain; incoming.volume = inGain
            if (progress >= 1f) finishMix()
        } else active.volume = 1f
    }
    private fun finishMix() {
        swapping = true
        val previous = active
        previous.removeListener(listener)
        previous.setAudioAttributes(attributes, false)
        previous.setHandleAudioBecomingNoisy(false)
        active = incoming; incoming = previous
        active.setAudioAttributes(attributes, true)
        active.setHandleAudioBecomingNoisy(true)
        active.volume = 1f
        active.addListener(listener)
        session?.setPlayer(active)
        PlaybackEvents.audioSessionId.value = active.audioSessionId
        incoming.stop(); incoming.clearMediaItems(); incoming.volume = 0f
        preparingIndex = C.INDEX_UNSET; fadeStartPosition = -1; introAt = -1
        PlaybackEvents.mixing.value = false
        swapping = false
        active.currentMediaItem?.let { item -> scope.launch { app.library.played(item.mediaId, System.currentTimeMillis()) } }
        savePosition()
    }
    private fun cancelMix() {
        if (swapping) return
        preparingIndex = C.INDEX_UNSET; fadeStartPosition = -1
        incoming.stop(); incoming.clearMediaItems(); incoming.volume = 0f
        active.volume = 1f
        PlaybackEvents.mixing.value = false
    }
    private fun savePosition() {
        if (active.mediaItemCount > 0) app.preferences.savePosition(active.currentMediaItemIndex, active.currentPosition)
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onTaskRemoved(rootIntent: Intent?) { if (!active.playWhenReady) { stopSelf() } }
    override fun onDestroy() {
        savePosition(); scope.cancel()
        session?.release(); session = null
        active.release(); incoming.release()
        super.onDestroy()
    }
}
