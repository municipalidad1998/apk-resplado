package com.streamvault.scanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.streamvault.LuminaApp
import com.streamvault.R
import com.streamvault.analysis.AnalysisWorker
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class ScanWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as LuminaApp
        return try {
            setForeground(getForegroundInfo())
            val warnings = AudioScanner(app) { count, message -> setProgress(workDataOf("count" to count, "message" to message.take(3000))) }.scan()
            if (app.preferences.state.value.detectSilence) AnalysisWorker.enqueue(app)
            Result.success(workDataOf("message" to if (warnings.isEmpty()) "Biblioteca actualizada" else warnings.joinToString("\n").take(3000)))
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(workDataOf("message" to (e.localizedMessage ?: "No se pudo actualizar"))) }
    }
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("scan", "Biblioteca", NotificationManager.IMPORTANCE_LOW))
        val cancel = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        val notification = NotificationCompat.Builder(applicationContext, "scan").setSmallIcon(R.drawable.ic_lumina)
            .setContentTitle("Escaneando biblioteca…").setContentText("Metadatos y archivos locales")
            .setOngoing(true).addAction(0, "Cancelar", cancel).build()
        return if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(41, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else ForegroundInfo(41, notification)
    }
    companion object {
        fun enqueue(context: Context) = WorkManager.getInstance(context).enqueueUniqueWork("scan", ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<ScanWorker>().build())
        fun schedule(context: Context, enabled: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (enabled) wm.enqueueUniquePeriodicWork("periodic-scan", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ScanWorker>(6, TimeUnit.HOURS).build())
            else wm.cancelUniqueWork("periodic-scan")
        }
    }
}
