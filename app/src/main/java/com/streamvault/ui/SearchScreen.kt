package com.streamvault.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streamvault.data.Playlist
import com.streamvault.data.Track
import com.streamvault.online.Quality
import com.streamvault.provider.MusicSource
import com.streamvault.provider.SearchHit
import com.streamvault.search.SearchTab

/**
 * One search box, three tabs, and two lists that are never mixed:
 *
 *  - TELÉFONO: only files that physically exist on the device. Works with no connection at all.
 *  - ONLINE: only what the internet providers returned, each row saying where it comes from.
 *  - TODOS: both, one section under the other, each keeping its own badge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(vm: LibraryViewModel, menu: (Track) -> Unit) {
    val outcome by vm.outcome.collectAsStateWithLifecycle()
    val tab by vm.searchTab.collectAsStateWithLifecycle()
    val searching by vm.searching.collectAsStateWithLifecycle()
    val current by vm.current.collectAsStateWithLifecycle()
    val net by vm.net.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf(vm.searchQuery.value) }
    val suggestions = listOf("Alex Campos", "La IBI", "Tu poeta Alex Campos", "Al taller del maestro", "música cristiana", "instrumental piano", "alabanza", "salsa en vivo")

    var picker by remember { mutableStateOf<SearchHit?>(null) }
    LaunchedEffect(text) { vm.search(text) }

    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        PageHeader("BUSCAR", "Buscar", "Tu teléfono primero, Internet después. Nunca mezclados.")
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().testTag("unified-search"),
            singleLine = true,
            placeholder = { Text("Canción, artista o álbum…") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = { if (text.isNotEmpty()) ActionIcon(Icons.Rounded.Close, "Borrar", { text = "" }) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.search(text) }),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(10.dp))
        TabRow(selectedTabIndex = tab.ordinal, containerColor = MaterialTheme.colorScheme.background) {
            SearchTab.values().forEach { option ->
                Tab(selected = tab == option, onClick = { vm.searchTab.value = option }, text = {
                    Text(option.label, fontSize = 13.sp, fontWeight = if (tab == option) FontWeight.Bold else FontWeight.Normal)
                })
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            val (color, label) = if (!net.online) MaterialTheme.colorScheme.error to "Sin conexión a Internet"
                else if (net.metered) MaterialTheme.colorScheme.tertiary to "Datos móviles"
                else MaterialTheme.colorScheme.secondary to "Wi‑Fi"
            Surface(color = color, shape = RoundedCornerShape(50), modifier = Modifier.size(9.dp)) {}
            Spacer(Modifier.width(7.dp))
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("Calidad:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            var expanded by remember { mutableStateOf(false) }
            TextButton(onClick = { expanded = true }) { Text(Quality.values().firstOrNull { it.key == settings.onlineQuality }?.label ?: "Automática", fontSize = 12.sp) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                Quality.entries.forEach { quality ->
                    DropdownMenuItem(text = { Text(quality.label) }, onClick = {
                        vm.app.preferences.update { it.copy(onlineQuality = quality.key) }
                        expanded = false
                    })
                }
            }
        }
        if (searching) LinearProgressIndicator(Modifier.fillMaxWidth())

        picker?.let { hit -> PlaylistChooser(vm, hit) { picker = null } }
        when {
            outcome.query.trim().length < 2 -> Suggestions(suggestions) { text = it }
            tab == SearchTab.PHONE -> SourceList(
                title = "MÚSICA DEL TELÉFONO", subtitle = "Archivos que existen en este dispositivo",
                hits = outcome.local, empty = "No hay coincidencias en tu teléfono",
                emptyHint = "Revisa el artista o el título. Si el archivo no tiene metadatos, edítalo desde la canción.",
                vm = vm, menu = menu, currentId = current?.id
            )
            tab == SearchTab.ONLINE -> {
                if (!net.online) OfflineNotice()
                outcome.onlineError?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
                SourceList(
                    title = "MÚSICA ONLINE", subtitle = "Resultados obtenidos por Internet",
                    hits = outcome.online, empty = if (net.online) "Sin resultados en Internet" else "Sin conexión a Internet",
                    emptyHint = "El catálogo libre no incluye éxitos de grandes sellos; ninguna API gratuita y legal los ofrece.",
                    vm = vm, menu = menu, currentId = current?.id
                )
            }
            else -> Column(Modifier.weight(1f)) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                    item { SourceHeader("TELÉFONO", "📱 Archivos en tu dispositivo", outcome.local.size) }
                    if (outcome.local.isEmpty()) item {
                        Text("Sin coincidencias en tu teléfono", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(outcome.local, key = { "all-local-${it.key}" }) { hit ->
                        val track = hit.track
                        if (track != null) TrackRow(track, current?.id == track.id, { vm.play(track, outcome.local.mapNotNull { it.track }) }, { menu(track) },
                            trailing = { SourceBadge(MusicSource.LOCAL, hit.origin) })
                    }
                    item { SourceHeader("ONLINE", "🌐 Resultados de Internet", outcome.online.size) }
                    if (!net.online) item { OfflineNotice() }
                    else if (outcome.online.isEmpty()) item {
                        outcome.onlineError?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
                            ?: Text("Sin resultados en Internet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(outcome.online, key = { "all-online-${it.key}" }) { hit -> OnlineRow(vm, hit) { picker = it } }
                }
            }
        }
    }
}

@Composable
private fun SourceHeader(title: String, subtitle: String, count: Int) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp)) {
        Text(title, fontSize = 11.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("$count", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SourceBadge(source: MusicSource, origin: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
        Column(horizontalAlignment = Alignment.End) {
            Text(if (source == MusicSource.LOCAL) "📱 Local" else "🌐 Online", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
            Text(origin, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun OfflineNotice() {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.CloudOff, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(8.dp))
        Text("Sin conexión a Internet. Tu música del teléfono, tus playlists y tus descargas siguen disponibles.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun SourceList(title: String, subtitle: String, hits: List<SearchHit>, empty: String, emptyHint: String,
                       vm: LibraryViewModel, menu: (Track) -> Unit, currentId: String?) {
    if (hits.isEmpty()) {
        LazyColumn(Modifier.weight(1f)) { item { EmptyState(empty, emptyHint, Icons.Rounded.SearchOff) } }
        return
    }
    Column(Modifier.weight(1f)) {
        SourceHeader(title, subtitle, hits.size)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(hits, key = { "${title}-${it.key}" }) { hit ->
                when (hit.source) {
                    MusicSource.LOCAL -> hit.track?.let { track ->
                        TrackRow(track, currentId == track.id, { vm.play(track, hits.mapNotNull { it.track }) }, { menu(track) },
                            trailing = { SourceBadge(MusicSource.LOCAL, hit.origin) })
                    }
                    MusicSource.ONLINE -> OnlineRow(vm, hit) { picker = it }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Suggestions(suggestions: List<String>, pick: (String) -> Unit) {
    LazyColumn(Modifier.weight(1f)) {
        item {
            Text("Prueba con", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { suggestion -> SuggestionChip(onClick = { pick(suggestion) }, label = { Text(suggestion, fontSize = 12.sp) }) }
            }
            Spacer(Modifier.height(18.dp))
            Text("Cómo busca la app", style = MaterialTheme.typography.titleMedium)
            Text("1. Escribe y se busca primero en tu teléfono; sin Internet esa pestaña sigue funcionando.\n"
                + "2. Después se consulta Internet y cada resultado dice de dónde viene.\n"
                + "3. La coincidencia exacta de artista o de título va primero; si el artista existe, no se cuela nadie más.\n"
                + "4. Ninguna canción online se convierte en archivo local, y ninguna local se sustituye por Internet.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        }
    }
}

@Composable
fun OnlineRow(vm: LibraryViewModel, hit: SearchHit, choosePlaylist: (SearchHit) -> Unit = {}) {
    val result = hit.online ?: return
    val best = result.bestStream
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { vm.playOnline(hit) }.padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(result.id, result.title, result.cover, Modifier.size(54.dp))
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(result.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(result.artist, maxLines = 1, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (best?.lossless == true) AssistChip(onClick = {}, label = { Text("FLAC", fontSize = 9.sp) }, modifier = Modifier.height(22.dp))
                else Text(best?.label ?: "Audio", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(time(hit.durationMs), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SourceBadge(MusicSource.ONLINE, hit.origin)
        hit.online?.let { online ->
            ActionIcon(Icons.Rounded.PlaylistAdd, "Agregar a playlist", { choosePlaylist(hit) })
            ActionIcon(Icons.Rounded.QueueMusic, "Agregar a la cola", { vm.enqueueOnline(online) })
        }
    }
}

/** Online songs can join a playlist: the list becomes mixed, the file is still not on the phone. */
@Composable
private fun PlaylistChooser(vm: LibraryViewModel, hit: SearchHit, dismiss: () -> Unit) {
    val lists by vm.playlists.collectAsStateWithLifecycle()
    AlertDialog(onDismissRequest = dismiss, title = { Text("Agregar a una playlist") }, text = {
        if (lists.isEmpty()) Text("Crea primero una playlist desde la pestaña Playlists.")
        else LazyColumn {
            items(lists, key = { it.id }) { playlist ->
                TextButton(onClick = { vm.addOnlineTo(hit, playlist); dismiss() }, modifier = Modifier.fillMaxWidth()) {
                    Text(playlist.name, modifier = Modifier.weight(1f))
                    Text(if (playlist.kind == Playlist.KIND_LOCAL) "Local" else if (playlist.kind == Playlist.KIND_ONLINE) "Online" else "Mixta",
                        fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }, confirmButton = { TextButton(onClick = dismiss) { Text("Cerrar") } })
}
