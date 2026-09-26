@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.streamvault.ui

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.*
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.paging.*
import androidx.work.WorkManager
import com.streamvault.BuildConfig
import com.streamvault.data.Track
import com.streamvault.LuminaApp
import com.streamvault.analysis.LoudnessAnalyzer
import com.streamvault.analysis.SilenceAnalyzer
import com.streamvault.data.*
import com.streamvault.playback.*
import com.streamvault.scanner.ScanWorker
import com.streamvault.flac.AudioFormatReader
import com.streamvault.search.SmartSearch
import com.streamvault.telegram.TelegramRepository
import com.streamvault.network.ConnectivityMonitor
import com.streamvault.network.NetState
import com.streamvault.online.OnlineRepository
import com.streamvault.online.OnlineResult
import com.streamvault.online.Quality
import com.streamvault.playback.CompressorPreset
import com.streamvault.update.ReleaseInfo
import com.streamvault.update.UpdateInstaller
import com.streamvault.update.UpdateManager
import com.streamvault.update.UpdateParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.*
import java.util.UUID

@Suppress("OPT_IN_USAGE")
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    val app = application as LuminaApp
    private val dao = app.library
    val settings = app.preferences.state
    val home = dao.home().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favorites = dao.favorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val whatsapp = dao.source("whatsapp").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val instrumentals = dao.source("instrumental").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val added = dao.added().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val count = dao.count().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val playlists = dao.playlists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val folders = dao.folders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val artists = dao.artists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val albums = dao.albums().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val genres = dao.genres().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(LibraryFilter())
    val tracks = combine(query.debounce(180), filter) { q, f -> q to f }.flatMapLatest { (q, f) ->
        Pager(PagingConfig(pageSize = 60, prefetchDistance = 15, maxSize = 240)) {
            dao.page(searchPattern(q), f.source, f.favorite, f.folder, f.artist, f.album, f.genre, f.sort)
        }.flow
    }.cachedIn(viewModelScope)
    val scan = WorkManager.getInstance(app).getWorkInfosForUniqueWorkFlow("scan")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val message = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updates = UpdateManager(application)
    val online = OnlineRepository(app)
    private val connectivity = ConnectivityMonitor(app)
    val telegram = TelegramRepository(app)
    val net: StateFlow<NetState> get() = connectivity.state
    val onlineResults = MutableStateFlow<List<OnlineResult>>(emptyList())
    val localResults = MutableStateFlow<List<Track>>(emptyList())
    val ranked = MutableStateFlow<List<Track>>(emptyList())
    val onlineSearching = MutableStateFlow(false)
    val onlineError = MutableStateFlow<String?>(null)
    val roots = MutableStateFlow(app.preferences.roots().toList())
    val playback = MutableStateFlow(PlaybackState())
    val mixing = PlaybackEvents.mixing
    val analyzing = PlaybackEvents.analyzing
    val current = playback.map { it.currentId }.distinctUntilChanged().flatMapLatest { id ->
        if (id.isEmpty()) flowOf(null) else dao.observeTrack(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private var queueLoad: Job? = null
    private var controller: MediaController? = null
    private val controllerReady = CompletableDeferred<MediaController>()
    private val future = MediaController.Builder(app, SessionToken(app, ComponentName(app, PlaybackService::class.java))).buildAsync()
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            // Crossfade emits volume events every 40 ms. Do not rebuild a 10k-item queue for each gain update.
            sync(events.contains(Player.EVENT_TIMELINE_CHANGED) || events.contains(Player.EVENT_MEDIA_METADATA_CHANGED))
        }
    }
    init {
        connectivity.start()
        viewModelScope.launch {
            query.debounce(220).collect { text -> ranked.value = runCatching { searchLocal(text) }.getOrDefault(emptyList()) }
        }
        UpdateInstaller.register(app)
        viewModelScope.launch {
            UpdateInstaller.status.collect { status ->
                when (status) {
                    is UpdateInstaller.Status.Conflict -> updateState.value = UpdateState.SignatureConflict(status.detail)
                    is UpdateInstaller.Status.Failed -> updateState.value = UpdateState.Failed(status.detail ?: "Android no pudo instalar la actualización")
                    else -> Unit
                }
            }
        }
        future.addListener({
            runCatching { future.get() }.onSuccess { c -> controller = c; c.addListener(listener); sync(); controllerReady.complete(c) }
                .onFailure { controllerReady.completeExceptionally(it); message.value = "No se pudo conectar el reproductor: ${it.localizedMessage}" }
        }, ContextCompat.getMainExecutor(app))
        viewModelScope.launch { while (isActive) { sync(false); delay(250) } }
        viewModelScope.launch { PlaybackEvents.error.collect { if (it != null) { message.value = it; PlaybackEvents.error.value = null } } }
    }
    private fun sync(queueChanged: Boolean = true) {
        val c = controller ?: return
        playback.value = PlaybackState(c.currentMediaItem?.mediaId.orEmpty(), c.currentMediaItemIndex,
            c.isPlaying, c.currentPosition.coerceAtLeast(0), c.duration.takeIf { it > 0 } ?: 0,
            if (queueChanged) (0 until c.mediaItemCount).map { i -> val m = c.getMediaItemAt(i); QueueTrack(m.mediaId, m.mediaMetadata.title.toString(), m.mediaMetadata.artist.toString(), m.mediaMetadata.artworkUri?.toString(), m.durationMs, m.mediaMetadata.albumTitle?.toString().orEmpty()) } else playback.value.queue,
            c.playbackState == Player.STATE_BUFFERING)
    }
    fun play(track: Track, context: List<Track>? = null, original: Boolean = false) {
        queueLoad?.cancel()
        queueLoad = viewModelScope.launch {
            try {
                // A context-menu play should not silently replace the queue with one song.
                val list = context ?: dao.playbackQueue("%", "", false, "", "", "", "", "title").map { it.toTrack() }
                startQueue(track, list, original)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { notify("No se pudo cargar la cola: ${e.localizedMessage}") }
        }
    }
    private suspend fun startQueue(track: Track, context: List<Track>, original: Boolean = false) {
        val c = withTimeout(10_000) { controllerReady.await() }
        val list = if (context.any { it.id == track.id }) context else listOf(track) + context
        val index = list.indexOfFirst { it.id == track.id }
        c.setMediaItems(list.map { it.mediaItem(settings.value, original && it.id == track.id) }, index, if (original) 0 else track.offset(settings.value.detectSilence))
        c.prepare(); c.play()
    }
    fun playLibrary(track: Track, context: List<Track>? = null) {
        if (context != null) { play(track, context); return }
        queueLoad?.cancel()
        val q = searchPattern(query.value)
        val f = filter.value

        queueLoad = viewModelScope.launch {
            try {
                val queue = dao.playbackQueue(q, f.source, f.favorite, f.folder, f.artist, f.album, f.genre, f.sort)
                startQueue(track, queue.map { it.toTrack() })
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { notify("No se pudo cargar la cola: ${e.localizedMessage}") }
        }
    }
    fun toggle() { controller?.let { if (it.playWhenReady) it.pause() else { if (it.playbackState == Player.STATE_IDLE) it.prepare(); it.play() } } }
    fun next() = navigate(1)
    fun previous() = navigate(-1)
    private fun navigate(direction: Int) {
        val c = controller ?: return notify("El reproductor se está conectando…")
        if (c.mediaItemCount < 2) return notify("Solo hay una pista en la cola. Agrega canciones desde A continuación.")
        val index = if (direction > 0) c.nextMediaItemIndex.takeIf { it != C.INDEX_UNSET } ?: 0
            else c.previousMediaItemIndex.takeIf { it != C.INDEX_UNSET } ?: c.mediaItemCount - 1
        // Previous means previous song, not restart-after-three-seconds. The ± buttons handle in-track seeks.
        c.seekTo(index, c.getMediaItemAt(index).offsetMs); c.prepare(); c.play()
    }
    fun seek(position: Long) { controller?.seekTo(position.coerceIn(0, playback.value.duration)) }
    fun skip(direction: Int) = seek(MixMath.seek(playback.value.position, direction * settings.value.skipSeconds * 1000L, playback.value.duration))
    fun enqueue(track: Track, next: Boolean = false) {
        val c = controller ?: return
        if (next) c.addMediaItem((c.currentMediaItemIndex + 1).coerceIn(0, c.mediaItemCount), track.mediaItem(settings.value)) else c.addMediaItem(track.mediaItem(settings.value))
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        notify(if (next) "Se reproducirá después" else "Agregada a la cola")
    }
    fun queueSelect(index: Int) { controller?.let { it.seekTo(index, it.getMediaItemAt(index).offsetMs); it.play() } }
    fun queueRemove(index: Int) { controller?.let { if (index in 0 until it.mediaItemCount) it.removeMediaItem(index) } }
    fun queueMove(from: Int, to: Int) { controller?.let { if (from in 0 until it.mediaItemCount && to in 0 until it.mediaItemCount) it.moveMediaItem(from, to) } }
    fun favorite(track: Track) = task { dao.favorite(track.id) }
    fun hide(track: Track) = task {
        dao.hide(track.id)
        controller?.let { c -> (c.mediaItemCount - 1 downTo 0).filter { c.getMediaItemAt(it).mediaId == track.id }.forEach(c::removeMediaItem) }
        notify("Eliminada de la biblioteca. El archivo original sigue intacto.")
    }
    fun edit(track: Track, name: String, title: String, artist: String, album: String, genre: String, notes: String, tags: String,
             year: Long = -1, trackNumber: Int = -1, discNumber: Int = -1, albumArtist: String = "") = task {
        val latest = dao.track(track.id) ?: return@task
        dao.update(latest.copy(customName = name.trim(), title = title.ifBlank { latest.title }, artist = artist.ifBlank { "Artista desconocido" },
            album = album.ifBlank { "Sin álbum" }, genre = genre.ifBlank { "Sin género" }, notes = notes, tags = tags,
            date = if (year >= 0) year else latest.date,
            trackNumber = if (trackNumber >= 0) trackNumber else latest.trackNumber,
            discNumber = if (discNumber >= 0) discNumber else latest.discNumber,
            albumArtist = albumArtist.trim()))
        notify("Información guardada sin modificar el archivo")
    }
    /** Ranked search: an exact artist + title always beats a partial coincidence. */
    suspend fun searchLocal(query: String): List<Track> {
        val clean = query.trim()
        if (clean.length < 2) return emptyList()
        val tokens = SmartSearch.normalize(clean).split(' ').filter { it.length > 1 }.distinct().take(6)
        if (tokens.isEmpty()) return emptyList()
        val where = tokens.joinToString(" OR ") {
            "(title LIKE ? ESCAPE '\\' OR customName LIKE ? ESCAPE '\\' OR artist LIKE ? ESCAPE '\\' OR album LIKE ? ESCAPE '\\' OR genre LIKE ? ESCAPE '\\' OR fileName LIKE ? ESCAPE '\\')"
        }
        val args = tokens.flatMap { token -> List(6) { "%${token.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")}%" } }
        val sql = "SELECT * FROM tracks WHERE hidden = 0 AND available = 1 AND ($where) LIMIT 600"
        val rows = withContext(Dispatchers.IO) {
            dao.candidates(androidx.sqlite.db.SimpleSQLiteQuery(sql, args.toTypedArray()))
        }
        return SmartSearch.rankItems(rows, clean) { track ->
            SmartSearch.Candidate(track.id, track.displayName, track.artist, track.album, track.source, track.plays, track.durationMs)
        }
    }

    /** Ranks what the provider returned with the same rules used for the local library. */
    fun rankOnline(results: List<com.streamvault.online.OnlineResult>, query: String): List<com.streamvault.online.OnlineResult> =
        SmartSearch.rankItems(results, query) { result ->
            SmartSearch.Candidate(result.id, result.title, result.artist, result.album, result.source, 0, (result.durationSeconds * 1000).toLong())
        }

    fun importTelegram(uri: Uri) = task {
        val text = withContext(Dispatchers.IO) {
            app.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
        } ?: error("No se pudo leer el archivo exportado")
        val summary = telegram.import(text)
        if (summary.saved == 0) error("El archivo no es una exportación de Telegram o no contiene música.")
        notify("${summary.saved} canciones de Telegram registradas · ${summary.linked} disponibles en el teléfono")
    }

    fun cover(track: Track, uri: Uri) = task {
        val cover = withContext(Dispatchers.IO) { app.artwork.import(track.id, uri) } ?: error("La imagen no es compatible")
        dao.track(track.id)?.let { dao.update(it.copy(cover = cover)) }
    }
    /**
     * The phone comes first, then the internet: the local library is searched and ranked
     * immediately, and only afterwards the online provider is queried with the same rules.
     */
    fun searchOnline(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        viewModelScope.launch {
            onlineSearching.value = true
            onlineError.value = null
            localResults.value = runCatching { searchLocal(clean) }.getOrDefault(emptyList())
            runCatching { online.search(clean) }
                .onSuccess { results ->
                    onlineResults.value = rankOnline(results, clean)
                    if (results.isEmpty() && localResults.value.isEmpty()) onlineError.value = "Sin resultados para \"$clean\". Prueba con el artista o con otra palabra."
                }
                .onFailure { onlineError.value = it.localizedMessage ?: "No se pudo buscar en Internet" }
            onlineSearching.value = false
        }
    }

    private fun wantedQuality(): Quality = Quality.values().firstOrNull { it.key == settings.value.onlineQuality } ?: Quality.AUTO

    /** The whole result page becomes the queue, so next and previous keep working online. */
    fun playOnline(result: OnlineResult) = task {
        val quality = wantedQuality()
        val context = onlineResults.value.take(30).mapNotNull { item -> runCatching { online.track(item, quality, net.value) }.getOrNull() }
        val track = online.track(result, quality, net.value)
        play(track, (listOf(track) + context).distinctBy { it.id })
    }

    fun enqueueOnline(result: OnlineResult, next: Boolean = false) = task {
        val track = online.track(result, wantedQuality(), net.value)
        enqueue(track, next)
    }

    /** Reads the real format of a local file (FLAC, bit depth, sample rate) for the player badge. */
    suspend fun formatOf(track: com.streamvault.data.Track) = AudioFormatReader.read(app, track.uri)

    /** Measures this song only, so a single quiet track can be levelled without re-analyzing everything. */
    fun measureLoudness(track: Track) = task {
        val result = withContext(Dispatchers.IO) { LoudnessAnalyzer(app).measure(track.uri, track.offset(settings.value.detectSilence)) }
        dao.loudness(track.id, result.lufs, result.peakDb)
        notify("Loudness medido: %.1f LUFS · pico %.1f dBTP. Se nivelará con el resto de tu música.".format(result.lufs, result.peakDb))
    }

    fun analyze(track: Track) = task {
        busy.value = true
        try {
            val result = app.analysis.resolve(track.id, force = true) ?: error("La pista no está disponible")
            dao.manualOffset(track.id, null)
            if (!settings.value.detectSilence) app.preferences.update { it.copy(detectSilence = true) }
            applyCurrentStart(track.id)
            notify("Inicio analizado: ${"%.2f".format(result.detectedOffsetMs / 1000.0)} s. Aplicado a la cola actual.")
        } finally { busy.value = false }
    }
    fun offset(track: Track, seconds: String?) = task {
        val value = seconds?.let(::parseAudioTime)
        if (seconds != null && (value == null || value >= track.durationMs || track.playbackEndMs?.let { value >= it } == true))
            error("Introduce segundos o mm:ss antes del final: 00:11 equivale a 11 segundos; 0.11 a 110 ms")
        dao.manualOffset(track.id, value)
        applyCurrentStart(track.id)
        notify("Inicio guardado y aplicado a la cola actual")
    }
    private suspend fun applyCurrentStart(id: String) {
        val fresh = dao.track(id) ?: return
        val c = controller ?: return
        if (c.currentMediaItem?.mediaId != id) return
        val index = c.currentMediaItemIndex
        // An explicit new start also ends the one-off "original start" override.
        c.replaceMediaItem(index, fresh.mediaItem(settings.value))
        c.seekTo(index, fresh.offset(settings.value.detectSilence))
    }
    suspend fun saveTransition(track: Track, endText: String, crossfadeText: String) {
        val latest = dao.track(track.id) ?: error("Pista no disponible")
        val end = if (endText.isBlank()) null else parseAudioTime(endText) ?: error("Final inválido: usa mm:ss o segundos")
        val seconds = if (crossfadeText.isBlank()) null else crossfadeText.toIntOrNull() ?: error("Crossfade: introduce segundos enteros")
        require(end == null || (end > latest.offset(settings.value.detectSilence) && end <= latest.durationMs)) { "El final debe estar después del inicio y dentro del archivo" }
        require(seconds == null || seconds in 0..180) { "Crossfade: de 0 a 180 segundos" }
        dao.transition(track.id, end, seconds)
        notify("Transición guardada y aplicada a la cola. El archivo original no cambia.")
    }
    fun createPlaylist(name: String, description: String, cover: String? = null) = task {
        if (name.isBlank()) error("Escribe un nombre para la playlist")
        dao.playlist(Playlist(UUID.randomUUID().toString(), name.trim(), description, cover))
    }
    fun updatePlaylist(playlist: Playlist) = task { dao.playlist(playlist) }
    fun playlistCover(playlist: Playlist, uri: Uri) = task {
        val cover = withContext(Dispatchers.IO) { app.artwork.import(playlist.id, uri) } ?: error("La imagen no es compatible")
        dao.playlist(playlist.copy(cover = cover))
    }
    fun playlistTracks(id: String) = dao.playlistTracks(id)
    fun addToPlaylist(track: Track, playlist: Playlist) = task { dao.addToPlaylist(playlist.id, track.id); notify("Agregada a ${playlist.name}") }
    fun removeFromPlaylist(track: Track, playlist: Playlist) = task { dao.removeEntry(playlist.id, track.id) }
    fun reorderPlaylist(playlist: Playlist, tracks: List<Track>) = task { dao.reorderPlaylist(playlist.id, tracks.map { it.id }) }
    fun deletePlaylist(playlist: Playlist) = task { dao.deletePlaylist(playlist.id) }
    fun enqueuePlaylist(playlist: Playlist) = task { controller?.addMediaItems(dao.getPlaylistTracks(playlist.id).map { it.mediaItem(settings.value) }); notify("Playlist agregada a la cola") }
    fun enqueueAlbum(track: Track) = task { controller?.addMediaItems(dao.albumTracks(track.album).map { it.mediaItem(settings.value) }); notify("Álbum agregado a la cola") }
    fun scan() { ScanWorker.enqueue(app) }
    fun addRoot(uri: Uri) { app.preferences.addRoot(uri.toString()); roots.value = app.preferences.roots().toList(); scan() }
    fun removeRoot(uri: String) = task {
        app.preferences.removeRoot(uri)
        runCatching { app.contentResolver.releasePersistableUriPermission(Uri.parse(uri), android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        dao.forgetRoot(uri); dao.reconcile(); roots.value = app.preferences.roots().toList()
    }
    /** Checks GitHub Releases. Automatic checks stay quiet unless a newer build exists. */
    fun checkUpdates(manual: Boolean = true) {
        if (updateState.value == UpdateState.Checking || updateState.value is UpdateState.Downloading) return
        val now = System.currentTimeMillis()
        if (!manual && (!app.preferences.state.value.autoUpdate || now - app.preferences.lastUpdateCheck < UPDATE_INTERVAL_MS)) return
        viewModelScope.launch {
            updateState.value = UpdateState.Checking
            app.preferences.lastUpdateCheck = System.currentTimeMillis()
            val info = updates.latest()
            if (info == null) {
                updateState.value = if (manual) UpdateState.Failed("No se pudo consultar GitHub. Revisa tu conexión e inténtalo otra vez.") else UpdateState.Idle
                return@launch
            }
            val current = BuildConfig.VERSION_NAME
            updateState.value = when {
                !UpdateParser.isNewer(info.version, current) -> if (manual) UpdateState.UpToDate(current) else UpdateState.Idle
                info.tag == app.preferences.skippedUpdate && !manual -> UpdateState.Idle
                else -> UpdateState.Available(info)
            }
        }
    }

    fun installUpdate(info: ReleaseInfo) {
        if (updateState.value is UpdateState.Downloading) return
        viewModelScope.launch {
            updateState.value = UpdateState.Downloading(0)
            runCatching { updates.download(info) { percent -> updateState.value = UpdateState.Downloading(percent) } }
                .onSuccess { file ->
                    if (updates.canInstall()) {
                        updateState.value = UpdateState.Idle
                        runCatching { updates.install(file) }
                            .onFailure { updateState.value = UpdateState.Failed("No se pudo abrir el instalador de Android: ${it.localizedMessage ?: "error desconocido"}") }
                    } else {
                        updateState.value = UpdateState.NeedsInstallPermission
                        updates.openInstallPermission()
                    }
                }
                .onFailure { error -> updateState.value = UpdateState.Failed(error.localizedMessage ?: "No se pudo descargar la actualización") }
        }
    }

    fun retryPendingInstall() {
        val file = updates.pending() ?: return
        if (updates.canInstall()) runCatching { updates.install(file) }
            .onFailure { notify("No se pudo abrir el instalador de Android") } else updates.openInstallPermission()
    }

    fun backupSettings(uri: Uri) = task {
        val json = app.preferences.export()
        withContext(Dispatchers.IO) {
            app.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                ?: error("No se pudo escribir el respaldo en esa ubicación")
        }
        notify("Respaldo guardado. Guárdalo donde puedas recuperarlo si reinstalas la app.")
    }

    fun restoreSettings(uri: Uri) = task {
        val text = withContext(Dispatchers.IO) {
            app.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                ?: error("No se pudo leer el archivo de respaldo")
        }
        val applied = app.preferences.import(text)
        notify("Ajustes restaurados: $applied valores.")
    }

    /** Only offered when Android itself refuses to update because the signature changed. */
    fun uninstallCurrentBuild() { updates.requestUninstall() }

    fun skipUpdate(info: ReleaseInfo) { app.preferences.skippedUpdate = info.tag; updateState.value = UpdateState.Idle }

    fun dismissUpdate() { updateState.value = UpdateState.Idle }

    fun notify(text: String) { message.value = text }
    fun task(block: suspend () -> Unit) { viewModelScope.launch { try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) { notify(e.localizedMessage ?: "Ocurrió un error") } } }
    override fun onCleared() { controller?.removeListener(listener); MediaController.releaseFuture(future); super.onCleared() }
}

private const val UPDATE_INTERVAL_MS = 6 * 60 * 60 * 1000L

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Available(val info: ReleaseInfo) : UpdateState
    data class Downloading(val percent: Int) : UpdateState
    data class UpToDate(val version: String) : UpdateState
    data class Failed(val reason: String) : UpdateState
    data class SignatureConflict(val detail: String?) : UpdateState
    data object NeedsInstallPermission : UpdateState
}

data class LibraryFilter(val source: String = "", val favorite: Boolean = false, val folder: String = "", val artist: String = "", val album: String = "", val genre: String = "", val sort: String = "title")
data class QueueTrack(val id: String, val title: String, val artist: String, val cover: String?, val duration: Long, val album: String = "")
data class PlaybackState(val currentId: String = "", val index: Int = 0, val playing: Boolean = false, val position: Long = 0, val duration: Long = 0, val queue: List<QueueTrack> = emptyList(), val buffering: Boolean = false)
