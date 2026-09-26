package com.streamvault.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.streamvault.scanner.ScanWorker

class MainActivity : ComponentActivity() {
    private val vm: LibraryViewModel by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private val rescan = Runnable { if (vm.settings.value.autoScan) vm.scan() }
    private var observing = false
    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) { handler.removeCallbacks(rescan); handler.postDelayed(rescan, 1500) }
    }
    private val permission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[audioPermission()] == true) { registerObserver(); vm.scan() }
        else vm.notify("Puedes seleccionar carpetas sin dar acceso a toda tu música. También puedes habilitar el permiso en Ajustes de Android.")
    }
    private val tree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            vm.addRoot(uri)
        }.onFailure { vm.notify("No se pudo conservar el permiso de esta carpeta") }
    }
    private fun audioPermission() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    private fun hasPermission() = ContextCompat.checkSelfPermission(this, audioPermission()) == PackageManager.PERMISSION_GRANTED
    fun requestAudio() {
        val permissions = mutableListOf(audioPermission())
        if (Build.VERSION.SDK_INT >= 33) permissions += Manifest.permission.POST_NOTIFICATIONS
        permission.launch(permissions.toTypedArray())
    }
    fun chooseFolder() { tree.launch(null) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LuminaAppUI(vm, ::requestAudio, ::chooseFolder) }
        ScanWorker.schedule(this, vm.settings.value.autoScan)
        vm.checkUpdates(manual = false)
        if (vm.settings.value.autoScan && (hasPermission() || vm.roots.value.isNotEmpty())) vm.scan()
    }
    private fun registerObserver() {
        if (!observing && hasPermission()) {
            runCatching { contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer); observing = true }
        }
    }
    override fun onStart() { super.onStart(); registerObserver() }
    override fun onStop() { if (observing) { contentResolver.unregisterContentObserver(observer); observing = false }; handler.removeCallbacks(rescan); super.onStop() }
}
