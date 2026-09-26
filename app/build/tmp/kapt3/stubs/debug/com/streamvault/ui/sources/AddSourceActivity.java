package com.streamvault.ui.sources;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000e\n\u0000\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u000b\u001a\u00020\fH\u0002J\b\u0010\r\u001a\u00020\fH\u0002J\u0010\u0010\u000e\u001a\u00020\f2\u0006\u0010\u000f\u001a\u00020\bH\u0002J\u0010\u0010\u0010\u001a\u00020\f2\u0006\u0010\u000f\u001a\u00020\bH\u0002J\b\u0010\u0011\u001a\u00020\fH\u0002J\"\u0010\u0012\u001a\u00020\f2\u0006\u0010\u0013\u001a\u00020\u00042\u0006\u0010\u0014\u001a\u00020\u00042\b\u0010\u0015\u001a\u0004\u0018\u00010\u0016H\u0014J\u0012\u0010\u0017\u001a\u00020\f2\b\u0010\u0018\u001a\u0004\u0018\u00010\u0019H\u0014J\b\u0010\u001a\u001a\u00020\fH\u0002J\b\u0010\u001b\u001a\u00020\fH\u0002J\u0010\u0010\u001c\u001a\u00020\f2\u0006\u0010\u001d\u001a\u00020\u0004H\u0002J\u0010\u0010\u001e\u001a\u00020\f2\u0006\u0010\u001f\u001a\u00020 H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006!"}, d2 = {"Lcom/streamvault/ui/sources/AddSourceActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "PICK_FILE", "", "binding", "Lcom/streamvault/databinding/ActivityAddSourceBinding;", "editingSource", "Lcom/streamvault/data/model/SavedSource;", "repo", "Lcom/streamvault/data/repository/MainRepository;", "clearM3uForm", "", "clearXtreamForm", "confirmDelete", "src", "editSource", "loadExistingSources", "onActivityResult", "requestCode", "resultCode", "data", "Landroid/content/Intent;", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "setupButtons", "setupTabs", "showTab", "tab", "toast", "msg", "", "app_debug"})
public final class AddSourceActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.streamvault.databinding.ActivityAddSourceBinding binding;
    private com.streamvault.data.repository.MainRepository repo;
    private final int PICK_FILE = 101;
    @org.jetbrains.annotations.Nullable
    private com.streamvault.data.model.SavedSource editingSource;
    
    public AddSourceActivity() {
        super();
    }
    
    @java.lang.Override
    protected void onCreate(@org.jetbrains.annotations.Nullable
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupTabs() {
    }
    
    private final void showTab(int tab) {
    }
    
    private final void setupButtons() {
    }
    
    @java.lang.Override
    protected void onActivityResult(int requestCode, int resultCode, @org.jetbrains.annotations.Nullable
    android.content.Intent data) {
    }
    
    private final void loadExistingSources() {
    }
    
    private final void editSource(com.streamvault.data.model.SavedSource src) {
    }
    
    private final void confirmDelete(com.streamvault.data.model.SavedSource src) {
    }
    
    private final void clearM3uForm() {
    }
    
    private final void clearXtreamForm() {
    }
    
    private final void toast(java.lang.String msg) {
    }
}