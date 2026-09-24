package com.streamvault.processing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.streamvault.LuminaApp
import com.streamvault.R
import com.streamvault.data.AudioLocation
import com.streamvault.data.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/** No fake progress, no center-channel cancellation posing as ML. Pluggable provider only. */
class SeparationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as LuminaApp
        val source = app.library.track(inputData.getString("track") ?: return@withContext Result.failure())
            ?: return@withContext Result.failure()
        val provider = app.separation.provider
        val available = provider.availability(app)
        if (available is Availability.Unavailable) return@withContext Result.failure(workDataOf("error" to available.reason))
        val workDirectory = File(app.cacheDir, "separation/$id").apply { mkdirs() }
        try {
            setForeground(getForegroundInfo())
            var result: SeparationEvent.Complete? = null
            provider.separate(app, Uri.parse(source.uri), workDirectory).collect { event ->
                when (event) {
                    is SeparationEvent.Progress -> setProgress(workDataOf("percent" to event.percent.coerceIn(0, 100), "stage" to event.stage.take(500)))
                    is SeparationEvent.Complete -> { check(result == null) { "El proveedor devolvió más de un resultado" }; result = event }
                    is SeparationEvent.Failed -> error(event.message)
                }
            }
            val output = result ?: error("El proveedor terminó sin generar archivos")
            check(output.instrumental.canonicalFile != output.vocals.canonicalFile) { "Las pistas separadas deben ser archivos distintos" }
            val instrumental = importOutput(app, source, output.instrumental, workDirectory, "Instrumental", "instrumental")
            val vocals = importOutput(app, source, output.vocals, workDirectory, "Voz", "vocal")
            Result.success(workDataOf("instrumental" to instrumental, "vocals" to vocals, "percent" to 100))
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(workDataOf("error" to (e.localizedMessage ?: "Falló la separación"))) }
        finally { workDirectory.deleteRecursively() }
    }
    private suspend fun importOutput(app: LuminaApp, original: Track, output: File, staging: File, suffix: String, source: String): String {
        check(output.canonicalPath.startsWith(staging.canonicalPath + File.separator)) { "El proveedor debe escribir en su carpeta temporal" }
        check(output.isFile && output.length() > 0) { "El archivo de salida está vacío" }
        val digest = MessageDigest.getInstance("SHA-256")
        output.inputStream().use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) { currentCoroutineContext().ensureActive(); val size = input.read(buffer); if (size < 0) break; digest.update(buffer, 0, size) }
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        check(hash != original.id) { "El proveedor devolvió el original en vez de una pista separada" }
        val extension = output.extension.lowercase()
        check(extension in setOf("wav", "flac", "mp3", "m4a", "ogg", "opus")) { "Contenedor de salida no compatible" }
        val metadata = MediaMetadataRetriever()
        val duration = try { metadata.setDataSource(output.path); metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0 } finally { metadata.release() }
        check(duration > 0) { "La salida no es un audio válido" }
        val exported = File(app.filesDir, "exports/$hash.$extension").apply { parentFile!!.mkdirs() }
        if (!exported.exists()) output.copyTo(exported)
        val title = "${original.displayName} - $suffix"
        val uri = Uri.fromFile(exported).toString()
        app.library.insert(Track(hash, uri, "$title.$extension", title, artist = original.artist, album = original.album,
            genre = original.genre, folder = "Denilson/Procesados", durationMs = duration, size = exported.length(),
            date = System.currentTimeMillis(), cover = original.cover, source = source))
        app.library.location(AudioLocation(uri, hash, "generated", exported.length(), exported.lastModified(), "generated"))
        return hash
    }
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val context = applicationContext
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("processing", "Procesamiento de audio", NotificationManager.IMPORTANCE_LOW))
        val notification = NotificationCompat.Builder(context, "processing").setSmallIcon(R.drawable.ic_lumina).setContentTitle("Procesando audio…")
            .setContentText("El archivo original se conserva").setOngoing(true)
            .addAction(0, "Cancelar", WorkManager.getInstance(context).createCancelPendingIntent(id)).build()
        return if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(42, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else ForegroundInfo(42, notification)
    }
    companion object {
        fun enqueue(context: Context, track: Track) = WorkManager.getInstance(context).enqueueUniqueWork("separate-${track.id}", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SeparationWorker>().setInputData(workDataOf("track" to track.id)).build())
    }
}
