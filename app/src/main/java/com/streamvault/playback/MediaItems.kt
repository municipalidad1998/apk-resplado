package com.streamvault.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.streamvault.data.Track
import com.streamvault.settings.PlayerSettings

fun Track.mediaItem(settings: PlayerSettings, original: Boolean = false): MediaItem {
    val extras = Bundle().apply {
        putLong("offset", if (original) 0 else offset(settings.detectSilence))
        putLong("duration", durationMs)
        putString("uri", uri)
        putBoolean("original", original)
        playbackEndMs?.let { putLong("end", it) }
        crossfadeSeconds?.let { putInt("crossfade", it) }
        loudnessDb?.let { putFloat("loudness", it) }
    }
    return MediaItem.Builder().setMediaId(id).setUri(uri).setMediaMetadata(
        MediaMetadata.Builder().setTitle(displayName).setArtist(artist).setAlbumTitle(album)
            .setArtworkUri(cover?.let(Uri::parse)).setIsPlayable(true).setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setExtras(extras).build()).build()
}
val MediaItem.offsetMs: Long get() = mediaMetadata.extras?.getLong("offset") ?: 0
val MediaItem.durationMs: Long get() = mediaMetadata.extras?.getLong("duration") ?: 0

val MediaItem.selectedEndMs: Long? get() = mediaMetadata.extras?.let { if (it.containsKey("end")) it.getLong("end") else null }
val MediaItem.crossfadeSeconds: Int? get() = mediaMetadata.extras?.let { if (it.containsKey("crossfade")) it.getInt("crossfade") else null }
val MediaItem.loudnessDb: Float? get() = mediaMetadata.extras?.let { if (it.containsKey("loudness")) it.getFloat("loudness") else null }
