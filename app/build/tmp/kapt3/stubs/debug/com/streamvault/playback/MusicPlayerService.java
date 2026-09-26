package com.streamvault.playback;

/**
 * Servicio de reproducción de música LOCAL en primer plano.
 * - MediaSession expone los controles a: pantalla de bloqueo, panel de
 *  notificaciones, auriculares, Bluetooth y vehículo (⏮ ▶ ⏭ con
 *  canción, artista, portada y progreso).
 * - Funciona con pantalla apagada, teléfono bloqueado, app cerrada y
 *  sin conexión a Internet.
 * - Maneja Audio Focus automáticamente (pausa si entra una llamada).
 * Solo reproduce fuentes locales; nunca contenido online.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\n\u001a\u00020\u000bH\u0016J\b\u0010\f\u001a\u00020\u000bH\u0016J\u0012\u0010\r\u001a\u0004\u0018\u00010\u00042\u0006\u0010\u000e\u001a\u00020\u000fH\u0016R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u0007\u001a\u00020\u00062\u0006\u0010\u0005\u001a\u00020\u0006@BX\u0086.\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\t\u00a8\u0006\u0010"}, d2 = {"Lcom/streamvault/playback/MusicPlayerService;", "Landroidx/media3/session/MediaSessionService;", "()V", "mediaSession", "Landroidx/media3/session/MediaSession;", "<set-?>", "Landroidx/media3/exoplayer/ExoPlayer;", "player", "getPlayer", "()Landroidx/media3/exoplayer/ExoPlayer;", "onCreate", "", "onDestroy", "onGetSession", "controllerInfo", "Landroidx/media3/session/MediaSession$ControllerInfo;", "app_debug"})
public final class MusicPlayerService extends androidx.media3.session.MediaSessionService {
    @org.jetbrains.annotations.Nullable
    private androidx.media3.session.MediaSession mediaSession;
    private androidx.media3.exoplayer.ExoPlayer player;
    
    public MusicPlayerService() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final androidx.media3.exoplayer.ExoPlayer getPlayer() {
        return null;
    }
    
    @java.lang.Override
    public void onCreate() {
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.Nullable
    public androidx.media3.session.MediaSession onGetSession(@org.jetbrains.annotations.NotNull
    androidx.media3.session.MediaSession.ControllerInfo controllerInfo) {
        return null;
    }
    
    @java.lang.Override
    public void onDestroy() {
    }
}