package com.streamvault.ui

import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import com.streamvault.BuildConfig
import com.streamvault.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.streamvault.analysis.AnalysisWorker
import com.streamvault.online.Quality
import com.streamvault.playback.LoudnessMath
import com.streamvault.playback.CompressorPreset
import com.streamvault.playback.PlaybackEvents
import com.streamvault.scanner.ScanWorker

@Composable
fun SettingsScreen(vm: LibraryViewModel, permission: () -> Unit, folder: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val roots by vm.roots.collectAsStateWithLifecycle()
    val scans by vm.scan.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var choose by remember { mutableStateOf("") }
    var exclusions by remember { mutableStateOf(false) }
    var excluded by remember(settings.excludedFolders) { mutableStateOf(settings.excludedFolders) }
    val update = vm.app.preferences::update
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) vm.backupSettings(uri) }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.restoreSettings(uri) }
    LazyColumn(contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 28.dp)) {
        item { PageHeader("A TU MANERA", "Ajustes", "Pequeños detalles. Tu experiencia perfecta.") }
        item {
            SectionTitle("Reproductor")
            Setting("Crossfade", if (settings.crossfade == 0) "Desactivado" else "${settings.crossfade} segundos · curva de potencia constante", { choose = "Crossfade" })
            Setting("Saltar hacia atrás / adelante", "${settings.skipSeconds} segundos", { choose = "Saltar ± segundos" })
            Setting("Repetición", listOf("Desactivada", "Una canción", "Toda la cola")[settings.repeat], { choose = "Repetición" })
            Toggle("Aleatorio", "Descubre tu colección en otro orden", settings.shuffle) { value -> update { it.copy(shuffle = value) } }
            Toggle("Reproducción automática", "Continuar con la siguiente canción", settings.autoPlay) { value -> update { it.copy(autoPlay = value) } }
            Toggle("Fade In / Fade Out", "Entrada y salida suave de 450 ms sin crossfade", settings.fades) { value -> update { it.copy(fades = value) } }
        }
        item {
            SectionTitle("Biblioteca")
            Setting("Actualizar biblioteca", "Buscar archivos nuevos o modificados", vm::scan)
            scans.firstOrNull()?.let { work ->
                val running = work.state == WorkInfo.State.RUNNING || work.state == WorkInfo.State.ENQUEUED
                if (running) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(if (running) "Escaneando biblioteca… ${work.progress.getInt("count", 0)} archivos" else work.outputData.getString("message") ?: "Último escaneo: ${work.state}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            }
            Toggle("Escanear automáticamente", "Al abrir, cuando hay cambios y cada 6 horas", settings.autoScan) { value -> update { it.copy(autoScan = value) }; ScanWorker.schedule(context, value) }
            Toggle("Detectar inicio automáticamente", "Analizar silencios en segundo plano", settings.detectSilence) { value ->
                update { it.copy(detectSilence = value) }
                if (value) AnalysisWorker.enqueue(context) else androidx.work.WorkManager.getInstance(context).cancelUniqueWork("analysis")
            }
            Setting("Sensibilidad", when (settings.thresholdDb) { -60 -> "Alta · −60 dBFS"; -30 -> "Baja · −30 dBFS"; else -> "Equilibrada · −45 dBFS" }, { choose = "Sensibilidad" })
            Setting("Silencio mínimo", "${settings.minimumSilence} segundos", { choose = "Silencio mínimo" })
            Setting("Permisos de música", "Autorizar acceso a los audios del dispositivo", permission)
            Setting("Agregar carpeta", "WhatsApp, Android/media, SD o carpeta personalizada", folder)
            roots.forEach { root ->
                ListItem(headlineContent = { Text(android.net.Uri.decode(root.substringAfterLast('/')), fontSize = 13.sp) }, supportingContent = { Text("Acceso persistente", fontSize = 11.sp) }, trailingContent = { ActionIcon(Icons.Rounded.Close, "Quitar acceso a la carpeta", { vm.removeRoot(root) }) })
            }
            Setting("Carpetas excluidas", "${settings.excludedFolders.lines().count { it.isNotBlank() }} filtros · por nombre o ruta", { exclusions = true })
            Setting("Restaurar pistas ocultas", "Volver a mostrar archivos eliminados de la biblioteca", { vm.task { vm.app.library.unhideAll(); vm.notify("Pistas restauradas") } })
        }
        item {
            SectionTitle("Apariencia")
            Setting("Tema", when (settings.theme) { "dark" -> "Oscuro"; "light" -> "Claro"; else -> "Automático según el sistema" }, { choose = "Tema" })
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) Toggle("Colores del sistema", "Adaptar la paleta al fondo de pantalla de Android 12+", settings.dynamicColor) { value -> update { it.copy(dynamicColor = value) } }
            Toggle("Portadas grandes", "Más espacio para el arte de tu música", settings.largeCovers) { value -> update { it.copy(largeCovers = value) } }
            Toggle("Transiciones suaves", "Animar los cambios de pantalla", settings.animations) { value -> update { it.copy(animations = value) } }
        }
        item {
            SectionTitle("Audio")
            Setting("Calidad original", "Sin transcodificación. La compatibilidad depende del decodificador de Android.")
            Toggle("Volumen parejo", "Nivela tus canciones para que ninguna suene más bajo que las demás", settings.normalize) { value -> update { it.copy(normalize = value) } }
            Setting("Loudness objetivo", if (settings.normalize) "${settings.targetLoudnessDb} LUFS · ${if (settings.targetLoudnessDb >= -12) "más fuerte" else if (settings.targetLoudnessDb <= -18) "más suave" else "equilibrado"}" else "Desactivado", { choose = "Loudness objetivo" })
            Text("Se mide el loudness real de cada canción (BS.1770, con curva K y doble compuerta) y la ganancia se aplica al reproducir, sin tocar el archivo. "
                + "Si el pico verdadero no deja subir todo lo necesario, la ganancia se limita a ${LoudnessMath.TRUE_PEAK_CEILING_DBTP} dBTP para no distorsionar.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
            Setting("Medir el volumen de tu biblioteca", "Analiza el volumen real de cada canción en segundo plano", { AnalysisWorker.enqueue(context); vm.notify("Midiendo el volumen de la biblioteca en segundo plano") })
            Setting("Compresor", CompressorPreset.from(settings.compressor).label + if (settings.compressor == "off") "" else " · ${CompressorPreset.from(settings.compressor).description}", { choose = "Compresor" })
            Text("El compresor baja lo muy fuerte y sube lo muy suave dentro de la misma canción, como el efecto Compresor de Audacity, y un limitador a −1 dB evita que se distorsione al subir el volumen. "
                + "Funciona en tiempo real, sobre la reproducción: el archivo original nunca se modifica. Requiere Android 9 o superior.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
            Text("Se decodifica el audio de cada pista para medir su volumen real y se guarda un ajuste por canción. "
                + "Las canciones más bajas se amplifican con el efecto de sonido del sistema y las más fuertes se atenúan; el archivo original nunca cambia. "
                + "Es una medición RMS de una ventana de 90 segundos, no loudness LUFS de broadcast.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
            Setting("Ecualizador del dispositivo", "Disponible solo si Android incluye un panel compatible", {
                try {
                    context.startActivity(Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                        .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, PlaybackEvents.audioSessionId.value)
                        .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC))
                } catch (_: Exception) { vm.notify("Este dispositivo no ofrece un ecualizador compatible") }
            })
            UpdateSection(vm)
            SectionTitle("Telegram")
            TelegramSection(vm)
            SectionTitle("Datos y calidad")
            Toggle("Usar datos móviles", "Reproducir música online con la red del operador", settings.mobileData) { value -> update { it.copy(mobileData = value) } }
            Toggle("Solo Wi‑Fi", "No consumir datos móviles en ninguna calidad", settings.wifiOnly) { value -> update { it.copy(wifiOnly = value) } }
            Setting("Calidad online", Quality.values().firstOrNull { it.key == settings.onlineQuality }?.label ?: "Automática", { choose = "Calidad online" })
            Text("Automática elige FLAC o la mejor calidad con Wi‑Fi, una calidad media con datos móviles y la más baja en conexiones lentas. "
                + "Tus archivos FLAC locales se reproducen tal cual, sin convertirlos, incluso sin Internet.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
            SectionTitle("Respaldo")
            Setting("Guardar mis ajustes", "Exporta la configuración a un archivo que puedes conservar", { backup.launch("ajustes-reproductor-denilson.json") })
            Setting("Restaurar mis ajustes", "Recupera la configuración desde un respaldo anterior", { restore.launch(arrayOf("application/json", "text/*")) })
            Text("La biblioteca, las colas y los ajustes se conservan al actualizar. Si alguna vez tienes que desinstalar, este respaldo te devuelve la configuración; "
                + "Android también puede restaurar la base de datos automáticamente al reinstalar con la misma cuenta.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
            SectionTitle("Hecha para tu música")
            Text("${com.streamvault.BuildConfig.VERSION_NAME} · ${context.getString(R.string.app_name)}\nLocal por naturaleza. Sin cuenta, sin anuncios y sin subir tus archivos. La separación de voz necesita un proveedor adicional.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
        }
    }
    if (choose.isNotEmpty()) {
        val options: List<Pair<String, String>> = when (choose) {
            "Crossfade" -> listOf(0, 2, 5, 10, 15, 20, 30, 60, 90, 120, 180).map { it.toString() to if (it == 0) "Desactivado" else "$it segundos" }
            "Saltar ± segundos" -> listOf(5, 10, 15, 30).map { it.toString() to "$it segundos" }
            "Repetición" -> listOf("0" to "Desactivada", "1" to "Una canción", "2" to "Toda la cola")
            "Sensibilidad" -> listOf("-60" to "Alta · −60 dBFS", "-45" to "Equilibrada · −45 dBFS", "-30" to "Baja · −30 dBFS")
            "Silencio mínimo" -> listOf(0, 1, 2, 3, 5).map { it.toString() to "$it segundos" }
            "Loudness objetivo" -> listOf(-20, -18, -16, -14, -12).map { it.toString() to "$it LUFS" }
            "Compresor" -> CompressorPreset.ALL.map { it.key to it.label }
            "Calidad online" -> Quality.values().map { it.key to it.label }
            else -> listOf("system" to "Automático según el sistema", "dark" to "Oscuro", "light" to "Claro")
        }
        AlertDialog(onDismissRequest = { choose = "" }, title = { Text(choose) }, text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) { options.forEach { (key, label) -> TextButton(onClick = {
                val reanalyze = choose == "Sensibilidad" || choose == "Silencio mínimo"
                update { old -> when (choose) {
                    "Crossfade" -> old.copy(crossfade = key.toInt()); "Saltar ± segundos" -> old.copy(skipSeconds = key.toInt())
                    "Repetición" -> old.copy(repeat = key.toInt()); "Sensibilidad" -> old.copy(thresholdDb = key.toInt())
                    "Silencio mínimo" -> old.copy(minimumSilence = key.toInt()); "Loudness objetivo" -> old.copy(targetLoudnessDb = key.toInt())
                    "Compresor" -> old.copy(compressor = key); "Calidad online" -> old.copy(onlineQuality = key); else -> old.copy(theme = key)
                } }
                if (reanalyze && vm.settings.value.detectSilence) AnalysisWorker.enqueue(context)
                choose = ""
            }, modifier = Modifier.fillMaxWidth()) { Text(label) } } }
        }, confirmButton = { TextButton(onClick = { choose = "" }) { Text("Cerrar") } })
    }
    if (exclusions) AlertDialog(onDismissRequest = { exclusions = false }, title = { Text("Carpetas excluidas") }, text = {
        Column { Text("Un nombre o fragmento de ruta por línea. Los cambios se aplican en el próximo escaneo.")
            OutlinedTextField(excluded, { excluded = it }, minLines = 4, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Ringtones\nNotifications") }) }
    }, confirmButton = { TextButton(onClick = { update { it.copy(excludedFolders = excluded) }; vm.scan(); exclusions = false }) { Text("Guardar y escanear") } }, dismissButton = { TextButton(onClick = { exclusions = false }) { Text("Cancelar") } })
}

@Composable
fun Setting(title: String, subtitle: String, click: (() -> Unit)? = null) {
    ListItem(headlineContent = { Text(title) }, supportingContent = { Text(subtitle, fontSize = 12.sp) }, trailingContent = { if (click != null) Icon(Icons.Rounded.ChevronRight, null) }, modifier = if (click != null) Modifier.clickable(onClick = click) else Modifier)
}
@Composable
fun Toggle(title: String, subtitle: String, checked: Boolean, change: (Boolean) -> Unit) {
    ListItem(headlineContent = { Text(title) }, supportingContent = { Text(subtitle, fontSize = 12.sp) }, trailingContent = { Switch(checked, change) }, modifier = Modifier.clickable { change(!checked) })
}

@Composable
private fun TelegramSection(vm: LibraryViewModel) {
    val count by vm.telegram.count.collectAsStateWithLifecycle(0)
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.importTelegram(uri) }
    Setting("Importar música de Telegram", "Lee el respaldo exportado por Telegram y guarda sus metadatos", { open.launch(arrayOf("application/json", "text/*")) })
    Text(if (count == 0) "Aún no hay música de Telegram registrada."
        else "$count canciones registradas con su id de mensaje, nombre, artista, duración y tamaño. "
            + "Las que siguen en el teléfono se reproducen igual que el resto de tu biblioteca; el resto conserva sus datos para cuando vuelvas a tener el archivo.",
        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
    Text("Reproducir directamente desde los servidores de Telegram necesita un cliente MTProto con tus propias credenciales de API. "
        + "Mientras tanto, la importación mantiene la canción en tu biblioteca con todos sus datos.",
        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
}
