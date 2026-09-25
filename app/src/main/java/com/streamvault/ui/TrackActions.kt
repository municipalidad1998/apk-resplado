@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.streamvault.ui

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streamvault.data.Track
import com.streamvault.processing.Availability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun TrackActions(vm: LibraryViewModel, initialTrack: Track, dismiss: () -> Unit) {
    val trackFlow = remember(initialTrack.id) { vm.app.library.observeTrack(initialTrack.id) }
    val live by trackFlow.collectAsStateWithLifecycle(initialTrack)
    val track = live ?: initialTrack
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var action by remember { mutableStateOf("menu") }
    val image = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.cover(track, uri); dismiss() }
    fun runAndClose(block: () -> Unit) { block(); dismiss() }
    if (action == "menu") ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
            item { TrackRow(track, onPlay = { runAndClose { vm.play(track) } }, onMenu = dismiss, trailing = {}) ; HorizontalDivider(Modifier.padding(vertical = 12.dp)) }
            val actions = listOf(
                Triple("Reproducir", Icons.Rounded.PlayArrow) { runAndClose { vm.play(track) } },
                Triple("Reproducir después", Icons.Rounded.SkipNext) { runAndClose { vm.enqueue(track, true) } },
                Triple("Agregar a cola", Icons.Rounded.QueueMusic) { runAndClose { vm.enqueue(track) } },
                Triple("Agregar álbum a cola", Icons.Rounded.Album) { runAndClose { vm.enqueueAlbum(track) } },
                Triple("Agregar a playlist", Icons.Rounded.PlaylistAdd) { action = "playlist" },
                Triple(if (track.favorite) "Quitar de favoritos" else "Favorito", Icons.Rounded.FavoriteBorder) { runAndClose { vm.favorite(track) } },
                Triple("Editar información", Icons.Rounded.Edit) { action = "edit" },
                Triple("Cambiar portada", Icons.Rounded.Image) { image.launch(arrayOf("image/*")) },
                Triple("Inicio y transición / crossfade", Icons.Rounded.GraphicEq) { action = "offset" },
                Triple("Medir volumen y nivelar", Icons.Rounded.VolumeUp) { vm.measureLoudness(track); dismiss() },
                Triple("Quitar voz · requiere modelo", Icons.Rounded.MicOff) { action = "separate" },
                Triple("Buscar en YouTube", Icons.Rounded.PlayCircle) { runAndClose { YouTubeLinks.open(context, YouTubeLinks.query(track.title, track.artist)) } },
                Triple("Compartir", Icons.Rounded.Share) {
                    runCatching {
                        val original = Uri.parse(track.uri)
                        val uri = if (original.scheme == "file") FileProvider.getUriForFile(context, "${context.packageName}.files", File(original.path!!)) else original
                        val intent = Intent(Intent.ACTION_SEND).setType(context.contentResolver.getType(uri) ?: "audio/*").putExtra(Intent.EXTRA_STREAM, uri)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply { clipData = ClipData.newRawUri("Audio", uri) }
                        context.startActivity(Intent.createChooser(intent, "Compartir archivo original"))
                    }.onFailure { vm.notify("No se pudo compartir. Comprueba el permiso del archivo.") }
                    dismiss()
                },
                Triple("Abrir ubicación", Icons.Rounded.FolderOpen) {
                    vm.task {
                        val location = vm.app.library.locations(track.id).firstOrNull { it.root.startsWith("content:") }
                        try {
                            if (location != null) {
                                val tree = Uri.parse(location.root)
                                val document = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
                                context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(document, DocumentsContract.Document.MIME_TYPE_DIR).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                            } else {
                                val initial = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:${track.folder.trim('/')}")
                                context.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).putExtra(DocumentsContract.EXTRA_INITIAL_URI, initial))
                            }
                        } catch (_: Exception) { action = "info"; vm.notify("Este dispositivo no permite abrir esa ubicación. Aquí está la ruta y URI.") }
                    }
                },
                Triple("Ver información", Icons.Rounded.Info) { action = "info" },
                Triple("Eliminar de biblioteca", Icons.Rounded.HideSource) { action = "hide" }
            )
            items(actions) { (label, icon, click) -> ListItem(headlineContent = { Text(label) }, leadingContent = { Icon(icon, null) }, modifier = Modifier.clickable(onClick = click)) }
        }
    }
    when (action) {
        "edit" -> EditTrack(track, dismiss) { name, title, artist, album, genre, notes, tags -> vm.edit(track, name, title, artist, album, genre, notes, tags); dismiss() }
        "playlist" -> AlertDialog(onDismissRequest = dismiss, title = { Text("Agregar a playlist") }, text = {
            LazyColumn {
                if (playlists.isEmpty()) item { Text("Crea tu primera playlist con el botón de abajo.") }
                items(playlists) { playlist -> TextButton(onClick = { vm.addToPlaylist(track, playlist); dismiss() }) { Text(playlist.name) } }
                item { TextButton(onClick = { action = "createPlaylist" }) { Icon(Icons.Rounded.Add, null); Text("Crear playlist") } }
            }
        }, confirmButton = { TextButton(onClick = dismiss) { Text("Cerrar") } })
        "createPlaylist" -> PlaylistEditor(null, dismiss) { name, description ->
            vm.task {
                val playlist = com.streamvault.data.Playlist(java.util.UUID.randomUUID().toString(), name, description)
                vm.app.library.playlist(playlist); vm.app.library.addToPlaylist(playlist.id, track.id)
                vm.notify("Playlist creada y canción agregada")
            }; dismiss()
        }
        "offset" -> OffsetDialog(vm, track, busy, dismiss)
        "info" -> AlertDialog(onDismissRequest = dismiss, title = { Text("Información del audio") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Info("ARCHIVO ORIGINAL", track.fileName); Info("NOMBRE EN LÚMINA", track.displayName)
                Info("ORIGEN", if (track.source == "whatsapp") "WhatsApp · nota de voz/audio" else track.source)
                Info("DURACIÓN · TAMAÑO", "${time(track.durationMs)} · ${"%.2f".format(track.size / 1048576.0)} MB")
                Info("FECHA", if (track.date > 0) DateFormat.getDateTimeInstance().format(Date(track.date)) else "No disponible")
                Info("UBICACIÓN", track.folder); Info("URI", track.uri)
                Info("ÁLBUM · GÉNERO", "${track.album} · ${track.genre}")
                Info("REPRODUCCIONES", track.plays.toString()); Info("NOTAS", track.notes.ifBlank { "Sin notas" }); Info("ETIQUETAS", track.tags.ifBlank { "Sin etiquetas" })
                Info("IDENTIDAD SHA-256", track.id)
            }
        }, confirmButton = { TextButton(onClick = dismiss) { Text("Cerrar") } })
        "hide" -> AlertDialog(onDismissRequest = dismiss, title = { Text("¿Quitar de tu biblioteca?") }, text = { Text("El archivo original no se borrará ni se modificará. Puedes restaurar las pistas ocultas en Ajustes.") },
            confirmButton = { TextButton(onClick = { vm.hide(track); dismiss() }) { Text("Quitar") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar") } })
        "separate" -> SeparationDialog(vm, track, dismiss)
    }
}

@Composable
private fun Info(label: String, value: String) { Column { Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(4.dp)); Text(value, fontSize = 13.sp) } }

