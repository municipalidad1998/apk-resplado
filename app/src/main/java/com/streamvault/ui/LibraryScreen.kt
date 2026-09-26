package com.streamvault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.streamvault.data.Track

@Composable
fun LibraryScreen(vm: LibraryViewModel, search: Boolean, permission: () -> Unit, folder: () -> Unit, menu: (Track) -> Unit) {
    val songs = vm.tracks.collectAsLazyPagingItems()
    val ranked by vm.ranked.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val searching = query.trim().length >= 2
    val filter by vm.filter.collectAsStateWithLifecycle()
    val count by vm.count.collectAsStateWithLifecycle()
    val current by vm.current.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val artists by vm.artists.collectAsStateWithLifecycle()
    val albums by vm.albums.collectAsStateWithLifecycle()
    val genres by vm.genres.collectAsStateWithLifecycle()
    var group by remember { mutableStateOf("") }
    val categories = listOf("Todas", "Favoritos", "Artistas", "Álbumes", "Carpetas", "Géneros", "Recientes", "Agregadas", "WhatsApp", "Grabaciones", "Instrumentales")
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        PageHeader(if (search) "ENCUENTRA TU PRÓXIMO SONIDO" else "TU UNIVERSO SONORO", if (search) "Buscar" else "Biblioteca", "$count archivos · guardados en tu dispositivo") {
            ActionIcon(Icons.Rounded.Refresh, "Actualizar biblioteca", vm::scan)
        }
        OutlinedTextField(value = query, onValueChange = { vm.query.value = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("Canciones, artistas, carpetas…") }, leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) ActionIcon(Icons.Rounded.Close, "Borrar búsqueda", { vm.query.value = "" }) }, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
        if (!search) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
            items(categories) { category ->
                FilterChip(selected = when (category) {
                    "Todas" -> filter == LibraryFilter(); "Favoritos" -> filter.favorite; "WhatsApp" -> filter.source == "whatsapp"; "Grabaciones" -> filter.source == "recording"; "Instrumentales" -> filter.source == "instrumental"
                    "Recientes" -> filter.sort == "recent"; "Agregadas" -> filter.sort == "added"; "Carpetas" -> filter.folder.isNotEmpty(); "Artistas" -> filter.artist.isNotEmpty(); "Álbumes" -> filter.album.isNotEmpty(); else -> filter.genre.isNotEmpty()
                }, onClick = {
                    when (category) {
                        "Todas" -> vm.filter.value = LibraryFilter()
                        "Favoritos" -> vm.filter.value = LibraryFilter(favorite = true)
                        "WhatsApp" -> vm.filter.value = LibraryFilter(source = "whatsapp")
                        "Grabaciones" -> vm.filter.value = LibraryFilter(source = "recording")
                        "Instrumentales" -> vm.filter.value = LibraryFilter(source = "instrumental")
                        "Recientes" -> vm.filter.value = LibraryFilter(sort = "recent")
                        "Agregadas" -> vm.filter.value = LibraryFilter(sort = "added")
                        else -> group = category
                    }
                }, label = { Text(category) })
            }
        } else Spacer(Modifier.height(16.dp))
        val detail = listOf(filter.folder, filter.artist, filter.album, filter.genre).firstOrNull { it.isNotEmpty() }
        if (detail != null) InputChip(selected = true, onClick = { vm.filter.value = LibraryFilter() }, label = { Text(detail) }, trailingIcon = { Icon(Icons.Rounded.Close, "Quitar filtro") })
        if (searching) LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 20.dp)) {
            if (ranked.isEmpty()) item { EmptyState("No encontramos esos sonidos", "Prueba otro título, artista, etiqueta o carpeta. La búsqueda prioriza las coincidencias exactas.", Icons.Rounded.SearchOff) }
            items(ranked, key = { "ranked-${it.id}" }) { track -> TrackRow(track, current?.id == track.id, { vm.playLibrary(track, ranked) }, { menu(track) }) }
        } else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 20.dp)) {
            if (songs.itemCount == 0 && songs.loadState.refresh !is LoadState.Loading) item {
                if (count == 0) {
                    EmptyState("Tu biblioteca empieza aquí", "Permite que el reproductor encuentre música, descargas y notas de voz. Para WhatsApp o una tarjeta SD puedes seleccionar una carpeta.", action = "Permitir acceso", onAction = permission)
                    OutlinedButton(onClick = folder, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.CreateNewFolder, null); Spacer(Modifier.width(8.dp)); Text("Seleccionar carpeta") }
                } else EmptyState("No encontramos esos sonidos", "Prueba otro título, artista, etiqueta o carpeta.", Icons.Rounded.SearchOff)
            }
            items(songs.itemCount, key = songs.itemKey { it.id }) { index ->
                songs[index]?.let { track -> TrackRow(track, current?.id == track.id, { vm.playLibrary(track) }, { menu(track) }) }
            }
            if (songs.loadState.refresh is LoadState.Loading || songs.loadState.append is LoadState.Loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (songs.loadState.refresh is LoadState.Error) item { TextButton(onClick = songs::retry) { Text("No se pudo cargar. Reintentar") } }
        }
    }
    if (group.isNotBlank()) AlertDialog(onDismissRequest = { group = "" }, title = { Text(group) }, text = {
        val options = when (group) { "Carpetas" -> folders; "Artistas" -> artists; "Álbumes" -> albums; else -> genres }
        LazyColumn { if (options.isEmpty()) item { Text("Todavía no hay elementos en esta categoría.") }; items(options) { value ->
            TextButton(onClick = {
                vm.filter.value = when (group) { "Carpetas" -> LibraryFilter(folder = value); "Artistas" -> LibraryFilter(artist = value); "Álbumes" -> LibraryFilter(album = value); else -> LibraryFilter(genre = value) }
                group = ""
            }) { Text(value) }
        } }
    }, confirmButton = { TextButton(onClick = { group = "" }) { Text("Cerrar") } })
}
