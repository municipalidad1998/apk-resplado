package com.streamvault.ui.web;

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
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u0000 \u00112\u00020\u0001:\u0001\u0011B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\t\u001a\u00020\nH\u0002J\u0012\u0010\u000b\u001a\u00020\n2\b\u0010\f\u001a\u0004\u0018\u00010\rH\u0015J\b\u0010\u000e\u001a\u00020\nH\u0014J\b\u0010\u000f\u001a\u00020\nH\u0014J\b\u0010\u0010\u001a\u00020\nH\u0014R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0012"}, d2 = {"Lcom/streamvault/ui/web/WebHubActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "fullscreenCallback", "Landroid/webkit/WebChromeClient$CustomViewCallback;", "fullscreenView", "Landroid/view/View;", "webView", "Landroid/webkit/WebView;", "exitFullscreen", "", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "onPause", "onResume", "Companion", "app_debug"})
public final class WebHubActivity extends androidx.appcompat.app.AppCompatActivity {
    @org.jetbrains.annotations.NotNull
    public static final java.lang.String EXTRA_URL = "url";
    @org.jetbrains.annotations.NotNull
    public static final java.lang.String EXTRA_TITLE = "title";
    @org.jetbrains.annotations.NotNull
    public static final java.lang.String URL_YOUTUBE = "https://m.youtube.com";
    @org.jetbrains.annotations.NotNull
    public static final java.lang.String URL_YOUTUBE_MUSIC = "https://music.youtube.com";
    private android.webkit.WebView webView;
    @org.jetbrains.annotations.Nullable
    private android.view.View fullscreenView;
    @org.jetbrains.annotations.Nullable
    private android.webkit.WebChromeClient.CustomViewCallback fullscreenCallback;
    @org.jetbrains.annotations.NotNull
    public static final com.streamvault.ui.web.WebHubActivity.Companion Companion = null;
    
    public WebHubActivity() {
        super();
    }
    
    @java.lang.Override
    @android.annotation.SuppressLint(value = {"SetJavaScriptEnabled"})
    protected void onCreate(@org.jetbrains.annotations.Nullable
    android.os.Bundle savedInstanceState) {
    }
    
    private final void exitFullscreen() {
    }
    
    @java.lang.Override
    protected void onPause() {
    }
    
    @java.lang.Override
    protected void onResume() {
    }
    
    @java.lang.Override
    protected void onDestroy() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lcom/streamvault/ui/web/WebHubActivity$Companion;", "", "()V", "EXTRA_TITLE", "", "EXTRA_URL", "URL_YOUTUBE", "URL_YOUTUBE_MUSIC", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}