package com.streamvault.artwork

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/** Decode sampled bitmaps; never keep original embedded album pictures in memory/cache. */
class ArtworkStore(private val context: Context) {
    private val directory get() = File(context.filesDir, "covers").apply { mkdirs() }
    fun embedded(id: String, bytes: ByteArray?): String? {
        if (bytes == null || bytes.size > 20 * 1024 * 1024) return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        options.inSampleSize = sample(options.outWidth, options.outHeight)
        options.inJustDecodeBounds = false
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        return save(id, bitmap)
    }
    fun import(id: String, uri: Uri): String? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        options.inSampleSize = sample(options.outWidth, options.outHeight)
        options.inJustDecodeBounds = false
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        return save("$id-${System.currentTimeMillis()}", bitmap)
    }
    private fun sample(w: Int, h: Int): Int { var n = 1; while (maxOf(w, h) / n > 768) n *= 2; return n }
    private fun save(id: String, bitmap: Bitmap): String {
        val file = File(directory, "$id.jpg")
        try { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) } } finally { bitmap.recycle() }
        return file.toURI().toString()
    }
}
