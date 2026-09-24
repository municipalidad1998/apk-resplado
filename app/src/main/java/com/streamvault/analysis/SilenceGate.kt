package com.streamvault.analysis

import kotlin.math.pow

/** 20 ms RMS windows, 60 ms of sustained sound and a short pre-roll avoid clipping attacks. */
class SilenceGate(thresholdDb: Int, private val minimumMs: Long, private val preRollMs: Long = 80) {
    private val threshold = 10.0.pow(thresholdDb / 20.0)
    private var candidate: Long? = null
    private var windows = 0
    var startMs: Long? = null
        private set
    fun window(rms: Double, timeMs: Long) {
        if (startMs != null) return
        if (rms.isFinite() && rms >= threshold) {
            if (candidate == null) candidate = timeMs
            windows++
            if (windows >= 3) startMs = candidate!!.let { if (it >= minimumMs) (it - preRollMs).coerceAtLeast(0) else 0 }
        } else { candidate = null; windows = 0 }
    }
}
