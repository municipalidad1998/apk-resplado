package com.streamvault.web;

/**
 * Filtro de contenido para el WebView:
 * - Dominios publicitarios y de tracking conocidos.
 * - Bloqueo de pop-ups y redirecciones maliciosas.
 * - Lista actualizable (persistida en SharedPreferences) y
 *  excepciones configurables.
 * NUNCA intenta vulnerar DRM ni mecanismos de seguridad de plataformas.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\"\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\b\u0007\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u0004J\u0014\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00040\t2\u0006\u0010\f\u001a\u00020\rJ\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00040\t2\u0006\u0010\f\u001a\u00020\rJ\u000e\u0010\u0011\u001a\u00020\u00122\u0006\u0010\f\u001a\u00020\rJ\u0016\u0010\u0013\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u0014\u001a\u00020\u0012J\u0018\u0010\u0015\u001a\u00020\u00122\u0006\u0010\f\u001a\u00020\r2\b\u0010\u0016\u001a\u0004\u0018\u00010\u0004J\u001c\u0010\u0017\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00040\tR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00040\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0019"}, d2 = {"Lcom/streamvault/web/AdBlocker;", "", "()V", "KEY_CUSTOM", "", "KEY_ENABLED", "KEY_EXCEPTIONS", "PREFS", "baseBlockedDomains", "", "addException", "", "context", "Landroid/content/Context;", "domain", "getCustomBlocked", "getExceptions", "isEnabled", "", "setEnabled", "enabled", "shouldBlock", "url", "updateBlockedList", "domains", "app_debug"})
public final class AdBlocker {
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String PREFS = "adblocker_prefs";
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String KEY_ENABLED = "enabled";
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String KEY_EXCEPTIONS = "exceptions";
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String KEY_CUSTOM = "custom_domains";
    
    /**
     * Dominios publicitarios/tracker conocidos (lista base).
     */
    @org.jetbrains.annotations.NotNull
    private static final java.util.Set<java.lang.String> baseBlockedDomains = null;
    @org.jetbrains.annotations.NotNull
    public static final com.streamvault.web.AdBlocker INSTANCE = null;
    
    private AdBlocker() {
        super();
    }
    
    public final boolean isEnabled(@org.jetbrains.annotations.NotNull
    android.content.Context context) {
        return false;
    }
    
    public final void setEnabled(@org.jetbrains.annotations.NotNull
    android.content.Context context, boolean enabled) {
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.Set<java.lang.String> getExceptions(@org.jetbrains.annotations.NotNull
    android.content.Context context) {
        return null;
    }
    
    public final void addException(@org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    java.lang.String domain) {
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.Set<java.lang.String> getCustomBlocked(@org.jetbrains.annotations.NotNull
    android.content.Context context) {
        return null;
    }
    
    /**
     * Sustituye la lista con una versión actualizada (p. ej. descargada).
     */
    public final void updateBlockedList(@org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    java.util.Set<java.lang.String> domains) {
    }
    
    public final boolean shouldBlock(@org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.Nullable
    java.lang.String url) {
        return false;
    }
}