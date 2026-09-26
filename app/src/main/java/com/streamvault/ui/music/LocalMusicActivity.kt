package com.streamvault.ui.music

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.streamvault.R
import com.streamvault.StreamVaultApp
import com.streamvault.data.local.LocalMusicProvider
import com.streamvault.data.local.LocalSong
import com.streamvault.playback.PlaybackStateHolder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 📱 MÚSICA DEL TELÉFONO — biblioteca 100% local e independiente.
 * El buscador actúa EXCLUSIVAMENTE sobre archivos físicos del
 * teléfono (MediaStore). Sin Internet, sin YouTube, sin APIs.
 *
 * Modos adicionales (misma pantalla, otra fuente MediaStore):
 * 🎬 Videos locales | 🖼 Fotos locales | 📄 Documentos locales.
 */
class LocalMusicActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_MUSIC = "music"
        const val MODE_VIDEOS = "videos"
        const val MODE_PHOTOS = "photos"
        const val MODE_DOCS = "docs"
        const val MODE_PLAYLISTS = "playlists"
        const val MODE_FAVORITES = "favorites"
        const val MODE_HISTORY = "history"
        private const val REQ_PERM = 1001
    }

    private lateinit var provider: LocalMusicProvider
    private var allSongs: List<LocalSong> = emptyList()
    private lateinit var adapter: SongAdapter
    private val mode: String by lazy { intent.getStringExtra(EXTRA_MODE) ?: MODE_MUSIC }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_music)
        provider = LocalMusicProvider(this)

        findViewById<View>(R.id.btnBackMusic).setOnClickListener { finish() }
        findViewById<RecyclerView>(R.id.rvSongs).apply {
            layoutManager = LinearLayoutManager(this@LocalMusicActivity)
        }

        when (mode) {
            MODE_MUSIC -> {
                findViewById<TextView>(R.id.tvMusicTitle).text = "📱 Música del teléfono"
                setupSearch()
                setupOrganizer()
                checkPermissionAndLoad()
            }
            MODE_VIDEOS -> {
                findViewById<TextView>(R.id.tvMusicTitle).text = "🎬 Videos del teléfono"
                findViewById<EditText>(R.id.etSearchMusic).hint = "Buscar en videos locales…"
                loadSimpleMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*")
            }
            MODE_PHOTOS -> {
                findViewById<TextView>(R.id.tvMusicTitle).text = "🖼 Fotos del teléfono"
                findViewById<EditText>(R.id.etSearchMusic).hint = "Buscar en fotos locales…"
                loadSimpleMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            }
            MODE_DOCS -> {
                findViewById<TextView>(R.id.tvMusicTitle).text = "📄 Documentos del teléfono"
                findViewById<EditText>(R.id.etSearchMusic).hint = "Buscar en documentos locales…"
                loadSimpleMedia(MediaStore.Files.getContentUri("external"), null)
            }
            MODE_FAVORITES -> {
                findViewById<TextView>(R.id.tvMusicTitle).text = "❤️ Favoritos locales"
                lifecycleScope.launch {
                    StreamVaultApp.db.musicFavoritesDao().bySource("LOCAL").collectLatest { favs ->
                        val songs = favs.map { f ->
                            LocalSong(
                                id = 0, title = f.title, artist = f.artist ?: "Desconocido",
                                album = f.album ?: "", duration = f.duration,
                                contentUri = f.uri, albumArtUri = f.artUri
                            )
                        }
                        if (songs.isEmpty()) showEmpty("Sin favoritos aún.\nToca ♥ en el reproductor.")
                        else showSongs(songs)
                    }
                }
            }
            MODE_HISTORY -> {
                findViewById<TextView>(R.id.tvMusicTitle).text = "🕒 Historial local"
                lifecycleScope.launch {
                    StreamVaultApp.db.historyDao().recent().collectLatest { hist ->
                        val local = hist.filter { it.source == "LOCAL" }
                        val songs = local.map { x ->
                            LocalSong(
                                id = 0, title = x.title, artist = x.artist ?: "Desconocido",
                                album = "", duration = x.durationMs,
                                contentUri = x.uri, albumArtUri = x.artUri
                            )
                        }
                        if (songs.isEmpty()) showEmpty("Sin reproducciones aún.")
                        else showSongs(songs)
                    }
                }
            }
        }
    }

    // ---------------- Música local ----------------

    /** Organización: Canciones / Álbumes / Artistas / Géneros / Carpetas. */
    private fun setupOrganizer() {
        val sp = findViewById<Spinner>(R.id.spOrganize)
        sp.visibility = View.VISIBLE
        val modes = listOf("🎵 Canciones", "💿 Álbumes", "🎤 Artistas", "🏷 Géneros", "📁 Carpetas")
        sp.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes)
        sp.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (allSongs.isNotEmpty()) applyOrganization(pos)
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }
    }

    private fun applyOrganization(pos: Int) {
        val query = findViewById<EditText>(R.id.etSearchMusic).text.toString()
        val base = if (query.isBlank()) allSongs else LocalMusicProvider.search(allSongs, query)
        when (pos) {
            0 -> showSongs(base) // canciones
            1 -> showGroups(base.groupBy { it.album }, "💿")
            2 -> showGroups(base.groupBy { it.artist }, "🎤")
            3 -> showGroups(base.groupBy { it.genre ?: "Sin género" }, "🏷")
            4 -> showGroups(base.groupBy { it.path?.substringBeforeLast('/') ?: "Desconocida" }, "📁")
        }
    }

    private fun showGroups(
        grouped: Map<String, List<LocalSong>>,
        icon: String
    ) {
        val groups = grouped.toSortedMap(String.CASE_INSENSITIVE_ORDER)
        val items = groups.map { (name, songs) ->
            SimpleItem("$icon $name (${songs.size})", android.net.Uri.EMPTY, null, songs)
        }
        findViewById<TextView>(R.id.tvEmptyMusic).visibility =
            if (items.isEmpty()) View.VISIBLE else View.GONE
        findViewById<RecyclerView>(R.id.rvSongs).adapter = SimpleAdapter(items) { item ->
            val songs = item.groupSongs ?: return@SimpleAdapter
            showSongs(songs) // tocar un álbum/artista/género/carpeta muestra sus canciones
        }
    }

    private fun setupSearch() {
        findViewById<EditText>(R.id.etSearchMusic).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                val q = s.toString()
                if (q.isBlank()) showSongs(allSongs)
                else showSongs(LocalMusicProvider.search(allSongs, q))
            }
        })
    }

    private fun audioPermission(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun checkPermissionAndLoad() {
        if (ContextCompat.checkSelfPermission(this, audioPermission()) == PackageManager.PERMISSION_GRANTED) {
            loadSongs()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(audioPermission()), REQ_PERM)
        }
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (code == REQ_PERM && results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) {
            loadSongs()
        } else {
            showEmpty("Se necesita permiso de audio para mostrar\nla música del teléfono.")
        }
    }

    private fun loadSongs() {
        showEmpty("Escaneando música local…")
        lifecycleScope.launch {
            allSongs = provider.scanAll()
            if (allSongs.isEmpty()) showEmpty("No se encontró música en el teléfono.")
            else showSongs(allSongs)
        }
    }

    private fun showSongs(list: List<LocalSong>) {
        findViewById<TextView>(R.id.tvEmptyMusic).visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        if (list.isEmpty() && allSongs.isNotEmpty())
            findViewById<TextView>(R.id.tvEmptyMusic).text = "Sin resultados en tu biblioteca local."
        adapter = SongAdapter(list,
            onClick = { pos, songs -> openPlayer(songs, pos) },
            onLongClick = { song -> showMetadataEditor(song) },
            onMenu = { song -> showSongMenu(song) })
        findViewById<RecyclerView>(R.id.rvSongs).adapter = adapter
    }

    /** Menú ⋮ de canción: reproducir, cola, playlist, favoritos, info… */
    private fun showSongMenu(s: LocalSong) {
        val options = arrayOf(
            "▶ Reproducir ahora", "➕ Agregar a cola",
            "🎶 Agregar a playlist", "❤️ Agregar a favoritos", "ℹ Información"
        )
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle(s.title).setItems(options) { _, which ->
            when (which) {
                0 -> openPlayer(listOf(s), 0)
                1 -> addToQueue(s)
                2 -> addSongToPlaylist(s)
                3 -> lifecycleScope.launch {
                    StreamVaultApp.db.musicFavoritesDao().insert(
                        com.streamvault.data.db.MusicFavoriteEntity(
                            uri = s.contentUri, title = s.title, artist = s.artist,
                            album = s.album, artUri = s.albumArtUri, duration = s.duration
                        )
                    )
                    Toast.makeText(this@LocalMusicActivity, "❤️ Agregada a favoritos", Toast.LENGTH_SHORT).show()
                }
                4 -> {
                    // Detalles de audio (bit depth / sample rate / bitrate / codec)
                    var sampleRate = ""; var bitrate = ""
                    try {
                        val mmr = android.media.MediaMetadataRetriever()
                        mmr.setDataSource(this, Uri.parse(s.contentUri))
                        if (android.os.Build.VERSION.SDK_INT >= 31) {
                            mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                                ?.let { sampleRate = "${it.toInt() / 1000} kHz" }
                        }
                        mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE)
                            ?.let { bitrate = "${it.toInt() / 1000} kbps" }
                        mmr.release()
                    } catch (e: Exception) { }
                    val sec = s.duration / 1000
                    val quality = s.codecLabel
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("ℹ ${s.title}")
                        .setMessage(
                            "Artista: ${s.artist}\nÁlbum: ${s.album}\n" +
                            (s.genre?.let { "Género: $it\n" } ?: "") +
                            (s.year?.let { "Año: $it\n" } ?: "") +
                            (s.track?.let { "Pista: $it\n" } ?: "") +
                            "Duración: %d:%02d".format(sec / 60, sec % 60) + "\n" +
                            if (quality.isNotEmpty()) "Calidad: $quality\n" else "" +
                            if (sampleRate.isNotEmpty()) "Sample rate: $sampleRate\n" else "" +
                            if (bitrate.isNotEmpty()) "Bitrate: $bitrate" else ""
                        )
                        .setPositiveButton("Cerrar", null).show()
                }
            }
        }.show()
    }

    private fun addToQueue(s: LocalSong) {
        PlaybackStateHolder.queue = PlaybackStateHolder.queue + s
        val token = androidx.media3.session.SessionToken(
            this, android.content.ComponentName(this, com.streamvault.playback.MusicPlayerService::class.java)
        )
        val future = androidx.media3.session.MediaController.Builder(this, token).buildAsync()
        future.addListener({
            val c = future.get()
            c.addMediaItem(
                androidx.media3.common.MediaItem.Builder().setMediaId(s.contentUri)
                    .setUri(s.contentUri)
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(s.title).setArtist(s.artist).setAlbumTitle(s.album).build()
                    ).build()
            )
            androidx.media3.session.MediaController.releaseFuture(future)
            Toast.makeText(this, "➕ Agregada a la cola", Toast.LENGTH_SHORT).show()
        }, com.google.common.util.concurrent.MoreExecutors.directExecutor())
    }

    private fun addSongToPlaylist(s: LocalSong) {
        val dao = StreamVaultApp.db.playlistDao()
        lifecycleScope.launch {
            dao.bySource("LOCAL").collectLatest { playlists ->
                if (playlists.isEmpty()) {
                    Toast.makeText(this@LocalMusicActivity, "Crea una playlist local primero", Toast.LENGTH_LONG).show()
                    return@collectLatest
                }
                val names = playlists.map { it.name }.toTypedArray()
                androidx.appcompat.app.AlertDialog.Builder(this@LocalMusicActivity)
                    .setTitle("Agregar a…").setItems(names) { _, which ->
                        lifecycleScope.launch {
                            dao.insertTrack(
                                com.streamvault.data.db.PlaylistTrackEntity(
                                    playlistId = playlists[which].id, title = s.title,
                                    artist = s.artist, uri = s.contentUri,
                                    artUri = s.albumArtUri, duration = s.duration
                                )
                            )
                            Toast.makeText(this@LocalMusicActivity, "Agregada a ${playlists[which].name}", Toast.LENGTH_SHORT).show()
                        }
                    }.setNegativeButton("Cancelar", null).show()
                return@collectLatest
            }
        }
    }

    /** Edición de metadatos cuando el archivo los tiene incompletos. */
    private fun showMetadataEditor(song: LocalSong) {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val etTitle = EditText(this).apply { setText(song.title); hint = "Título" }
        val etArtist = EditText(this).apply { setText(song.artist); hint = "Artista" }
        val etAlbum = EditText(this).apply { setText(song.album); hint = "Álbum" }
        view.addView(etTitle); view.addView(etArtist); view.addView(etAlbum)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("✏️ Editar metadatos")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                try {
                    val values = android.content.ContentValues().apply {
                        put(MediaStore.Audio.Media.TITLE, etTitle.text.toString())
                        put(MediaStore.Audio.Media.ARTIST, etArtist.text.toString())
                        put(MediaStore.Audio.Media.ALBUM, etAlbum.text.toString())
                    }
                    val rows = contentResolver.update(Uri.parse(song.contentUri), values, null, null)
                    Toast.makeText(this,
                        if (rows > 0) "Metadatos actualizados" else "No se pudo escribir en el archivo",
                        Toast.LENGTH_SHORT).show()
                    if (rows > 0) loadSongs()
                } catch (e: Exception) {
                    Toast.makeText(this, "Android requiere confirmar el permiso de escritura para este archivo", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openPlayer(songs: List<LocalSong>, pos: Int) {
        startActivity(Intent(this, PlayerMusicActivity::class.java).apply {
            putParcelableArrayListExtra(PlayerMusicActivity.EXTRA_QUEUE, ArrayList(songs))
            putExtra(PlayerMusicActivity.EXTRA_INDEX, pos)
        })
    }

    // ---------------- Videos / Fotos / Documentos ----------------

    data class SimpleItem(
        val name: String, val uri: Uri, val mime: String?,
        val groupSongs: List<LocalSong>? = null
    )

    private fun loadSimpleMedia(collection: Uri, mimeFallback: String?) {
        showEmpty("Escaneando…")
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.MIME_TYPE)
        contentResolver.query(collection, projection, null, null, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { c ->
            val items = mutableListOf<SimpleItem>()
            val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeCol = c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            while (c.moveToNext() && items.size < 500) {
                val mime = if (mimeCol >= 0) c.getString(mimeCol) else null
                items.add(SimpleItem(
                    c.getString(nameCol) ?: "Archivo",
                    android.content.ContentUris.withAppendedId(collection, c.getLong(idCol)),
                    mime ?: mimeFallback
                ))
            }
            runOnUiThread {
                if (items.isEmpty()) showEmpty("No se encontraron archivos.")
                else {
                    findViewById<TextView>(R.id.tvEmptyMusic).visibility = View.GONE
                    findViewById<RecyclerView>(R.id.rvSongs).adapter = SimpleAdapter(items) { item ->
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(item.uri, item.mime ?: "*/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            })
                        } catch (e: Exception) {
                            Toast.makeText(this, "No hay app para abrir este archivo", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun showEmpty(msg: String) {
        findViewById<TextView>(R.id.tvEmptyMusic).apply { text = msg; visibility = View.VISIBLE }
    }

    // ---------------- Adapters ----------------

    class SongAdapter(
        val songs: List<LocalSong>,
        val onClick: (Int, List<LocalSong>) -> Unit,
        val onLongClick: (LocalSong) -> Unit = {},
        val onMenu: (LocalSong) -> Unit = {}
    ) : RecyclerView.Adapter<SongAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val art: ImageView = v.findViewById(R.id.ivSongArt)
            val title: TextView = v.findViewById(R.id.tvSongTitle)
            val artist: TextView = v.findViewById(R.id.tvSongArtist)
            val duration: TextView = v.findViewById(R.id.tvSongDuration)
            val menu: TextView = v.findViewById(R.id.tvSongMenu)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_song, p, false))

        override fun getItemCount() = songs.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val s = songs[pos]
            h.title.text = s.title
            h.artist.text = "${s.artist} · ${s.album}"
            val totalSec = s.duration / 1000
            val badge = s.codecLabel
            h.duration.text = if (badge.isNotEmpty())
                "$badge\n%d:%02d".format(totalSec / 60, totalSec % 60)
            else "%d:%02d".format(totalSec / 60, totalSec % 60)
            Glide.with(h.art).load(s.albumArtUri)
                .placeholder(R.drawable.ic_channel)
                .error(R.drawable.ic_channel).into(h.art)
            h.menu.visibility = View.VISIBLE
            h.itemView.setOnClickListener { onClick(h.bindingAdapterPosition, songs) }
            h.itemView.setOnLongClickListener {
                songs.getOrNull(h.bindingAdapterPosition)?.let(onLongClick)
                true
            }
            h.menu.setOnClickListener { songs.getOrNull(h.bindingAdapterPosition)?.let(onMenu) }
        }
    }

    class SimpleAdapter(val items: List<SimpleItem>, val onClick: (SimpleItem) -> Unit) :
        RecyclerView.Adapter<SimpleAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val art: ImageView = v.findViewById(R.id.ivSongArt)
            val title: TextView = v.findViewById(R.id.tvSongTitle)
            val sub: TextView = v.findViewById(R.id.tvSongArtist)
            val dur: TextView = v.findViewById(R.id.tvSongDuration)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_song, p, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = items[pos]
            h.title.text = item.name
            h.sub.text = item.mime ?: "archivo"
            h.dur.text = ""
            Glide.with(h.art).load(item.uri).placeholder(R.drawable.ic_film).error(R.drawable.ic_film).into(h.art)
            h.itemView.setOnClickListener { onClick(item) }
        }
    }
}
