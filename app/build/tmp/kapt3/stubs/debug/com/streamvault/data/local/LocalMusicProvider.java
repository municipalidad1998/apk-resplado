package com.streamvault.data.local;

/**
 * Proveedor EXCLUSIVO de música local. Usa únicamente MediaStore del
 * dispositivo. Jamás consulta Internet, YouTube ni APIs musicales.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u0000 \u000b2\u00020\u0001:\u0001\u000bB\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\t0\u0006H\u0086@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\nR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\f"}, d2 = {"Lcom/streamvault/data/local/LocalMusicProvider;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "supportedExt", "", "", "scanAll", "Lcom/streamvault/data/local/LocalSong;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "app_debug"})
public final class LocalMusicProvider {
    @org.jetbrains.annotations.NotNull
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull
    private final java.util.List<java.lang.String> supportedExt = null;
    @org.jetbrains.annotations.NotNull
    public static final com.streamvault.data.local.LocalMusicProvider.Companion Companion = null;
    
    public LocalMusicProvider(@org.jetbrains.annotations.NotNull
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object scanAll(@org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super java.util.List<com.streamvault.data.local.LocalSong>> $completion) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u0006J\u000e\u0010\b\u001a\u00020\u00062\u0006\u0010\t\u001a\u00020\u0006J\"\u0010\n\u001a\b\u0012\u0004\u0012\u00020\f0\u000b2\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\f0\u000b2\u0006\u0010\u000e\u001a\u00020\u0006\u00a8\u0006\u000f"}, d2 = {"Lcom/streamvault/data/local/LocalMusicProvider$Companion;", "", "()V", "levenshtein", "", "a", "", "b", "norm", "s", "search", "", "Lcom/streamvault/data/local/LocalSong;", "songs", "query", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        /**
         * Normaliza: minúsculas, sin acentos, espacios compactados.
         */
        @org.jetbrains.annotations.NotNull
        public final java.lang.String norm(@org.jetbrains.annotations.NotNull
        java.lang.String s) {
            return null;
        }
        
        /**
         * Búsqueda 100% LOCAL: exacta, por artista/título/álbum, parcial,
         * insensible a mayúsculas y acentos, con tolerancia básica a errores
         * (distancia de edición <= 2 por token). Nunca devuelve resultados
         * aleatorios ni contenido online.
         */
        @org.jetbrains.annotations.NotNull
        public final java.util.List<com.streamvault.data.local.LocalSong> search(@org.jetbrains.annotations.NotNull
        java.util.List<com.streamvault.data.local.LocalSong> songs, @org.jetbrains.annotations.NotNull
        java.lang.String query) {
            return null;
        }
        
        public final int levenshtein(@org.jetbrains.annotations.NotNull
        java.lang.String a, @org.jetbrains.annotations.NotNull
        java.lang.String b) {
            return 0;
        }
    }
}