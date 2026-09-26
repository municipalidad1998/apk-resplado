package com.streamvault.ui.telegram

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.streamvault.R
import com.streamvault.StreamVaultApp
import com.streamvault.data.cloud.FileChunker
import com.streamvault.data.db.TelegramChunkEntity
import com.streamvault.data.db.TelegramFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ☁ Telegram Cloud — archivos almacenados en el canal privado.
 * - Los archivos sobreviven aunque se borren del dispositivo.
 * - Subida con fragmentación automática (chunks ~1.9 GB + SHA-256).
 * - Descarga y restauración al dispositivo.
 */
class TelegramCloudActivity : AppCompatActivity() {

    private val dao by lazy { StreamVaultApp.db.telegramFilesDao() }
    private val client by lazy { StreamVaultApp.telegramClient }

    private val pickLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri -> uploadUri(uri) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telegram_cloud)
        findViewById<View>(R.id.btnBackCloud).setOnClickListener { finish() }

        findViewById<RecyclerView>(R.id.rvCloud).layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.btnUploadCloud).setOnClickListener {
            pickLauncher.launch(arrayOf("*/*"))
        }

        lifecycleScope.launch {
            dao.all().collectLatest { files ->
                findViewById<RecyclerView>(R.id.rvCloud).adapter = CloudAdapter(files) { f ->
                    showFileActions(f)
                }
                findViewById<TextView>(R.id.tvEmptyCloud).visibility =
                    if (files.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    // ---------- Subida con fragmentación ----------

    private fun uploadUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val name = queryName(uri) ?: "archivo"
                // Copiar a caché para tener una ruta de archivo real
                val tmp = File(cacheDir, "upload_$name")
                contentResolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } }
                    ?: return@launch

                val fullHash = FileChunker.sha256(tmp)
                val chunks = FileChunker.split(tmp, File(cacheDir, "chunks"))
                tmp.delete()

                val entity = TelegramFileEntity(
                    fileUid = fullHash, originalName = name, localPath = null,
                    category = categoryFor(name), sizeBytes = chunks.sumOf { it.size },
                    sha256 = fullHash
                )
                dao.insert(entity)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TelegramCloudActivity,
                        "☁ Subiendo \"$name\" (${chunks.size} fragmento(s))…", Toast.LENGTH_LONG).show()
                }

                for (chunk in chunks) {
                    // Reintentos básicos por fragmento
                    var ok: Pair<Long, String>? = null
                    repeat(3) {
                        if (ok == null) ok = client.uploadFile(chunk.file)
                    }
                    ok ?: break
                    dao.insertChunk(
                        TelegramChunkEntity(
                            fileUid = fullHash, chunkIndex = chunk.index,
                            telegramMessageId = ok!!.first, telegramFileId = ok!!.second,
                            sha256 = chunk.sha256, sizeBytes = chunk.size, uploaded = true
                        )
                    )
                    chunk.file.delete()
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TelegramCloudActivity, "☁ \"$name\" respaldado en Telegram", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TelegramCloudActivity, "Error al subir: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ---------- Descarga / restauración ----------

    private fun showFileActions(f: TelegramFileEntity) {
        AlertDialog.Builder(this)
            .setTitle(f.originalName)
            .setMessage("☁ ${f.sizeBytes / 1024 / 1024} MB · ${f.category}\nSHA-256: ${f.sha256.take(16)}…")
            .setPositiveButton("Descargar / restaurar") { _, _ -> downloadFile(f) }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun downloadFile(f: TelegramFileEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val chunks = dao.chunks(f.fileUid)
                if (chunks.isEmpty()) throw Exception("Sin fragmentos registrados")
                val parts = mutableListOf<File>()
                for (c in chunks.sortedBy { it.chunkIndex }) {
                    val path = client.downloadRemoteFile(c.telegramFileId)
                        ?: throw Exception("Fallo al descargar fragmento ${c.chunkIndex}")
                    val partFile = File(path)
                    if (!FileChunker.verify(partFile, c.sha256))
                        throw Exception("SHA-256 no coincide en fragmento ${c.chunkIndex}")
                    parts.add(partFile)
                }
                val dest = File(getExternalFilesDir(null), f.originalName)
                FileChunker.join(parts, dest)
                if (!FileChunker.verify(dest, f.sha256)) throw Exception("Verificación final falló")
                dao.insert(f.copy(localPath = dest.absolutePath, deletedFromLocal = false))
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TelegramCloudActivity, "✅ Restaurado: ${dest.absolutePath}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TelegramCloudActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun queryName(uri: Uri): String? =
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst() && i >= 0) c.getString(i) else null
        }

    private fun categoryFor(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "tiff", "heic") -> "PHOTO"
            in listOf("mp4", "mkv", "avi", "mov", "webm", "ts", "flv", "m4v") -> "VIDEO"
            in listOf("mp3", "flac", "wav", "ogg", "aac", "m4a", "opus") -> "MUSIC"
            in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "epub") -> "DOC"
            else -> "ARCHIVE"
        }
    }

    class CloudAdapter(
        private val items: List<TelegramFileEntity>,
        private val onClick: (TelegramFileEntity) -> Unit
    ) : RecyclerView.Adapter<CloudAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: TextView = v.findViewById(R.id.tvHubIcon)
            val title: TextView = v.findViewById(R.id.tvHubTitle)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_hub, p, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val f = items[pos]
            h.icon.text = when (f.category) {
                "PHOTO" -> "🖼"; "VIDEO" -> "🎬"; "MUSIC" -> "🎵"; "DOC" -> "📄"; else -> "📦"
            }
            h.title.text = "${f.originalName}\n${f.sizeBytes / 1024 / 1024} MB" +
                    if (f.deletedFromLocal) " · solo en la nube" else ""
            h.itemView.setOnClickListener { onClick(f) }
        }
    }
}
