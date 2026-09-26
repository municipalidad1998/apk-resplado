package com.streamvault.playback

import com.streamvault.data.local.LocalSong

/** Mantiene la cola actual accesible entre pantallas (mini reproductor). */
object PlaybackStateHolder {
    var queue: List<LocalSong> = emptyList()
    var index: Int = 0
}
