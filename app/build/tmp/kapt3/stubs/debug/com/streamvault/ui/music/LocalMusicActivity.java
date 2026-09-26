package com.streamvault.ui.music;

/**
 * 📱 MÚSICA DEL TELÉFONO — biblioteca 100% local e independiente.
 * El buscador actúa EXCLUSIVAMENTE sobre archivos físicos del
 * teléfono (MediaStore). Sin Internet, sin YouTube, sin APIs.
 *
 * Modos adicionales (misma pantalla, otra fuente MediaStore):
 * 🎬 Videos locales | 🖼 Fotos locales | 📄 Documentos locales.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000X\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0011\n\u0000\n\u0002\u0010\u0015\n\u0002\b\u000e\u0018\u0000 +2\u00020\u0001:\u0004+,-.B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0010\u001a\u00020\tH\u0002J\b\u0010\u0011\u001a\u00020\u0012H\u0002J\u001a\u0010\u0013\u001a\u00020\u00122\u0006\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\tH\u0002J\b\u0010\u0017\u001a\u00020\u0012H\u0002J\u0012\u0010\u0018\u001a\u00020\u00122\b\u0010\u0019\u001a\u0004\u0018\u00010\u001aH\u0014J-\u0010\u001b\u001a\u00020\u00122\u0006\u0010\u001c\u001a\u00020\u001d2\u000e\u0010\u001e\u001a\n\u0012\u0006\b\u0001\u0012\u00020\t0\u001f2\u0006\u0010 \u001a\u00020!H\u0016\u00a2\u0006\u0002\u0010\"J\u001e\u0010#\u001a\u00020\u00122\f\u0010$\u001a\b\u0012\u0004\u0012\u00020\u00070\u00062\u0006\u0010%\u001a\u00020\u001dH\u0002J\b\u0010&\u001a\u00020\u0012H\u0002J\u0010\u0010\'\u001a\u00020\u00122\u0006\u0010(\u001a\u00020\tH\u0002J\u0016\u0010)\u001a\u00020\u00122\f\u0010*\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\b\u001a\u00020\t8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\f\u0010\r\u001a\u0004\b\n\u0010\u000bR\u000e\u0010\u000e\u001a\u00020\u000fX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006/"}, d2 = {"Lcom/streamvault/ui/music/LocalMusicActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "adapter", "Lcom/streamvault/ui/music/LocalMusicActivity$SongAdapter;", "allSongs", "", "Lcom/streamvault/data/local/LocalSong;", "mode", "", "getMode", "()Ljava/lang/String;", "mode$delegate", "Lkotlin/Lazy;", "provider", "Lcom/streamvault/data/local/LocalMusicProvider;", "audioPermission", "checkPermissionAndLoad", "", "loadSimpleMedia", "collection", "Landroid/net/Uri;", "mimeFallback", "loadSongs", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onRequestPermissionsResult", "code", "", "perms", "", "results", "", "(I[Ljava/lang/String;[I)V", "openPlayer", "songs", "pos", "setupSearch", "showEmpty", "msg", "showSongs", "list", "Companion", "SimpleAdapter", "SimpleItem", "SongAdapter", "app_debug"})
public final class LocalMusicActivity extends androidx.appcompat.app.AppCompatActivity {
    @org.jetbrains.annotations.NotNull
    public static final java.lang.String EXTRA_MODE = "mode";
    @org.jetbrains.annotations.NotNull
    public static final java.lang.String MODE_MUSIC = "music";
    @org.jetbrains.annotations.NotNull
    public static final java.lang.String MODE_VIDEOS = "videos";
    @org.jetbrains.annotations.NotNull
    public static final java.lang.String MODE_PHOTOS = "photos";
    @org.jetbrains.annotations.NotNull
    public static final java.lang.String MODE_DOCS = "docs";
    @org.jetbrains.annotations.NotNull
    public static final java.lang.String MODE_PLAYLISTS = "playlists";
    private static final int REQ_PERM = 1001;
    private com.streamvault.data.local.LocalMusicProvider provider;
    @org.jetbrains.annotations.NotNull
    private java.util.List<com.streamvault.data.local.LocalSong> allSongs;
    private com.streamvault.ui.music.LocalMusicActivity.SongAdapter adapter;
    @org.jetbrains.annotations.NotNull
    private final kotlin.Lazy mode$delegate = null;
    @org.jetbrains.annotations.NotNull
    public static final com.streamvault.ui.music.LocalMusicActivity.Companion Companion = null;
    
    public LocalMusicActivity() {
        super();
    }
    
    private final java.lang.String getMode() {
        return null;
    }
    
    @java.lang.Override
    protected void onCreate(@org.jetbrains.annotations.Nullable
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupSearch() {
    }
    
    private final java.lang.String audioPermission() {
        return null;
    }
    
    private final void checkPermissionAndLoad() {
    }
    
    @java.lang.Override
    public void onRequestPermissionsResult(int code, @org.jetbrains.annotations.NotNull
    java.lang.String[] perms, @org.jetbrains.annotations.NotNull
    int[] results) {
    }
    
    private final void loadSongs() {
    }
    
    private final void showSongs(java.util.List<com.streamvault.data.local.LocalSong> list) {
    }
    
    private final void openPlayer(java.util.List<com.streamvault.data.local.LocalSong> songs, int pos) {
    }
    
    private final void loadSimpleMedia(android.net.Uri collection, java.lang.String mimeFallback) {
    }
    
    private final void showEmpty(java.lang.String msg) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\b\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lcom/streamvault/ui/music/LocalMusicActivity$Companion;", "", "()V", "EXTRA_MODE", "", "MODE_DOCS", "MODE_MUSIC", "MODE_PHOTOS", "MODE_PLAYLISTS", "MODE_VIDEOS", "REQ_PERM", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0001\u0017B\'\u0012\f\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u0012\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\b0\u0007\u00a2\u0006\u0002\u0010\tJ\b\u0010\u000e\u001a\u00020\u000fH\u0016J\u0018\u0010\u0010\u001a\u00020\b2\u0006\u0010\u0011\u001a\u00020\u00022\u0006\u0010\u0012\u001a\u00020\u000fH\u0016J\u0018\u0010\u0013\u001a\u00020\u00022\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u000fH\u0016R\u0017\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u001d\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\b0\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\r\u00a8\u0006\u0018"}, d2 = {"Lcom/streamvault/ui/music/LocalMusicActivity$SimpleAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Lcom/streamvault/ui/music/LocalMusicActivity$SimpleAdapter$VH;", "items", "", "Lcom/streamvault/ui/music/LocalMusicActivity$SimpleItem;", "onClick", "Lkotlin/Function1;", "", "(Ljava/util/List;Lkotlin/jvm/functions/Function1;)V", "getItems", "()Ljava/util/List;", "getOnClick", "()Lkotlin/jvm/functions/Function1;", "getItemCount", "", "onBindViewHolder", "h", "pos", "onCreateViewHolder", "p", "Landroid/view/ViewGroup;", "t", "VH", "app_debug"})
    public static final class SimpleAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<com.streamvault.ui.music.LocalMusicActivity.SimpleAdapter.VH> {
        @org.jetbrains.annotations.NotNull
        private final java.util.List<com.streamvault.ui.music.LocalMusicActivity.SimpleItem> items = null;
        @org.jetbrains.annotations.NotNull
        private final kotlin.jvm.functions.Function1<com.streamvault.ui.music.LocalMusicActivity.SimpleItem, kotlin.Unit> onClick = null;
        
        public SimpleAdapter(@org.jetbrains.annotations.NotNull
        java.util.List<com.streamvault.ui.music.LocalMusicActivity.SimpleItem> items, @org.jetbrains.annotations.NotNull
        kotlin.jvm.functions.Function1<? super com.streamvault.ui.music.LocalMusicActivity.SimpleItem, kotlin.Unit> onClick) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull
        public final java.util.List<com.streamvault.ui.music.LocalMusicActivity.SimpleItem> getItems() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        public final kotlin.jvm.functions.Function1<com.streamvault.ui.music.LocalMusicActivity.SimpleItem, kotlin.Unit> getOnClick() {
            return null;
        }
        
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public com.streamvault.ui.music.LocalMusicActivity.SimpleAdapter.VH onCreateViewHolder(@org.jetbrains.annotations.NotNull
        android.view.ViewGroup p, int t) {
            return null;
        }
        
        @java.lang.Override
        public int getItemCount() {
            return 0;
        }
        
        @java.lang.Override
        public void onBindViewHolder(@org.jetbrains.annotations.NotNull
        com.streamvault.ui.music.LocalMusicActivity.SimpleAdapter.VH h, int pos) {
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0007\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\r\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\fR\u0011\u0010\u000f\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\f\u00a8\u0006\u0011"}, d2 = {"Lcom/streamvault/ui/music/LocalMusicActivity$SimpleAdapter$VH;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "v", "Landroid/view/View;", "(Landroid/view/View;)V", "art", "Landroid/widget/ImageView;", "getArt", "()Landroid/widget/ImageView;", "dur", "Landroid/widget/TextView;", "getDur", "()Landroid/widget/TextView;", "sub", "getSub", "title", "getTitle", "app_debug"})
        public static final class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            @org.jetbrains.annotations.NotNull
            private final android.widget.ImageView art = null;
            @org.jetbrains.annotations.NotNull
            private final android.widget.TextView title = null;
            @org.jetbrains.annotations.NotNull
            private final android.widget.TextView sub = null;
            @org.jetbrains.annotations.NotNull
            private final android.widget.TextView dur = null;
            
            public VH(@org.jetbrains.annotations.NotNull
            android.view.View v) {
                super(null);
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.ImageView getArt() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.TextView getTitle() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.TextView getSub() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.TextView getDur() {
                return null;
            }
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u001f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\u0002\u0010\u0007J\t\u0010\r\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u000e\u001a\u00020\u0005H\u00c6\u0003J\u000b\u0010\u000f\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J)\u0010\u0010\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u0003H\u00c6\u0001J\u0013\u0010\u0011\u001a\u00020\u00122\b\u0010\u0013\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0014\u001a\u00020\u0015H\u00d6\u0001J\t\u0010\u0016\u001a\u00020\u0003H\u00d6\u0001R\u0013\u0010\u0006\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\tR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\f\u00a8\u0006\u0017"}, d2 = {"Lcom/streamvault/ui/music/LocalMusicActivity$SimpleItem;", "", "name", "", "uri", "Landroid/net/Uri;", "mime", "(Ljava/lang/String;Landroid/net/Uri;Ljava/lang/String;)V", "getMime", "()Ljava/lang/String;", "getName", "getUri", "()Landroid/net/Uri;", "component1", "component2", "component3", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
    public static final class SimpleItem {
        @org.jetbrains.annotations.NotNull
        private final java.lang.String name = null;
        @org.jetbrains.annotations.NotNull
        private final android.net.Uri uri = null;
        @org.jetbrains.annotations.Nullable
        private final java.lang.String mime = null;
        
        public SimpleItem(@org.jetbrains.annotations.NotNull
        java.lang.String name, @org.jetbrains.annotations.NotNull
        android.net.Uri uri, @org.jetbrains.annotations.Nullable
        java.lang.String mime) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull
        public final java.lang.String getName() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        public final android.net.Uri getUri() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable
        public final java.lang.String getMime() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        public final android.net.Uri component2() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable
        public final java.lang.String component3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        public final com.streamvault.ui.music.LocalMusicActivity.SimpleItem copy(@org.jetbrains.annotations.NotNull
        java.lang.String name, @org.jetbrains.annotations.NotNull
        android.net.Uri uri, @org.jetbrains.annotations.Nullable
        java.lang.String mime) {
            return null;
        }
        
        @java.lang.Override
        public boolean equals(@org.jetbrains.annotations.Nullable
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public java.lang.String toString() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0002\u0010\u0002\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0001\u0017B3\u0012\f\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u0012\u001e\u0010\u0006\u001a\u001a\u0012\u0004\u0012\u00020\b\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u0004\u0012\u0004\u0012\u00020\t0\u0007\u00a2\u0006\u0002\u0010\nJ\b\u0010\u000f\u001a\u00020\bH\u0016J\u0018\u0010\u0010\u001a\u00020\t2\u0006\u0010\u0011\u001a\u00020\u00022\u0006\u0010\u0012\u001a\u00020\bH\u0016J\u0018\u0010\u0013\u001a\u00020\u00022\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\bH\u0016R)\u0010\u0006\u001a\u001a\u0012\u0004\u0012\u00020\b\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u0004\u0012\u0004\u0012\u00020\t0\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0017\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000e\u00a8\u0006\u0018"}, d2 = {"Lcom/streamvault/ui/music/LocalMusicActivity$SongAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Lcom/streamvault/ui/music/LocalMusicActivity$SongAdapter$VH;", "songs", "", "Lcom/streamvault/data/local/LocalSong;", "onClick", "Lkotlin/Function2;", "", "", "(Ljava/util/List;Lkotlin/jvm/functions/Function2;)V", "getOnClick", "()Lkotlin/jvm/functions/Function2;", "getSongs", "()Ljava/util/List;", "getItemCount", "onBindViewHolder", "h", "pos", "onCreateViewHolder", "p", "Landroid/view/ViewGroup;", "t", "VH", "app_debug"})
    public static final class SongAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<com.streamvault.ui.music.LocalMusicActivity.SongAdapter.VH> {
        @org.jetbrains.annotations.NotNull
        private final java.util.List<com.streamvault.data.local.LocalSong> songs = null;
        @org.jetbrains.annotations.NotNull
        private final kotlin.jvm.functions.Function2<java.lang.Integer, java.util.List<com.streamvault.data.local.LocalSong>, kotlin.Unit> onClick = null;
        
        public SongAdapter(@org.jetbrains.annotations.NotNull
        java.util.List<com.streamvault.data.local.LocalSong> songs, @org.jetbrains.annotations.NotNull
        kotlin.jvm.functions.Function2<? super java.lang.Integer, ? super java.util.List<com.streamvault.data.local.LocalSong>, kotlin.Unit> onClick) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull
        public final java.util.List<com.streamvault.data.local.LocalSong> getSongs() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        public final kotlin.jvm.functions.Function2<java.lang.Integer, java.util.List<com.streamvault.data.local.LocalSong>, kotlin.Unit> getOnClick() {
            return null;
        }
        
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public com.streamvault.ui.music.LocalMusicActivity.SongAdapter.VH onCreateViewHolder(@org.jetbrains.annotations.NotNull
        android.view.ViewGroup p, int t) {
            return null;
        }
        
        @java.lang.Override
        public int getItemCount() {
            return 0;
        }
        
        @java.lang.Override
        public void onBindViewHolder(@org.jetbrains.annotations.NotNull
        com.streamvault.ui.music.LocalMusicActivity.SongAdapter.VH h, int pos) {
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0007\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\r\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\fR\u0011\u0010\u000f\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\f\u00a8\u0006\u0011"}, d2 = {"Lcom/streamvault/ui/music/LocalMusicActivity$SongAdapter$VH;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "v", "Landroid/view/View;", "(Landroid/view/View;)V", "art", "Landroid/widget/ImageView;", "getArt", "()Landroid/widget/ImageView;", "artist", "Landroid/widget/TextView;", "getArtist", "()Landroid/widget/TextView;", "duration", "getDuration", "title", "getTitle", "app_debug"})
        public static final class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            @org.jetbrains.annotations.NotNull
            private final android.widget.ImageView art = null;
            @org.jetbrains.annotations.NotNull
            private final android.widget.TextView title = null;
            @org.jetbrains.annotations.NotNull
            private final android.widget.TextView artist = null;
            @org.jetbrains.annotations.NotNull
            private final android.widget.TextView duration = null;
            
            public VH(@org.jetbrains.annotations.NotNull
            android.view.View v) {
                super(null);
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.ImageView getArt() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.TextView getTitle() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.TextView getArtist() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.TextView getDuration() {
                return null;
            }
        }
    }
}