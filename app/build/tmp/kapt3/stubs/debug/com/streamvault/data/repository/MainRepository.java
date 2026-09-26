package com.streamvault.data.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eJ\u001a\u0010\u000f\u001a\u00020\u000e2\u0006\u0010\u0010\u001a\u00020\u000e2\b\b\u0002\u0010\u0011\u001a\u00020\u0012H\u0002J\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00150\u0014J\u0019\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u0015H\u0086@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0019J\u001f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u001b0\u00142\u0006\u0010\u0018\u001a\u00020\u0015H\u0086@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0019J\u000e\u0010\u001c\u001a\u00020\f2\u0006\u0010\u0018\u001a\u00020\u0015R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\u001d"}, d2 = {"Lcom/streamvault/data/repository/MainRepository;", "", "prefs", "Landroid/content/SharedPreferences;", "(Landroid/content/SharedPreferences;)V", "client", "Lokhttp3/OkHttpClient;", "gson", "Lcom/google/gson/Gson;", "xtream", "Lcom/streamvault/data/repository/XtreamRepository;", "deleteSource", "", "id", "", "fetchWithRetry", "url", "retries", "", "getSources", "", "Lcom/streamvault/data/model/SavedSource;", "loadAll", "Lcom/streamvault/util/ParsedContent;", "source", "(Lcom/streamvault/data/model/SavedSource;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "loadChannels", "Lcom/streamvault/data/model/Channel;", "saveSource", "app_debug"})
public final class MainRepository {
    @org.jetbrains.annotations.NotNull
    private final android.content.SharedPreferences prefs = null;
    @org.jetbrains.annotations.NotNull
    private final com.google.gson.Gson gson = null;
    @org.jetbrains.annotations.NotNull
    private final com.streamvault.data.repository.XtreamRepository xtream = null;
    @org.jetbrains.annotations.NotNull
    private final okhttp3.OkHttpClient client = null;
    
    public MainRepository(@org.jetbrains.annotations.NotNull
    android.content.SharedPreferences prefs) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.List<com.streamvault.data.model.SavedSource> getSources() {
        return null;
    }
    
    public final void saveSource(@org.jetbrains.annotations.NotNull
    com.streamvault.data.model.SavedSource source) {
    }
    
    public final void deleteSource(@org.jetbrains.annotations.NotNull
    java.lang.String id) {
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object loadAll(@org.jetbrains.annotations.NotNull
    com.streamvault.data.model.SavedSource source, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.streamvault.util.ParsedContent> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object loadChannels(@org.jetbrains.annotations.NotNull
    com.streamvault.data.model.SavedSource source, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super java.util.List<com.streamvault.data.model.Channel>> $completion) {
        return null;
    }
    
    private final java.lang.String fetchWithRetry(java.lang.String url, int retries) {
        return null;
    }
}