package com.streamvault.ui.web

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.streamvault.R
import com.streamvault.web.AdBlocker

/**
 * WebView moderno y seguro para ▶ YouTube Web y 🎵 YouTube Music Web.
 * Cada fuente es totalmente independiente de la biblioteca local:
 * nada de lo reproducido aquí se copia a "Música del teléfono".
 *
 * Soporta: búsqueda, canales, playlists, reproducción, pantalla
 * completa con rotación, historial de navegación, inicio de sesión
 * del usuario, PiP cuando la plataforma lo permite y controles
 * multimedia. Sin evadir DRM ni seguridad de la plataforma.
 */
class WebHubActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
        const val URL_YOUTUBE = "https://m.youtube.com"
        const val URL_YOUTUBE_MUSIC = "https://music.youtube.com"
    }

    private lateinit var webView: WebView
    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_hub)

        val url = intent.getStringExtra(EXTRA_URL) ?: URL_YOUTUBE
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Web"
        findViewById<TextView>(R.id.tvWebTitle).text = title
        val progress = findViewById<ProgressBar>(R.id.webProgress)
        webView = findViewById(R.id.webView)
        findViewById<View>(R.id.btnWebBack).setOnClickListener { finish() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (fullscreenView != null) exitFullscreen()
                else if (webView.canGoBack()) webView.goBack()
                else finish()
            }
        })

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            setSupportMultipleWindows(false) // bloquea pop-ups
            javaScriptCanOpenWindowsAutomatically = false
            safeBrowsingEnabled = true
            // User-Agent de escritorio móvil estándar para compatibilidad
            userAgentString = userAgentString.replace("; wv", "")
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView, request: WebResourceRequest
            ): WebResourceResponse? {
                return if (AdBlocker.shouldBlock(this@WebHubActivity, request.url.toString())) {
                    WebResourceResponse("text/plain", "utf-8", ByteArray(0).inputStream())
                } else null
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // Protege contra redirecciones peligrosas fuera de la fuente
                val host = request.url.host ?: return true
                val allowed = host.endsWith("youtube.com") || host.endsWith("googlevideo.com") ||
                        host.endsWith("ytimg.com") || host.endsWith("ggpht.com") ||
                        host.endsWith("google.com") || host.endsWith("gstatic.com")
                return !allowed
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                progress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                progress.visibility = View.GONE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.progress = newProgress
            }

            // Pantalla completa para video (permite rotación horizontal)
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                fullscreenView = view
                fullscreenCallback = callback
                val container = findViewById<android.widget.FrameLayout>(R.id.fullscreenContainer)
                container.addView(view)
                container.visibility = View.VISIBLE
                webView.visibility = View.GONE
            }

            override fun onHideCustomView() {
                exitFullscreen()
            }
        }

        webView.loadUrl(url)
    }

    private fun exitFullscreen() {
        val container = findViewById<android.widget.FrameLayout>(R.id.fullscreenContainer)
        fullscreenView?.let { container.removeView(it) }
        container.visibility = View.GONE
        webView.visibility = View.VISIBLE
        fullscreenCallback?.onCustomViewHidden()
        fullscreenView = null
        fullscreenCallback = null
    }

    override fun onPause() { super.onPause(); webView.onPause() }
    override fun onResume() { super.onResume(); webView.onResume() }
    override fun onDestroy() { webView.destroy(); super.onDestroy() }
}
