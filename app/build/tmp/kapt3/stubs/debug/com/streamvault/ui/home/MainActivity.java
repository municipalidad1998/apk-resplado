package com.streamvault.ui.home;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010 \n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u0010H\u0002J\b\u0010\u0011\u001a\u00020\u000eH\u0002J\u0012\u0010\u0012\u001a\u00020\u000e2\b\u0010\u0013\u001a\u0004\u0018\u00010\u0014H\u0014J\b\u0010\u0015\u001a\u00020\u000eH\u0014J\u0018\u0010\u0016\u001a\u00020\u000e2\u0006\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u0010H\u0002J\b\u0010\u001a\u001a\u00020\u000eH\u0002J\u0016\u0010\u001b\u001a\u00020\u000e2\f\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u00100\u001dH\u0002J\b\u0010\u001e\u001a\u00020\u000eH\u0002J\b\u0010\u001f\u001a\u00020\u000eH\u0002J\u0010\u0010 \u001a\u00020\u000e2\u0006\u0010!\u001a\u00020\u0018H\u0002J\u0016\u0010\"\u001a\u00020\u000e2\f\u0010#\u001a\b\u0012\u0004\u0012\u00020$0\u001dH\u0002J\u0016\u0010%\u001a\u00020\u000e2\f\u0010#\u001a\b\u0012\u0004\u0012\u00020&0\u001dH\u0002J\u0016\u0010\'\u001a\u00020\u000e2\f\u0010#\u001a\b\u0012\u0004\u0012\u00020(0\u001dH\u0002J\b\u0010)\u001a\u00020\u000eH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0007\u001a\u00020\b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000b\u0010\f\u001a\u0004\b\t\u0010\n\u00a8\u0006*"}, d2 = {"Lcom/streamvault/ui/home/MainActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/streamvault/databinding/ActivityMainBinding;", "currentTab", "", "vm", "Lcom/streamvault/ui/home/HomeViewModel;", "getVm", "()Lcom/streamvault/ui/home/HomeViewModel;", "vm$delegate", "Lkotlin/Lazy;", "filterContent", "", "q", "", "observeData", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onResume", "setEmpty", "isEmpty", "", "msg", "setupCategories", "setupCategoryChips", "cats", "", "setupNav", "setupSearch", "showCategoriesBar", "show", "showChannels", "list", "Lcom/streamvault/data/model/Channel;", "showMovies", "Lcom/streamvault/data/model/Movie;", "showSeries", "Lcom/streamvault/data/model/Series;", "updateTabUI", "app_debug"})
public final class MainActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.streamvault.databinding.ActivityMainBinding binding;
    @org.jetbrains.annotations.NotNull
    private final kotlin.Lazy vm$delegate = null;
    private int currentTab = 0;
    
    public MainActivity() {
        super();
    }
    
    private final com.streamvault.ui.home.HomeViewModel getVm() {
        return null;
    }
    
    @java.lang.Override
    protected void onCreate(@org.jetbrains.annotations.Nullable
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override
    protected void onResume() {
    }
    
    private final void setupNav() {
    }
    
    private final void setupSearch() {
    }
    
    private final void setupCategories() {
    }
    
    private final void observeData() {
    }
    
    private final void setupCategoryChips(java.util.List<java.lang.String> cats) {
    }
    
    private final void showCategoriesBar(boolean show) {
    }
    
    private final void updateTabUI() {
    }
    
    private final void showChannels(java.util.List<com.streamvault.data.model.Channel> list) {
    }
    
    private final void showMovies(java.util.List<com.streamvault.data.model.Movie> list) {
    }
    
    private final void showSeries(java.util.List<com.streamvault.data.model.Series> list) {
    }
    
    private final void setEmpty(boolean isEmpty, java.lang.String msg) {
    }
    
    private final void filterContent(java.lang.String q) {
    }
}