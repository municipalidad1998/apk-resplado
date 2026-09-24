package com.streamvault.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import com.streamvault.data.Track

@Composable
fun HomeScreen(vm: LibraryViewModel, permission: () -> Unit, folder: () -> Unit, library: (LibraryFilter) -> Unit, playlists: () -> Unit, menu: (Track) -> Unit) {
    val tracks by vm.home.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val whatsapp by vm.whatsapp.collectAsStateWithLifecycle()
    val instrumentals by vm.instrumentals.collectAsStateWithLifecycle()
    val added by vm.added.collectAsStateWithLifecycle()
    val lists by vm.playlists.collectAsStateWithLifecycle()
    val count by vm.count.collectAsStateWithLifecycle()
    val current by vm.current.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val work by vm.scan.collectAsStateWithLifecycle()
    val running = work.firstOrNull { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
    val recent = tracks.filter { it.lastPlayed > 0 }
    LazyColumn(contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 30.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.GraphicEq, null, Modifier.size(28.dp), MaterialTheme.colorScheme.primary)
                Text(" lúmina", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp, modifier = Modifier.weight(1f))
                Row(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.secondary, CircleShape)); Spacer(Modifier.width(6.dp)); Text("TU MÚSICA, LOCAL", fontSize = 8.sp, letterSpacing = 1.sp)
                }
            }
            Text("Hecha para tus oídos.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text("Un universo de música.", fontSize = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = true, onClick = {}, label = { Text("Para ti") }) }
                item { FilterChip(selected = false, onClick = { library(LibraryFilter()) }, label = { Text("Canciones") }) }
                item { FilterChip(selected = false, onClick = { library(LibraryFilter(source = "whatsapp")) }, label = { Text("WhatsApp") }) }
                item { FilterChip(selected = false, onClick = { library(LibraryFilter(source = "instrumental")) }, label = { Text("Instrumentales") }) }
            }
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF51406E), Color(0xFF292537), Color(0xFF1D2427))))) {
                Canvas(Modifier.fillMaxSize()) {
                    for (i in 0..12) drawCircle(Color(0xFFD5BBFF).copy(alpha = .18f), size.height * (.22f + i * .055f),
                        Offset(size.width * .96f, size.height * .55f), style = Stroke(1.5.dp.toPx()))
                    drawCircle(Brush.radialGradient(listOf(Color(0xFFC7ADFA).copy(alpha = .5f), Color.Transparent), Offset(size.width * .88f, size.height * .4f), size.height * .65f), size.height * .65f, Offset(size.width * .88f, size.height * .4f))
                }
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(15.dp), Lilac); Spacer(Modifier.width(7.dp)); Text(if (current != null) "CONTINUAR ESCUCHANDO" else "TU COLECCIÓN. SIN LÍMITES.", color = Lilac, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    }
                    Column(Modifier.fillMaxWidth(.8f)) {
                        Text(current?.displayName ?: "Tu música.\nEn tu universo.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-.8).sp, maxLines = 2)
                        Spacer(Modifier.height(8.dp)); Text(current?.artist ?: "Redescubre los sonidos que son tuyos.", color = Color(0xFFD0C9DE), fontSize = 12.sp, maxLines = 1)
                    }
                    Button(onClick = { if (current != null) vm.play(current!!, tracks) else if (count == 0) permission() else library(LibraryFilter()) }, colors = ButtonDefaults.buttonColors(containerColor = Lilac, contentColor = Color(0xFF251A37))) {
                        Icon(if (current != null) Icons.Rounded.PlayArrow else Icons.Rounded.LibraryMusic, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(if (current != null) "Volver a escuchar" else if (count == 0) "Encontrar mi música" else "Explorar biblioteca", fontSize = 12.sp)
                    }
                }
            }
            Row(Modifier.padding(top = 16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.OfflineBolt, null, Modifier.size(14.dp), MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(6.dp)); Text("$count canciones · 100% en tu dispositivo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                ActionIcon(Icons.Rounded.Refresh, "Actualizar biblioteca", vm::scan)
            }
            if (running != null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Escaneando biblioteca… ${running.progress.getInt("count", 0)} archivos", fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
        if (count == 0) item {
            EmptyState("Todos tus sonidos, un solo lugar", "Música, grabaciones y notas de voz. Autoriza el acceso o selecciona una carpeta; tus archivos nunca se suben a un servidor.", action = "Seleccionar carpeta", onAction = folder)
        }
        if (recent.isNotEmpty()) item { SectionTitle("Vuelve a tus favoritos", "Recientes") { library(LibraryFilter(sort = "recent")) }; CoverCarousel(recent, settings.largeCovers, { vm.play(it, recent) }, menu) }
        if (favorites.isNotEmpty()) item { SectionTitle("Hechas para quedarse", "Favoritos") { library(LibraryFilter(favorite = true)) }; CoverCarousel(favorites, settings.largeCovers, { vm.play(it, favorites) }, menu) }
        if (tracks.isNotEmpty()) {
            item { SectionTitle("Tus canciones", "Ver todas") { library(LibraryFilter()) } }
            items(tracks.take(5), key = { "song-${it.id}" }) { track -> TrackRow(track, current?.id == track.id, { vm.play(track, tracks) }, { menu(track) }) }
        }
        item {
            SectionTitle("Tu selección personal", "Playlists", playlists)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = playlists).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.QueueMusic, null, Modifier.size(32.dp), MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f).padding(horizontal = 16.dp)) { Text(if (lists.isEmpty()) "Una playlist para cada momento" else "${lists.size} playlists a tu manera", fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Spacer(Modifier.height(4.dp)); Text("Dale tu propio ritmo al día.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Icon(Icons.Rounded.ArrowForward, null, Modifier.size(20.dp))
            }
        }
        if (whatsapp.isNotEmpty()) item { SectionTitle("Voces que importan", "WhatsApp") { library(LibraryFilter(source = "whatsapp")) }; CoverCarousel(whatsapp, settings.largeCovers, { vm.play(it, whatsapp) }, menu) }
        if (instrumentals.isNotEmpty()) item { SectionTitle("Solo la música", "Instrumentales") { library(LibraryFilter(source = "instrumental")) }; CoverCarousel(instrumentals, settings.largeCovers, { vm.play(it, instrumentals) }, menu) }
        if (added.isNotEmpty()) item { SectionTitle("Recién llegadas", "Ver todas") { library(LibraryFilter(sort = "added")) }; CoverCarousel(added, settings.largeCovers, { vm.play(it, added) }, menu) }
        item { Text("SIN ALGORITMOS. SIN INTERRUPCIONES. SOLO TÚ.", fontSize = 8.sp, letterSpacing = 1.3.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 36.dp, bottom = 12.dp)) }
    }
}

@Composable
private fun CoverCarousel(tracks: List<Track>, large: Boolean, play: (Track) -> Unit, menu: (Track) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(tracks, key = { it.id }) { track ->
            Column(Modifier.width(if (large) 152.dp else 120.dp)) {
                Cover(track.artworkKey, track.displayName, track.cover, Modifier.fillMaxWidth().aspectRatio(1f).clickable { play(track) }, track.source == "whatsapp")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(track.displayName, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    ActionIcon(Icons.Rounded.MoreVert, "Opciones de ${track.displayName}", { menu(track) }, modifier = Modifier.size(32.dp))
                }
                Text(track.artist, maxLines = 1, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
