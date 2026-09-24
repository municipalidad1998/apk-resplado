package com.streamvault.analysis

import com.streamvault.LuminaApp
import com.streamvault.data.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/** The playback request and the background worker share the same cache/locks. Never cache a decoder failure as silence. */
class AnalysisRepository(private val app: LuminaApp) {
    private val locks = Array(32) { Mutex() }
    suspend fun resolve(id: String, force: Boolean = false): Track? = locks[(id.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
        val track = app.library.track(id) ?: return@withLock null
        val settings = app.preferences.state.value
        if (!force && (!settings.detectSilence || track.manualOffsetMs != null || track.analysisKey == settings.analysisKey)) return@withLock track
        if (!force && track.analysisKey == "failed:${settings.analysisKey}") error("No se pudo analizar ${track.displayName}. Reintenta desde Inicio y transición o fija 00:11 manualmente.")
        try {
            val result = withTimeout(45_000) { SilenceAnalyzer(app).analyze(track.uri, settings.thresholdDb, settings.minimumSilence) }
            app.library.analysis(id, result.offsetMs, result.waveform, settings.analysisKey)
            app.library.track(id)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            app.library.analysis(id, track.detectedOffsetMs, track.waveform, "failed:${settings.analysisKey}")
            throw IllegalStateException("No se pudo analizar ${track.displayName}: ${e.localizedMessage}. Puedes fijar el inicio manualmente.", e)
        }
    }
}
