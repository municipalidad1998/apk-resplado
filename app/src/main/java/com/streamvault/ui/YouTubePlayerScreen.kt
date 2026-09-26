package com.streamvault.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.streamvault.online.OnlineResult
import com.streamvault.online.YouTubeProvider

/**
 * Plays a YouTube video **inside the app**, with the official embedded player, so nothing opens
 * in another window or another application.
 *
 * What this screen deliberately does NOT do:
 *
 *  - It does not block, hide or skip the ads YouTube serves. Removing them breaks YouTube's
 *    terms; the only legitimate way is YouTube Premium.
 *  - It does not extract an audio track, so there is no background playback, no casting of our
 *    own and no downloads.
 *  - It pauses the player as soon as the app goes to the background, which is what the official
 *    embedded player requires.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubePlayerScreen(result: OnlineResult, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var webView by remember { mutableStateOf<WebView?>(null) }
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    val videoId = remember(result.id) { result.id.removePrefix("yt-") }

    BackHandler(onBack = onClose)

    // YouTube's player must not keep running while the app is in the background.
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webView?.onPause()
                Lifecycle.Event.ON_RESUME -> webView?.onResume()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = {
            Column {
                Text(result.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
                Text(result.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }, navigationIcon = { ActionIcon(Icons.Rounded.Close, "Cerrar", onClose) }, actions = {
            // The official apps: for listening without ads and in the background, YouTube Premium.
            ActionIcon(Icons.Rounded.OpenInNew, "Abrir en YouTube", { open(context, YouTubeProvider.watchUrl(videoId)) })
            ActionIcon(Icons.Rounded.LibraryMusic, "Abrir en YouTube Music", { open(context, YouTubeProvider.musicUrl(videoId)) })
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(androidx.compose.ui.graphics.Color.Black)) {
                if (fullscreenView == null) {
                    AndroidView(factory = { ctx ->
                        val view = createWebView(ctx, result.embedUrl ?: YouTubeProvider.watchUrl(videoId)) { fullscreenView = it }
                        webView = view
                        view
                    }, modifier = Modifier.fillMaxSize(), update = { webView = it })
                } else {
                    AndroidView(factory = { ctx ->
                        FrameLayout(ctx).apply { addView(fullscreenView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)) }
                    }, modifier = Modifier.fillMaxSize())
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (result.durationSeconds > 0) Text(time((result.durationSeconds * 1000).toLong()), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(10.dp))
                    Text("Fuente: ${result.source}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
                Text("Se reproduce con el reproductor oficial de YouTube, dentro de la app y sin abrir otra "
                    + "ventana. Los anuncios los pone YouTube y no se bloquean: para escuchar sin anuncios y con la "
                    + "pantalla apagada existe YouTube Premium. Al salir de la app la reproducción se pausa.",
                    fontSize = 12.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(context: Context, embedUrl: String, onFullscreen: (View?) -> Unit): WebView =
    WebView(context).apply {
        setBackgroundColor(Color.BLACK)
        settings.javaScriptEnabled = true      // the official IFrame player needs it
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        webViewClient = WebViewClient()
        webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) { onFullscreen(view) }
            override fun onHideCustomView() { onFullscreen(null) }
        }
        loadUrl(embedUrl)
    }

private fun open(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
