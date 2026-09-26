package com.streamvault.playback

/** User-entered seconds (including fractions) or mm:ss / hh:mm:ss. A dot is decimal, not a clock separator. */
fun parseAudioTime(value: String): Long? {
    val parts = value.trim().replace(',', '.').split(':')
    if (parts.size !in 1..3 || parts.any { it.isBlank() }) return null
    val values = parts.map { it.toDoubleOrNull() ?: return null }
    if (values.any { !it.isFinite() || it < 0 } || values.drop(1).any { it >= 60 } ||
        values.dropLast(1).any { it % 1.0 != 0.0 }) return null
    val seconds = values.fold(0.0) { total, part -> total * 60 + part }
    return (seconds * 1000).takeIf { it <= Long.MAX_VALUE.toDouble() }?.toLong()
}

/** A useful end is separate from crossfade length: a 2-minute outro need not be a 2-minute mix. */
object PlaybackCue {
    fun end(duration: Long, start: Long, selectedEnd: Long?): Long =
        selectedEnd?.takeIf { it > start && it <= duration } ?: duration
    fun overlap(requested: Long, outgoing: Long, incoming: Long): Long =
        requested.coerceAtLeast(0).coerceAtMost((outgoing - 100).coerceAtLeast(0)).coerceAtMost((incoming - 100).coerceAtLeast(0))
}