@Composable
private fun EditTrack(track: Track, dismiss: () -> Unit, save: (String, String, String, String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf(track.customName) }; var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artist) }; var album by remember { mutableStateOf(track.album) }
    var genre by remember { mutableStateOf(track.genre) }; var notes by remember { mutableStateOf(track.notes) }; var tags by remember { mutableStateOf(track.tags) }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Hazlo tuyo") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Solo se cambia la información en la app. El archivo original permanece intacto.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(name, { name = it }, label = { Text("Nombre personalizado") })
            OutlinedTextField(title, { title = it }, label = { Text("Título") })
            OutlinedTextField(artist, { artist = it }, label = { Text("Artista") })
            OutlinedTextField(album, { album = it }, label = { Text("Álbum") })
            OutlinedTextField(genre, { genre = it }, label = { Text("Género") })
            OutlinedTextField(tags, { tags = it }, label = { Text("Etiquetas") })
            OutlinedTextField(notes, { notes = it }, label = { Text("Descripción / notas") })
        }
    }, confirmButton = { TextButton(onClick = { save(name, title, artist, album, genre, notes, tags) }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar") } })
}

@Composable
fun OffsetDialog(vm: LibraryViewModel, track: Track, busy: Boolean, dismiss: () -> Unit) {
    var offset by remember(track.manualOffsetMs) { mutableStateOf(track.manualOffsetMs?.let { (it / 1000.0).toString() }.orEmpty()) }
    var end by remember(track.playbackEndMs) { mutableStateOf(track.playbackEndMs?.let { (it / 1000.0).toString() }.orEmpty()) }
    var crossfade by remember(track.crossfadeSeconds) { mutableStateOf(track.crossfadeSeconds?.toString().orEmpty()) }
    val state by vm.playback.collectAsStateWithLifecycle()
    val working by vm.busy.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val color = MaterialTheme.colorScheme.primary
    AlertDialog(onDismissRequest = dismiss, title = { Text("Inicio y transición") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Analiza hasta 120 segundos del inicio. Se guarda un offset de reproducción, sin recortar el original.")
            if (track.waveform.isNotEmpty()) {
                val peaks = remember(track.waveform) { track.waveform.split(',').mapNotNull(String::toFloatOrNull) }
                Canvas(Modifier.fillMaxWidth().height(72.dp)) {
                    peaks.forEachIndexed { i, value -> val x = size.width * i / peaks.size; val height = maxOf(2f, size.height * value); drawLine(color, Offset(x, (size.height - height) / 2), Offset(x, (size.height + height) / 2), maxOf(1f, size.width / peaks.size - 1)) }
                }
                Text("Forma de onda del fragmento inicial", fontSize = 10.sp)
            }
            Info("INICIO DETECTADO", "${"%.2f".format(track.detectedOffsetMs / 1000.0)} s")
            Button(onClick = { vm.analyze(track) }, enabled = !busy && !working) { Text(if (busy || working) "Analizando audio…" else "Detectar y usar inicio automático") }
            if (busy || working) LinearProgressIndicator(Modifier.fillMaxWidth())
            OutlinedTextField(offset, { offset = it }, label = { Text("Inicio manual: mm:ss o segundos") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), singleLine = true)
            Row {
                TextButton(onClick = { vm.offset(track, offset) }) { Text("Guardar punto") }
                TextButton(onClick = { vm.offset(track, null) }) { Text("Usar automático") }
            }
            Text("00:11 = 11 segundos. 0.11 = 110 milisegundos. Para silencios menores de un segundo, selecciona Silencio mínimo: 0 en Ajustes.", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text("Final útil y crossfade", style = MaterialTheme.typography.titleMedium)
            Text("Si el canto termina en 03:20 pero el archivo sigue dos minutos, fija 03:20 aquí. No se detecta automáticamente el final musical: tú eliges dónde salir.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(end, { end = it }, label = { Text("Final útil: mm:ss o segundos") }, placeholder = { Text("Vacío: final original (${time(track.durationMs)})") }, singleLine = true)
            if (state.currentId == track.id) TextButton(onClick = { end = (state.position / 1000.0).toString() }) { Text("Usar posición actual como final") }
            OutlinedTextField(crossfade, { crossfade = it }, label = { Text("Crossfade de esta pista (0–180 s)") }, placeholder = { Text("Vacío: global ${settings.crossfade} s") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            Text("La mezcla empieza antes del final útil, durante el tiempo elegido. 0 desactiva la mezcla; se limita únicamente al audio disponible.", style = MaterialTheme.typography.bodySmall)
            Row {
                TextButton(onClick = { vm.task { vm.saveTransition(track, end, crossfade) } }) { Text("Guardar transición") }
                TextButton(onClick = { end = ""; crossfade = ""; vm.task { vm.saveTransition(track, "", "") } }) { Text("Restablecer") }
            }
            HorizontalDivider()
            TextButton(onClick = { vm.play(track, original = true); dismiss() }) { Text("Reproducir desde el inicio original") }
            TextButton(onClick = { vm.play(track); dismiss() }) { Text("Reproducir desde el inicio guardado") }
        }
    }, confirmButton = { TextButton(onClick = dismiss) { Text("Cerrar") } })
}

@Composable
private fun SeparationDialog(vm: LibraryViewModel, track: Track, dismiss: () -> Unit) {
    var availability by remember { mutableStateOf<Availability?>(null) }
    LaunchedEffect(Unit) { availability = vm.app.separation.provider.availability(vm.app) }
    val workFlow = remember(track.id) { androidx.work.WorkManager.getInstance(vm.app).getWorkInfosForUniqueWorkFlow("separate-${track.id}") }
    val works by workFlow.collectAsStateWithLifecycle(emptyList())
    val work = works.firstOrNull()
    val running = work?.state == androidx.work.WorkInfo.State.RUNNING || work?.state == androidx.work.WorkInfo.State.ENQUEUED
    var result by remember(work?.state) { mutableStateOf<Track?>(null) }
    LaunchedEffect(work?.state) { work?.outputData?.getString("instrumental")?.let { result = vm.app.library.track(it) } }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/*")) { uri ->
        if (uri != null) result?.let { audio -> vm.task {
            withContext(Dispatchers.IO) {
                vm.app.contentResolver.openInputStream(Uri.parse(audio.uri))!!.use { input ->
                    vm.app.contentResolver.openOutputStream(uri)!!.use { output -> input.copyTo(output) }
                }
            }
            vm.notify("Instrumental exportado. El original permanece intacto.")
        } }
    }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Quitar voz") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
            Text(if (availability is Availability.Unavailable) "No hay un motor de separación instalado" else "Voz e instrumental", style = MaterialTheme.typography.titleMedium)
            Text(when (val value = availability) { is Availability.Unavailable -> value.reason; Availability.Ready -> "Motor: ${vm.app.separation.provider.name}"; null -> "Comprobando motor…" })
            if (running) {
                val percent = work?.progress?.getInt("percent", 0) ?: 0
                Text("Procesando audio… $percent%")
                LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
                Text(work?.progress?.getString("stage").orEmpty(), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { androidx.work.WorkManager.getInstance(vm.app).cancelUniqueWork("separate-${track.id}") }) { Text("Cancelar procesamiento") }
            } else if (availability == Availability.Ready) {
                Button(onClick = { com.streamvault.processing.SeparationWorker.enqueue(vm.app, track) }) { Text("Separar voz e instrumental") }
            }
            work?.outputData?.getString("error")?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            result?.let { audio ->
                TextButton(onClick = { vm.play(track) }) { Text("Escuchar original") }
                TextButton(onClick = { vm.play(audio) }) { Text("Escuchar instrumental") }
                TextButton(onClick = { vm.task { work?.outputData?.getString("vocals")?.let { vm.app.library.track(it)?.let { voice -> vm.play(voice) } } } }) { Text("Escuchar voz") }
                Button(onClick = { export.launch(audio.fileName) }) { Text("Guardar instrumental como…") }
            }
            Text("Escuchar el original no elimina la voz. Solo un procesamiento terminado genera una nueva pista Instrumental. Esta versión no incluye un modelo de separación.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }, confirmButton = { TextButton(onClick = dismiss) { Text("Cerrar") } })
}
