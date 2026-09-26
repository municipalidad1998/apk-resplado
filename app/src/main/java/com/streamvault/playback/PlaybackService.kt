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
import kotlinx.coroutines.flow.distinctUntilChanged
import com.streamvault.data.Track
import com.streamvault.network.ConnectivityMonitor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object PlaybackEvents { val error = MutableStateFlow<String?>(null); val mixing = MutableStateFlow(false); val audioSessionId = MutableStateFlow(0); val analyzing = MutableStateFlow<String?>(null) }

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
    private val cues = mutableMapOf<String, Track>()
    private var resolveJob: Job? = null
    private var resolveGeneration = 0
    private var gateId: String? = null
    private var resumeAfterAnalysis = false
    private var ignoredPauseEvents = 0
    private var internalSeek = false
    private var soughtWhileAnalyzing = false
    private var prefetchJob: Job? = null
    private var prefetchId: String? = null
    private val preparedIds = mutableSetOf<String>()
    private val normalizer = LoudnessNormalizer()
    private val dynamics = DynamicsController()
    private val connectivity = ConnectivityMonitor(this)
    private var offlineError = false
    private var attenuation = 1f
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
                active.currentMediaItem?.let { applyTrackGain(it, active) }
                if (!settings.normalize) normalizer.release()
                if (CompressorPreset.from(settings.compressor).key == CompressorPreset.OFF.key) dynamics.release()
                preparedIds.clear(); prefetchJob?.cancel(); prefetchId = null
            }
        }
        scope.launch {
            app.library.observeQueueCues().distinctUntilChanged().collect { records ->
                records.forEach { record ->
                    val fresh = record.toTrack()
                    val old = cues.put(record.id, fresh)
                    val changed = old == null || old.offset(app.preferences.state.value.detectSilence) != fresh.offset(app.preferences.state.value.detectSilence) ||
                        old.playbackEndMs != fresh.playbackEndMs || old.crossfadeSeconds != fresh.crossfadeSeconds
                    if (changed) {
                        if (active.currentMediaItem?.mediaId == record.id || (preparingIndex in 0 until active.mediaItemCount && active.getMediaItemAt(preparingIndex).mediaId == record.id)) cancelMix()
                        if (active.currentMediaItem?.mediaId == record.id && !isOriginal(active.currentMediaItem!!) &&
                            old != null && old.offset(app.preferences.state.value.detectSilence) != fresh.offset(app.preferences.state.value.detectSilence)) {
                            val start = fresh.offset(app.preferences.state.value.detectSilence)
                            if (active.currentPosition < start) seekInternally(start)
                        }
                    }
                }
            }
        }
        connectivity.start()
        scope.launch {
            connectivity.state.collect { net ->
                // A track that failed only because the network dropped resumes by itself.
                if (net.online && offlineError && active.playerError != null) {
                    offlineError = false
                    active.prepare(); active.play()
                }
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
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                val id = active.currentMediaItem?.mediaId ?: return
                val duration = active.duration
                if (duration > 0) scope.launch { app.library.discoveredDuration(id, duration) }
            }
        }
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            PlaybackEvents.audioSessionId.value = audioSessionId
            val item = active.currentMediaItem ?: return
            val preset = CompressorPreset.from(app.preferences.state.value.compressor)
            val gain = gainOf(item)
            dynamics.apply(audioSessionId, preset, CompressorMath.postGainDb(preset, gain))
            normalizer.setBoost(audioSessionId, CompressorMath.enhancerMillibels(preset, gain))
        }
        override fun onPlayerError(error: PlaybackException) {
            cancelMix()
            val isNetwork = error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
            val net = connectivity.state.value
            offlineError = isNetwork || (isRemote(active.currentMediaItem) && !net.online)
            PlaybackEvents.error.value = when {
                offlineError -> "Se perdió la conexión. La canción continuará cuando vuelva el Internet."
                isRemote(active.currentMediaItem) -> "El servidor de la canción no respondió (${error.errorCodeName}). Intenta otra calidad u otra canción."
                else -> "No se pudo reproducir este archivo (${error.errorCodeName}). Comprueba el formato y el acceso a la carpeta."
            }
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (swapping) return
            cancelMix()
            if (mediaItem == null) {
                ++resolveGeneration; resolveJob?.cancel(); gateId = null; PlaybackEvents.analyzing.value = null
                return
            }
            beginTrack(mediaItem)
            if (!restoring) scope.launch { app.library.played(mediaItem.mediaId, System.currentTimeMillis()) }
        }
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady && ignoredPauseEvents > 0) { ignoredPauseEvents--; return }
            if (gateId != null) {
                resumeAfterAnalysis = playWhenReady
                if (playWhenReady) pauseForAnalysis()
            }
        }
        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            if (!swapping) {
                cancelMix(); introAt = -1
                if (gateId != null && !internalSeek && reason == Player.DISCONTINUITY_REASON_SEEK && oldPosition.mediaItemIndex == newPosition.mediaItemIndex) soughtWhileAnalyzing = true
            }
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
        if (!active.isPlaying || swapping || gateId != null) return
        val item = active.currentMediaItem ?: return
        val fileDuration = active.duration.takeIf { it > 0 } ?: return
        val duration = PlaybackCue.end(fileDuration, startOf(item), endOf(item))
        val position = active.currentPosition
        val settings = app.preferences.state.value
        if (System.currentTimeMillis() - lastSavedAt > 3000) { savePosition(); lastSavedAt = System.currentTimeMillis() }
        val crossfade = (if (cues.containsKey(item.mediaId)) cues[item.mediaId]?.crossfadeSeconds else item.crossfadeSeconds) ?: settings.crossfade
        if (position >= duration && duration < fileDuration) {
            cancelMix()
            when {
                active.repeatMode == Player.REPEAT_MODE_ONE -> seekInternally(startOf(item))
                settings.autoPlay && active.hasNextMediaItem() -> {
                    val next = active.nextMediaItemIndex
                    active.seekTo(next, startOf(active.getMediaItemAt(next))); active.play()
                }
                else -> active.pause()
            }
            return
        }
        if (crossfade == 0 || !settings.autoPlay || active.repeatMode == Player.REPEAT_MODE_ONE || !active.hasNextMediaItem()) {
            if (preparingIndex != C.INDEX_UNSET) cancelMix()
            val inGain = if (settings.fades && introAt >= 0) ((position - introAt) / 450f).coerceIn(0f, 1f) else 1f
            val outGain = if (settings.fades && !active.hasNextMediaItem()) ((duration - position) / 450f).coerceIn(0f, 1f) else 1f
            applyVolume(active, minOf(inGain, outGain))
            return
        }
        val nextIndex = active.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET) return
        val next = active.getMediaItemAt(nextIndex)
        prefetch(next)
        val nextEnd = PlaybackCue.end(next.durationMs, startOf(next), endOf(next))
        val requested = PlaybackCue.overlap(crossfade * 1000L, duration - startOf(item), nextEnd - startOf(next))
        if (requested <= 0) return
        val remaining = duration - position
        if (preparingIndex != C.INDEX_UNSET && preparingIndex != nextIndex) cancelMix()
        if (preparingIndex == C.INDEX_UNSET && next.mediaId in preparedIds && remaining <= requested + 2500) {
            preparingIndex = nextIndex
            incoming.setMediaItems((0 until active.mediaItemCount).map(active::getMediaItemAt), nextIndex, startOf(next))
            incoming.repeatMode = active.repeatMode
            incoming.shuffleModeEnabled = active.shuffleModeEnabled
            incoming.pauseAtEndOfMediaItems = active.pauseAtEndOfMediaItems
            incoming.volume = 0f
            normalizer.setBoost(incoming.audioSessionId, boostOf(next))
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
            applyVolume(active, outGain)
            incoming.volume = (inGain * attenuationForIncoming()).coerceIn(0f, 1f)
            if (progress >= 1f) finishMix()
        } else applyVolume(active, 1f)
    }
    /** Player volume is always the fade/mix gain times the normalization attenuation. */
    private fun applyVolume(player: ExoPlayer, gain: Float) {
        player.volume = (gain * if (player === active) attenuation else attenuationForIncoming()).coerceIn(0f, 1f)
    }
    private fun attenuationForIncoming(): Float {
        val index = preparingIndex
        if (index !in 0 until active.mediaItemCount) return 1f
        return volumeOf(active.getMediaItemAt(index))
    }
    /** Gain in dB and resulting volume for one media item, using live Room cues when available. */
    private fun gainOf(item: MediaItem): Float {
        val settings = app.preferences.state.value
        if (!settings.normalize) return 0f
        val measured = cues[item.mediaId]?.loudnessDb ?: item.loudnessDb
        return LoudnessMath.gainDb(settings.targetLoudnessDb.toFloat(), measured)
    }
    private fun volumeOf(item: MediaItem): Float = LoudnessMath.attenuation(gainOf(item))
    private fun boostOf(item: MediaItem): Int = LoudnessMath.boostMillibels(gainOf(item))
    private fun applyTrackGain(item: MediaItem, player: ExoPlayer) {
        val preset = CompressorPreset.from(app.preferences.state.value.compressor)
        val gain = gainOf(item)
        attenuation = volumeOf(item)
        val session = player.audioSessionId
        if (session > 0) {
            // The compressor already lifts the track with its post gain and a −1 dB limiter,
            // so the loudness enhancer stays off to avoid applying the same gain twice.
            dynamics.apply(session, preset, CompressorMath.postGainDb(preset, gain))
            normalizer.setBoost(session, CompressorMath.enhancerMillibels(preset, gain))
        }
        if (player === active) applyVolume(player, 1f)
    }
    private fun isOriginal(item: MediaItem) = item.mediaMetadata.extras?.getBoolean("original") == true
    private fun isRemote(item: MediaItem?): Boolean = item?.mediaMetadata?.extras?.getString("uri")?.startsWith("http") == true
    private fun startOf(item: MediaItem): Long = if (isOriginal(item)) 0 else
        cues[item.mediaId]?.offset(app.preferences.state.value.detectSilence) ?: item.offsetMs
    private fun endOf(item: MediaItem): Long? = if (cues.containsKey(item.mediaId)) cues[item.mediaId]?.playbackEndMs else item.selectedEndMs
    private fun seekInternally(position: Long) {
        internalSeek = true
        try { active.seekTo(position) } finally { internalSeek = false }
    }
    private fun pauseForAnalysis() {
        if (active.playWhenReady) { ignoredPauseEvents++; active.pause() }
    }
    private fun beginTrack(item: MediaItem) {
        val shouldResume = active.playWhenReady || (gateId != null && resumeAfterAnalysis)
        val generation = ++resolveGeneration
        resolveJob?.cancel()
        val player = active
        gateId = item.mediaId
        soughtWhileAnalyzing = false
        resumeAfterAnalysis = shouldResume
        pauseForAnalysis()
        PlaybackEvents.analyzing.value = item.mediaId
        resolveJob = scope.launch {
            try {
                val stored = app.library.track(item.mediaId)
                val track = if (isOriginal(item) || stored?.source == "online") stored else app.analysis.resolve(item.mediaId)
                if (generation != resolveGeneration || active !== player) return@launch
                if (track != null) cues[item.mediaId] = track
                preparedIds += item.mediaId
                applyTrackGain(item, player)
                val start = startOf(item)
                if (!soughtWhileAnalyzing && player.currentPosition < start) seekInternally(start)
                introAt = start
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { PlaybackEvents.error.value = e.localizedMessage ?: "No se pudo detectar el inicio" }
            finally {
                if (generation == resolveGeneration && active === player) {
                    gateId = null
                    PlaybackEvents.analyzing.value = null
                    if (resumeAfterAnalysis) player.play()
                }
            }
        }
    }
    private fun prefetch(item: MediaItem) {
        if (item.mediaId in preparedIds || prefetchId == item.mediaId) return
        prefetchJob?.cancel()
        prefetchId = item.mediaId
        prefetchJob = scope.launch {
            try {
                val stored = app.library.track(item.mediaId)
                val track = if (isOriginal(item) || stored?.source == "online") stored else app.analysis.resolve(item.mediaId)
                if (track != null) cues[item.mediaId] = track
                preparedIds += item.mediaId
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Active-track gate will surface errors, never invent a successful offset. */ }
        }
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
        normalizer.clear(incoming.audioSessionId)
        dynamics.clear(incoming.audioSessionId)
        val item = active.currentMediaItem
        attenuation = item?.let(::volumeOf) ?: 1f
        if (item != null) applyTrackGain(item, active)
        applyVolume(active, 1f)
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
        normalizer.clear(incoming.audioSessionId)
        dynamics.clear(incoming.audioSessionId)
        incoming.stop(); incoming.clearMediaItems(); incoming.volume = 0f
        applyVolume(active, 1f)
        PlaybackEvents.mixing.value = false
    }
    private fun savePosition() {
        if (active.mediaItemCount > 0) app.preferences.savePosition(active.currentMediaItemIndex, active.currentPosition)
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onTaskRemoved(rootIntent: Intent?) { if (!active.playWhenReady && gateId == null) { stopSelf() } }
    override fun onDestroy() {
        normalizer.release()
        dynamics.release()
        connectivity.stop()
        savePosition(); ++resolveGeneration; scope.cancel(); PlaybackEvents.analyzing.value = null
        session?.release(); session = null
        active.release(); incoming.release()
        super.onDestroy()
    }
}
