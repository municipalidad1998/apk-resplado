package com.streamvault.analysis

import android.content.Context
import androidx.work.*
import com.streamvault.LuminaApp
import kotlinx.coroutines.CancellationException

/** Bounded batches keep analysis resumable and below WorkManager's execution limit. */
class AnalysisWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as LuminaApp
        val settings = app.preferences.state.value
        if (!settings.detectSilence) return Result.success()
        val dao = app.library
        val tracks = dao.pendingAnalysis(settings.analysisKey)
        for (track in tracks) {
            if (isStopped) return Result.retry()
            try {
                app.analysis.resolve(track.id, force = true)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Repository records failure separately; manual retry stays available. */ }
        }
        if (tracks.isNotEmpty()) WorkManager.getInstance(app).enqueueUniqueWork("analysis", ExistingWorkPolicy.APPEND_OR_REPLACE, request())
        return Result.success()
    }
    companion object {
        private fun request() = OneTimeWorkRequestBuilder<AnalysisWorker>().setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build()).build()
        fun enqueue(context: Context) { WorkManager.getInstance(context).enqueueUniqueWork("analysis", ExistingWorkPolicy.KEEP, request()) }
    }
}
