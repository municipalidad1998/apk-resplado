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
        val analyzer = SilenceAnalyzer(app)
        for (track in tracks) {
            if (isStopped) return Result.retry()
            try {
                val result = analyzer.analyze(track.uri, settings.thresholdDb, settings.minimumSilence)
                dao.analysis(track.id, result.offsetMs, result.waveform, settings.analysisKey)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { dao.analysis(track.id, 0, "", settings.analysisKey) }
        }
        if (tracks.isNotEmpty()) WorkManager.getInstance(app).enqueueUniqueWork("analysis", ExistingWorkPolicy.APPEND_OR_REPLACE, request())
        return Result.success()
    }
    companion object {
        private fun request() = OneTimeWorkRequestBuilder<AnalysisWorker>().setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build()).build()
        fun enqueue(context: Context) { WorkManager.getInstance(context).enqueueUniqueWork("analysis", ExistingWorkPolicy.KEEP, request()) }
    }
}
