package com.streamvault.ui.music;

/**
 * Reproductor de música local a pantalla completa.
 * Sigue funcionando con pantalla apagada, teléfono bloqueado o app
 * en segundo plano gracias a MusicPlayerService (MediaSession),
 * con controles en bloqueo, notificaciones, auriculares y Bluetooth.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000x\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0002\b\u0004\u0018\u0000 /2\u00020\u0001:\u0001/B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u001f\u001a\u00020 2\u0006\u0010!\u001a\u00020\"H\u0002J\u0012\u0010#\u001a\u00020$2\b\u0010%\u001a\u0004\u0018\u00010&H\u0014J\b\u0010\'\u001a\u00020$H\u0014J\b\u0010(\u001a\u00020$H\u0014J\b\u0010)\u001a\u00020$H\u0002J\u0010\u0010*\u001a\u00020$2\u0006\u0010+\u001a\u00020,H\u0002J\u0012\u0010-\u001a\u00020$2\b\u0010.\u001a\u0004\u0018\u00010\u0015H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0016\u0010\t\u001a\n\u0012\u0004\u0012\u00020\b\u0018\u00010\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00150\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u0017X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0018\u001a\u00020\u0019X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001a\u001a\u00020\u001bX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001c\u001a\u00020\u001bX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001d\u001a\u00020\u001bX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001e\u001a\u00020\u001bX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u00060"}, d2 = {"Lcom/streamvault/ui/music/PlayerMusicActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "btnPlay", "Landroid/widget/ImageButton;", "btnRepeat", "btnShuffle", "controller", "Landroidx/media3/session/MediaController;", "controllerFuture", "Lcom/google/common/util/concurrent/ListenableFuture;", "handler", "Landroid/os/Handler;", "ivCover", "Landroid/widget/ImageView;", "playerListener", "Landroidx/media3/common/Player$Listener;", "progressRunnable", "Ljava/lang/Runnable;", "queue", "", "Lcom/streamvault/data/local/LocalSong;", "seekBar", "Landroid/widget/SeekBar;", "startIndex", "", "tvArtist", "Landroid/widget/TextView;", "tvCurrent", "tvTitle", "tvTotal", "fmt", "", "ms", "", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onStart", "onStop", "startQueue", "updatePlayButton", "playing", "", "updateSongUI", "s", "Companion", "app_debug"})
public final class PlayerMusicActivity extends androidx.appcompat.app.AppCompatActivity {
    @org.jetbrains.annotations.NotNull
    public static final java.lang.String EXTRA_QUEUE = "queue";
    @org.jetbrains.annotations.NotNull
    public static final java.lang.String EXTRA_INDEX = "index";
    @org.jetbrains.annotations.Nullable
    private com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.MediaController> controllerFuture;
    @org.jetbrains.annotations.Nullable
    private androidx.media3.session.MediaController controller;
    @org.jetbrains.annotations.NotNull
    private java.util.List<com.streamvault.data.local.LocalSong> queue;
    private int startIndex = 0;
    @org.jetbrains.annotations.NotNull
    private final android.os.Handler handler = null;
    private android.widget.ImageView ivCover;
    private android.widget.TextView tvTitle;
    private android.widget.TextView tvArtist;
    private android.widget.SeekBar seekBar;
    private android.widget.TextView tvCurrent;
    private android.widget.TextView tvTotal;
    private android.widget.ImageButton btnPlay;
    private android.widget.ImageButton btnShuffle;
    private android.widget.ImageButton btnRepeat;
    @org.jetbrains.annotations.NotNull
    private final java.lang.Runnable progressRunnable = null;
    @org.jetbrains.annotations.NotNull
    private final androidx.media3.common.Player.Listener playerListener = null;
    @org.jetbrains.annotations.NotNull
    public static final com.streamvault.ui.music.PlayerMusicActivity.Companion Companion = null;
    
    public PlayerMusicActivity() {
        super();
    }
    
    @java.lang.Override
    protected void onCreate(@org.jetbrains.annotations.Nullable
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override
    protected void onStart() {
    }
    
    private final void startQueue() {
    }
    
    private final void updateSongUI(com.streamvault.data.local.LocalSong s) {
    }
    
    private final void updatePlayButton(boolean playing) {
    }
    
    private final java.lang.String fmt(long ms) {
        return null;
    }
    
    @java.lang.Override
    protected void onStop() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/streamvault/ui/music/PlayerMusicActivity$Companion;", "", "()V", "EXTRA_INDEX", "", "EXTRA_QUEUE", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}