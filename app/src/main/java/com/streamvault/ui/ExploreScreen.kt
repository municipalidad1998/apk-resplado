package com.streamvault.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streamvault.online.OnlineResult
import com.streamvault.online.Quality

/** Online search. Same player, queue and playlists as the local files; only the source changes. */
@Composable
fun ExploreScreen(vm: LibraryViewModel) {
    val results by vm.onlineResults.collectAsStateWithLifecycle()
    val searching by vm.onlineSearching.collectAsStateWithLifecycle()
    val error by vm.onlineError.collectAsStateWithLifecycle()
    val net by vm.net.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val suggestions = listOf("Imagine Dragons Believer", "música cristiana", "instrumental piano", "salsa en vivo", "banda sonora", "cover acústico", "reguetón instrumental", "alabanza")

    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        PageHeader("MÚSICA ONLINE", "Explorar", "Catálogo libre, sin anuncios y con pantalla apagada")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().testTag("online-search"),
            singleLine = true,
            placeholder = { Text("Canción, artista, álbum, concierto…") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) ActionIcon(Icons.Rounded.Close, "Borrar", { query = ""; vm.onlineResults.value = emptyList() }) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.searchOnline(query) }),
            shape = RoundedCornerShape(16.dp)
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            val (color, text) = when {
                !net.online -> MaterialTheme.colorScheme.error to "Sin conexión"
                net.slow -> MaterialTheme.colorScheme.tertiary to "Conexión lenta"
                else -> MaterialTheme.colorScheme.secondary to if (net.metered) "Datos móviles" else "Wi‑Fi"
            }
            Box(Modifier.size(8.dp).padding(end = 0.dp))
            Surface(color = color, shape = RoundedCornerShape(50), modifier = Modifier.size(9.dp)) {}
            Spacer(Modifier.width(7.dp))
            Text(text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("Calidad:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            var expanded by remember { mutableStateOf(false) }
            TextButton(onClick = { expanded = true }) { Text(Quality.values().firstOrNull { it.key == settings.onlineQuality }?.label ?: "Automática", fontSize = 12.sp) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                Quality.values().forEach { quality ->
                    DropdownMenuItem(text = { Text(quality.label) }, onClick = {
                        vm.app.preferences.update { it.copy(onlineQuality = quality.key) }
                        expanded = false
                    })
                }
            }
        }
        if (!net.online) {
            Text("Sin Internet no se puede reproducir música online. Tu música local y FLAC sigue disponible.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (searching) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }

        if (results.isEmpty() && !searching) {
            LazyColumn(Modifier.weight(1f)) {
                item {
                    Text("Prueba con", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                    WrapRow(suggestions) { suggestion ->
                        SuggestionChip(onClick = { query = suggestion; vm.searchOnline(suggestion) }, label = { Text(suggestion, fontSize = 12.sp) })
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Sobre este catálogo", style = MaterialTheme.typography.titleMedium)
                    Text("La música viene de Internet Archive: grabaciones en vivo, netlabels y obras de dominio público, "
                        + "publicadas con licencias que permiten escucharlas y compartirlas. No es el catálogo comercial, así que "
                        + "no encontrarás éxitos de grandes sellos; ninguna API gratuita y legal ofrece eso. "
                        + "Aquí no hay anuncios insertados por la app, en ninguna calidad.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(results, key = { it.id }) { result -> OnlineRow(vm, result) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WrapRow(items: List<String>, content: @Composable (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { content(it) }
    }
}

@Composable
private fun OnlineRow(vm: LibraryViewModel, result: OnlineResult) {
    val best = result.bestStream
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { vm.playOnline(result) }.padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(result.id, result.title, result.cover, Modifier.size(54.dp))
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(result.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            Text(result.artist, maxLines = 1, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (best?.lossless == true) AssistChip(onClick = {}, label = { Text("FLAC", fontSize = 9.sp) }, modifier = Modifier.height(22.dp))
                else Text(best?.label ?: "Audio", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(time((result.durationSeconds * 1000).toLong()), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        ActionIcon(Icons.Rounded.PlaylistAdd, "Agregar a la cola", { vm.enqueueOnline(result) })
    }
}
