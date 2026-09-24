@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.streamvault.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.*
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.streamvault.data.*

@Composable
fun PlaylistsScreen(vm: LibraryViewModel, menu: (Track) -> Unit, library: () -> Unit) {
    val lists by vm.playlists.collectAsStateWithLifecycle()
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = lists.find { it.id == selectedId }
    var create by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    var add by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    val cover = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null && selected != null) vm.playlistCover(selected, uri) }
    val tracksFlow = remember(selectedId) { selectedId?.let(vm::playlistTracks) ?: kotlinx.coroutines.flow.flowOf(emptyList()) }
    val tracks by tracksFlow.collectAsStateWithLifecycle(emptyList())
    BackHandler(selected != null) { selectedId = null }
    LazyColumn(contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp)) {
        item {
            if (selected != null) TextButton(onClick = { selectedId = null }) { Icon(Icons.Rounded.ArrowBack, null); Text(" Playlists") }
            PageHeader("EL SONIDO DE TUS MOMENTOS", selected?.name ?: "Tus playlists", selected?.description ?: "Colecciones con tu propia personalidad.")
            if (selected == null) Button(onClick = { create = true }) { Icon(Icons.Rounded.Add, null); Text(" Crear playlist") }
            else {
                Cover(selected.id, selected.name, selected.cover, Modifier.size(156.dp).clickable { cover.launch(arrayOf("image/*")) })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = { tracks.firstOrNull()?.let { vm.play(it, tracks) } }, enabled = tracks.isNotEmpty()) { Icon(Icons.Rounded.PlayArrow, null); Text("Escuchar") }
                    ActionIcon(Icons.Rounded.Add, "Agregar canciones", { add = true })
                    ActionIcon(Icons.Rounded.PlaylistAdd, "Agregar playlist a la cola", { vm.enqueuePlaylist(selected) })
                    ActionIcon(Icons.Rounded.Edit, "Editar playlist", { edit = true })
                    ActionIcon(Icons.Rounded.DeleteOutline, "Eliminar playlist", { delete = true })
                }
                Text("${tracks.size} canciones · ${time(tracks.sumOf { it.durationMs })}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (tracks.isEmpty()) EmptyState("Dale vida a esta playlist", "Agrega canciones desde tu biblioteca. Usa las flechas para cambiar el orden.", action = "Agregar canciones", onAction = { add = true })
            }
        }
        if (selected == null) {
            if (lists.isEmpty()) item { EmptyState("Una banda sonora para cada día", "Para trabajar, para desconectar, para volver a ese lugar. Tu primera colección empieza contigo.", Icons.Rounded.QueueMusic) }
            items(lists, key = { it.id }) { playlist ->
                Row(Modifier.fillMaxWidth().clickable { selectedId = playlist.id }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cover(playlist.id, playlist.name, playlist.cover, Modifier.size(72.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 16.dp)) { Text(playlist.name, fontWeight = FontWeight.SemiBold); Text(playlist.description.ifBlank { "Tu selección personal" }, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Icon(Icons.Rounded.ChevronRight, null)
                }
            }
        } else items(tracks, key = { it.id }) { track ->
            Column {
                TrackRow(track, onPlay = { vm.play(track, tracks) }, onMenu = { menu(track) })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    val index = tracks.indexOf(track)
                    IconButton(onClick = { val reordered = tracks.toMutableList(); reordered.add(index - 1, reordered.removeAt(index)); vm.reorderPlaylist(selected, reordered) }, enabled = index > 0) { Icon(Icons.Rounded.ArrowUpward, "Subir ${track.displayName}") }
                    IconButton(onClick = { val reordered = tracks.toMutableList(); reordered.add(index + 1, reordered.removeAt(index)); vm.reorderPlaylist(selected, reordered) }, enabled = index < tracks.lastIndex) { Icon(Icons.Rounded.ArrowDownward, "Bajar ${track.displayName}") }
                    ActionIcon(Icons.Rounded.RemoveCircleOutline, "Quitar de playlist", { vm.removeFromPlaylist(track, selected) })
                }
            }
        }
    }
    if (create || edit) PlaylistEditor(if (edit) selected else null, { create = false; edit = false }) { name, description ->
        if (edit && selected != null) vm.updatePlaylist(selected.copy(name = name, description = description)) else vm.createPlaylist(name, description)
        create = false; edit = false
    }
    if (add && selected != null) TrackPicker(vm, "Agregar a ${selected.name}", { add = false }) { vm.addToPlaylist(it, selected) }
    if (delete && selected != null) AlertDialog(onDismissRequest = { delete = false }, title = { Text("¿Eliminar playlist?") }, text = { Text("Solo se eliminará la lista. Tus archivos y canciones permanecerán en la biblioteca.") },
        confirmButton = { TextButton(onClick = { vm.deletePlaylist(selected); selectedId = null; delete = false }) { Text("Eliminar") } }, dismissButton = { TextButton(onClick = { delete = false }) { Text("Cancelar") } })
}

@Composable
fun PlaylistEditor(playlist: Playlist?, dismiss: () -> Unit, save: (String, String) -> Unit) {
    var name by remember { mutableStateOf(playlist?.name.orEmpty()) }
    var description by remember { mutableStateOf(playlist?.description.orEmpty()) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (playlist == null) "Crear playlist" else "Editar playlist") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true)
            OutlinedTextField(description, { description = it }, label = { Text("Descripción") })
            Text("Puedes cambiar la portada tocándola al abrir la playlist.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = { save(name.trim(), description) }, enabled = name.isNotBlank()) { Text("Guardar") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar") } })
}

@Composable
fun TrackPicker(vm: LibraryViewModel, title: String, dismiss: () -> Unit, select: (Track) -> Unit) {
    var query by remember { mutableStateOf("") }
    val flow = remember(query) { Pager(PagingConfig(50)) { vm.app.library.page(searchPattern(query), "", false, "", "", "", "", "title") }.flow }
    val tracks = flow.collectAsLazyPagingItems()
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(.85f).padding(horizontal = 22.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(query, { query = it }, placeholder = { Text("Buscar en tu biblioteca") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), singleLine = true)
            LazyColumn {
                if (tracks.itemCount == 0) item { Text("No hay canciones. Actualiza la biblioteca o selecciona una carpeta.") }
                items(tracks.itemCount, key = tracks.itemKey { it.id }) { i -> tracks[i]?.let { track ->
                    TrackRow(track, onPlay = { select(track) }, onMenu = {}, trailing = { ActionIcon(Icons.Rounded.Add, "Agregar ${track.displayName}", { select(track) }) })
                } }
            }
        }
    }
}
