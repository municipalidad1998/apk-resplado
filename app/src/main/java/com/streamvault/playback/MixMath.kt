package com.streamvault.playback

import kotlin.math.cos
import kotlin.math.sin

object MixMath {
    /** Equal-power rather than linear crossfading avoids a perceived dip at the midpoint. */
    fun gains(progress: Float): Pair<Float, Float> {
        val angle = progress.coerceIn(0f, 1f) * Math.PI / 2
        return cos(angle).toFloat().coerceIn(0f, 1f) to sin(angle).toFloat().coerceIn(0f, 1f)
    }
    fun duration(requestedMs: Long, outgoingPlayableMs: Long, incomingPlayableMs: Long): Long =
        requestedMs.coerceAtLeast(0).coerceAtMost(outgoingPlayableMs.coerceAtLeast(0) / 2).coerceAtMost(incomingPlayableMs.coerceAtLeast(0) / 2)
    fun seek(position: Long, delta: Long, duration: Long) = (position + delta).coerceIn(0, duration.coerceAtLeast(0))
}
