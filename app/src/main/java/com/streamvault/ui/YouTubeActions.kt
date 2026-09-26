package com.streamvault.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

/**
 * YouTube integration, limited to what YouTube itself allows third party apps to do:
 * hand a search over to the official YouTube app or the browser.
 *
 * It deliberately does NOT block ads, does NOT stream YouTube audio inside this player and
 * does NOT enable background playback of YouTube: that requires YouTube Premium and removing
 * ads would break YouTube's terms.
 */
object YouTubeLinks {

    fun query(title: String, artist: String): String {
        val parts = listOf(title, artist).map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("Artista desconocido", true) && !it.equals("Sin álbum", true) }
        return parts.joinToString(" ").take(120)
    }

    fun searchUrl(terms: String): String =
        "https://www.youtube.com/results?search_query=" + URLEncoder.encode(terms, "UTF-8")

    fun musicUrl(terms: String): String =
        "https://music.youtube.com/search?q=" + URLEncoder.encode(terms, "UTF-8")

    /** Opens the official YouTube app when installed, otherwise any browser. */
    fun open(context: Context, terms: String, music: Boolean = false): Boolean {
        if (terms.isBlank()) return false
        val url = if (music) musicUrl(terms) else searchUrl(terms)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent); true }.getOrElse {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }.getOrElse {
                Toast.makeText(context, "Instala YouTube o un navegador para abrir la búsqueda", Toast.LENGTH_LONG).show()
                false
            }
        }
    }
}
