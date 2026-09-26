package com.streamvault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

/** Update section shown inside Ajustes, plus the dialogs used when the user acts on an update. */
@Composable
fun UpdateSection(vm: LibraryViewModel) {
    val state by vm.updateState.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    SectionTitle("Actualizaciones")
    val subtitle = when (state) {
        UpdateState.Idle -> "Instalada: ${versionLabel()}. Revisa si hay una versión nueva."
        UpdateState.Checking -> "Consultando GitHub…"
        is UpdateState.UpToDate -> "Tienes la última versión (${(state as UpdateState.UpToDate).version})."
        is UpdateState.Available -> "Versión ${(state as UpdateState.Available).info.version} disponible."
        is UpdateState.Downloading -> "Descargando… ${(state as UpdateState.Downloading).percent}%"
        is UpdateState.Failed -> (state as UpdateState.Failed).reason
        UpdateState.NeedsInstallPermission -> "Autoriza instalar desde esta fuente y vuelve a intentarlo."
        is UpdateState.SignatureConflict -> "La versión instalada tiene otra firma: Android exige desinstalarla primero."
    }
    Setting("Buscar actualizaciones", subtitle, vm::checkUpdates)
    Toggle("Buscar automáticamente", "Al abrir la app, como máximo una vez cada 6 horas", settings.autoUpdate) { value ->
        vm.app.preferences.update { it.copy(autoUpdate = value) }
    }
    when (val current = state) {
        UpdateState.Checking -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
        is UpdateState.Downloading -> {
            LinearProgressIndicator(progress = { current.percent / 100f }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            Text("Descargando ${current.percent}% · se abrirá el instalador de Android al terminar.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        is UpdateState.Available -> {
            Spacer(Modifier.height(6.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nueva versión ${current.info.version}", style = MaterialTheme.typography.titleMedium)
                    if (current.info.notes.isNotBlank()) Text(current.info.notes.take(600), fontSize = 12.sp, maxLines = 8)
                    if (current.info.sizeBytes > 0) Text("Tamaño: ${(current.info.sizeBytes / 10485.6).roundToInt() / 100.0} MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.installUpdate(current.info) }) { Text("Descargar e instalar") }
                        TextButton(onClick = { vm.skipUpdate(current.info) }) { Text("Más tarde") }
                    }
                    var sameSignature by remember(current.info) { mutableStateOf<Boolean?>(null) }
                    LaunchedEffect(current.info) {
                        sameSignature = withContext(Dispatchers.IO) {
                            val file = java.io.File(vm.app.cacheDir, "updates/${current.info.apkName}")
                            if (file.exists()) vm.updates.signatureMatches(file) else null
                        }
                    }
                    if (sameSignature == false) Text("Este APK está firmado distinto a la app instalada. Android pedirá desinstalar antes; haz un respaldo de tus ajustes si quieres conservarlos.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    Text("Se descarga el APK publicado en GitHub y Android pide confirmación antes de instalar. "
                        + "Tus canciones, colas y ajustes se conservan porque la app mantiene la misma firma.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        is UpdateState.Failed -> Text(current.reason, fontSize = 12.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 6.dp))
        else -> Unit
    }
}

@Composable
fun UpdateDialog(vm: LibraryViewModel) {
    val state by vm.updateState.collectAsStateWithLifecycle()
    when (val current = state) {
        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Descargando actualización") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LinearProgressIndicator(progress = { current.percent / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("${current.percent}% · no cierres la app")
                }
            },
            confirmButton = {}
        )
        is UpdateState.Failed -> AlertDialog(
            onDismissRequest = vm::dismissUpdate,
            title = { Text("No se pudo actualizar") },
            text = { Text(current.reason) },
            confirmButton = { TextButton(onClick = vm::dismissUpdate) { Text("Aceptar") } }
        )
        UpdateState.NeedsInstallPermission -> AlertDialog(
            onDismissRequest = vm::dismissUpdate,
            title = { Text("Autoriza la instalación") },
            text = { Text("Android pide permiso una sola vez para que esta app instale sus propias actualizaciones. Activa «Permitir desde esta fuente» y toca Reintentar.") },
            confirmButton = { TextButton(onClick = { vm.dismissUpdate(); vm.retryPendingInstall() }) { Text("Reintentar") } },
            dismissButton = { TextButton(onClick = vm::dismissUpdate) { Text("Cancelar") } }
        )
        is UpdateState.SignatureConflict -> AlertDialog(
            onDismissRequest = vm::dismissUpdate,
            title = { Text("Android no puede actualizarla así") },
            text = { Text("Ya existe una instalación de esta app firmada con otra clave, y Android no permite reemplazarla sin desinstalar antes. "
                + "Guarda tus ajustes en Ajustes → Respaldo, desinstala y vuelve a instalar; desde esa versión las futuras actualizaciones sí se instalan sin desinstalar. "
                + (current.detail?.let { "\n\nDetalle de Android: $it" } ?: "")) },
            confirmButton = { TextButton(onClick = { vm.dismissUpdate(); vm.uninstallCurrentBuild() }) { Text("Desinstalar la actual") } },
            dismissButton = { TextButton(onClick = vm::dismissUpdate) { Text("Cancelar") } }
        )
        is UpdateState.UpToDate -> AlertDialog(
            onDismissRequest = vm::dismissUpdate,
            title = { Text("Al día") },
            text = { Text("Ya tienes la versión ${current.version}. No hay actualizaciones pendientes.") },
            confirmButton = { TextButton(onClick = vm::dismissUpdate) { Text("Aceptar") } }
        )
        else -> Unit
    }
}

private fun versionLabel(): String = com.streamvault.BuildConfig.VERSION_NAME
