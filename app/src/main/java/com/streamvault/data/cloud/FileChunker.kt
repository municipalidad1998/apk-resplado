package com.streamvault.data.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Fragmentación de archivos grandes para Telegram (límite de 2 GB
 * por mensaje en cuentas estándar).
 *
 * Un archivo de 10 GB → PELICULA.part001, .part002, …
 * El usuario siempre ve un solo archivo; los fragmentos se
 * reconstruyen automáticamente y se verifican con SHA-256.
 */
object FileChunker {

    const val CHUNK_SIZE = 1_900L * 1024L * 1024L // ~1.9 GB por fragmento

    data class ChunkInfo(val file: File, val index: Int, val sha256: String, val size: Long)

    suspend fun sha256(file: File): String = withContext(Dispatchers.IO) {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(256 * 1024)
            var read: Int
            while (input.read(buf).also { read = it } != -1) md.update(buf, 0, read)
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }

    /** Divide el archivo en fragmentos; devuelve la lista ordenada con su hash. */
    suspend fun split(source: File, outDir: File): List<ChunkInfo> = withContext(Dispatchers.IO) {
        outDir.mkdirs()
        val chunks = mutableListOf<ChunkInfo>()
        FileInputStream(source).use { input ->
            val buf = ByteArray(256 * 1024)
            var index = 1
            var remaining = source.length()
            while (remaining > 0) {
                val chunkFile = File(outDir, "%s.part%03d".format(source.name, index))
                val md = MessageDigest.getInstance("SHA-256")
                var written = 0L
                FileOutputStream(chunkFile).use { out ->
                    while (written < CHUNK_SIZE && remaining > 0) {
                        val toRead = minOf(buf.size.toLong(), CHUNK_SIZE - written, remaining).toInt()
                        val read = input.read(buf, 0, toRead)
                        if (read == -1) break
                        out.write(buf, 0, read)
                        md.update(buf, 0, read)
                        written += read
                        remaining -= read
                    }
                }
                chunks.add(ChunkInfo(chunkFile, index, md.digest().joinToString("") { "%02x".format(it) }, written))
                index++
            }
        }
        chunks
    }

    /** Reconstruye el archivo original a partir de sus fragmentos ordenados. */
    suspend fun join(chunks: List<File>, dest: File): Unit = withContext(Dispatchers.IO) {
        FileOutputStream(dest).use { out ->
            val buf = ByteArray(256 * 1024)
            chunks.sortedBy { it.name }.forEach { chunk ->
                FileInputStream(chunk).use { input ->
                    var read: Int
                    while (input.read(buf).also { read = it } != -1) out.write(buf, 0, read)
                }
            }
        }
    }

    /** Verifica que el archivo reconstruido coincide con el hash original. */
    suspend fun verify(file: File, expectedSha256: String): Boolean =
        sha256(file).equals(expectedSha256, ignoreCase = true)
}
