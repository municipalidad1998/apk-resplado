package com.streamvault.telegram

import com.streamvault.LuminaApp
import com.streamvault.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Stores Telegram music and links it to the local copy when the file is still on the device. */
class TelegramRepository(private val app: LuminaApp) {

    val files get() = app.database.telegram().all()
    val count get() = app.database.telegram().count()

    suspend fun import(text: String): TelegramImport.Summary = withContext(Dispatchers.IO) {
        val parsed = TelegramImport.parse(text)
        if (parsed.isEmpty()) return@withContext TelegramImport.Summary(0, 0, 0)
        app.database.telegram().insert(parsed)
        var linked = 0
        for (file in parsed) {
            val track: Track? = app.library.byFile(file.name, file.size)
            if (track != null) {
                // Keep the metadata the chat gave it, and remember which copy is local.
                val merged = track.copy(
                    artist = if (track.artist == "Artista desconocido" && file.artist != "Artista desconocido") file.artist else track.artist,
                    title = if (file.durationMs > 0 && track.title == file.name.substringBeforeLast('.')) file.name.substringBeforeLast('.') else track.title,
                    album = if (track.album == "Sin álbum" && file.album != "Telegram") file.album else track.album,
                    source = "telegram"
                )
                if (merged != track) app.library.update(merged)
                app.database.telegram().link(file.messageId, merged.id)
                linked++
            }
        }
        TelegramImport.Summary(parsed.size, linked, parsed.size)
    }
}
