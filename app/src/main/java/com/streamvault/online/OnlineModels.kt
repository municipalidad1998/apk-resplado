package com.streamvault.online

import com.streamvault.network.NetState

/** Quality tiers the user can choose, independent of the provider that ends up serving them. */
enum class Quality(val key: String, val label: String, val rank: Int) {
    AUTO("auto", "Automática", 0),
    LOW("low", "Baja", 1),
    NORMAL("normal", "Normal", 2),
    HIGH("high", "Alta", 3)
}

/** What a concrete stream file offers. */
enum class StreamFormat(val label: String, val rank: Int) {
    FLAC("FLAC", 4),
    ALAC("ALAC", 4),
    WAV("WAV", 4),
    OGG("OGG", 2),
    MP3("MP3", 1),
    AAC("AAC", 3),
    OTHER("Otro", 0)
}

data class OnlineStream(
    val url: String,
    val format: StreamFormat,
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 0,
    val sizeBytes: Long = 0,
    val durationSeconds: Float = 0f
) {
    val lossless: Boolean get() = format.rank >= 4
    val label: String get() = if (lossless) "${format.label} · sin pérdida" else if (bitrateKbps > 0) "${format.label} ${bitrateKbps} kbps" else format.label
}

data class OnlineResult(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val cover: String?,
    val durationSeconds: Float,
    val streams: List<OnlineStream>,
    val source: String,
    val license: String = ""
) {
    val bestStream: OnlineStream? get() = streams.maxByOrNull { it.format.rank * 10000 + it.bitrateKbps }
}

/**
 * Provider interface. The player only knows this interface, so the YouTube module (official
 * links only) or any other authorized source can be swapped in without touching playback.
 */
interface OnlineProvider {
    val key: String
    val label: String
    /** Streams audio itself. False for sources that may only hand the user over to their app. */
    val canStream: Boolean
    suspend fun search(query: String, limit: Int = 40): List<OnlineResult>
}

object QualityChooser {

    /**
     * Picks the stream for the requested quality. Falls back to the best available file when the
     * wanted tier does not exist, because a missing FLAC must not mean silence.
     */
    fun choose(streams: List<OnlineStream>, wanted: Quality, net: NetState, wifiOnly: Boolean, mobileData: Boolean): OnlineStream? {
        if (streams.isEmpty()) return null
        if (!net.online) return null
        if (wifiOnly && net.metered) return null
        if (net.metered && !mobileData) return null
        val effective = if (wanted == Quality.AUTO) auto(net) else wanted
        val ranked = streams.sortedByDescending { it.format.rank * 10000 + it.bitrateKbps }
        return when (effective) {
            Quality.HIGH -> ranked.firstOrNull { it.lossless } ?: ranked.first()
            Quality.NORMAL -> ranked.firstOrNull { !it.lossless && it.bitrateKbps >= 160 } ?: ranked.lastOrNull { !it.lossless } ?: ranked.first()
            Quality.LOW -> ranked.lastOrNull { !it.lossless } ?: ranked.last()
            Quality.AUTO -> ranked.first()
        }
    }

    fun auto(net: NetState): Quality = when {
        net.slow -> Quality.LOW
        net.metered -> Quality.NORMAL
        else -> Quality.HIGH
    }
}
