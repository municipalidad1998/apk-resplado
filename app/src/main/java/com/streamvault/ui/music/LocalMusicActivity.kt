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
            onLongClick = { song -> showMetadataEditor(song) })
        findViewById<RecyclerView>(R.id.rvSongs).adapter = adapter
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

    data class SimpleItem(val name: String, val uri: Uri, val mime: String?)

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
        val onLongClick: (LocalSong) -> Unit = {}
    ) : RecyclerView.Adapter<SongAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val art: ImageView = v.findViewById(R.id.ivSongArt)
            val title: TextView = v.findViewById(R.id.tvSongTitle)
            val artist: TextView = v.findViewById(R.id.tvSongArtist)
            val duration: TextView = v.findViewById(R.id.tvSongDuration)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_song, p, false))

        override fun getItemCount() = songs.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val s = songs[pos]
            h.title.text = s.title
            h.artist.text = "${s.artist} · ${s.album}"
            val totalSec = s.duration / 1000
            h.duration.text = "%d:%02d".format(totalSec / 60, totalSec % 60)
            Glide.with(h.art).load(s.albumArtUri)
                .placeholder(R.drawable.ic_channel)
                .error(R.drawable.ic_channel).into(h.art)
            h.itemView.setOnClickListener { onClick(h.bindingAdapterPosition, songs) }
            h.itemView.setOnLongClickListener {
                songs.getOrNull(h.bindingAdapterPosition)?.let(onLongClick)
                true
            }
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
