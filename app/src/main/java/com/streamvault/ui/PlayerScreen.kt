@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.streamvault.ui

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streamvault.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MiniPlayer(track: Track, state: PlaybackState, toggle: () -> Unit, next: () -> Unit, expand: () -> Unit) {
    Column(Modifier.testTag("mini-player").padding(horizontal = 10.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = expand)) {
        Row(Modifier.padding(start = 9.dp, top = 8.dp, bottom = 7.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(track.artworkKey, track.displayName, track.cover, Modifier.size(43.dp), track.source == "whatsapp")
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(track.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ActionIcon(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (state.playing) "Pausar" else "Reproducir", toggle)
            ActionIcon(Icons.Rounded.SkipNext, "Siguiente", next)
        }
        LinearProgressIndicator(progress = { if (state.duration > 0) state.position.toFloat() / state.duration else 0f }, modifier = Modifier.fillMaxWidth().height(2.dp), trackColor = Color.Transparent)
    }
}

@Composable
fun FullPlayer(vm: LibraryViewModel, track: Track, state: PlaybackState, dismiss: () -> Unit, menu: (Track) -> Unit, library: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val mixing by vm.mixing.collectAsStateWithLifecycle()
    val analyzing by vm.analyzing.collectAsStateWithLifecycle()
    var transition by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var volume by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    val maxVolume = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    var queue by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf<Float?>(null) }
    var background by remember(track.id) { mutableStateOf(Color(0xFF332845)) }
    LaunchedEffect(track.cover) {
        background = withContext(Dispatchers.IO) {
            runCatching {
                val uri = track.cover ?: return@withContext Color(0xFF332845)
                val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 32 }
                val bitmap = context.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) } ?: return@withContext Color(0xFF332845)
                var red = 0L; var green = 0L; var blue = 0L; var count = 0
                for (x in 0 until bitmap.width step 2) for (y in 0 until bitmap.height step 2) {
                    val c = bitmap.getPixel(x, y); red += android.graphics.Color.red(c); green += android.graphics.Color.green(c); blue += android.graphics.Color.blue(c); count++
                }
                bitmap.recycle()
                if (count == 0) Color(0xFF332845) else Color((red / count).toInt(), (green / count).toInt(), (blue / count).toInt())
            }.getOrDefault(Color(0xFF332845))
        }
    }
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(background.copy(alpha = .48f), MaterialTheme.colorScheme.background))).safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ActionIcon(Icons.Rounded.KeyboardArrowDown, "Cerrar reproductor", dismiss)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AHORA SUENA", letterSpacing = 2.sp, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (analyzing == track.id) "Analizando inicio…" else if (mixing) "Mezclando · Crossfade" else "Tu biblioteca", fontSize = 12.sp)
                    }
                    ActionIcon(Icons.Rounded.MoreHoriz, "Opciones de la canción", { menu(track) })
                }
                BoxWithConstraints(Modifier.weight(1f)) {
                    val landscape = maxWidth > maxHeight * 1.25f
                    val artwork: @Composable () -> Unit = {
                        Cover(track.artworkKey, track.displayName, track.cover, Modifier.padding(24.dp).fillMaxWidth().aspectRatio(1f).pointerInput(Unit) {
                            var distance = 0f
                            detectVerticalDragGestures(onDragStart = { distance = 0f }, onDragEnd = { if (distance < -70) queue = true }) { change, amount -> change.consume(); distance += amount }
                        }, track.source == "whatsapp")
                    }
                    val controls: @Composable () -> Unit = {
                        Column(Modifier.padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(track.displayName, fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(track.artist, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                                }
                                ActionIcon(if (track.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorito", { vm.favorite(track) }, track.favorite)
                            }
                            Spacer(Modifier.height(20.dp))
                            Slider(value = dragging ?: state.position.toFloat().coerceIn(0f, state.duration.toFloat().coerceAtLeast(1f)), onValueChange = { dragging = it }, onValueChangeFinished = { dragging?.let { vm.seek(it.toLong()) }; dragging = null }, valueRange = 0f..state.duration.toFloat().coerceAtLeast(1f))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(time(dragging?.toLong() ?: state.position), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(time(state.duration), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                ActionIcon(Icons.Rounded.Shuffle, "Aleatorio", { vm.app.preferences.update { it.copy(shuffle = !it.shuffle) } }, settings.shuffle)
                                ActionIcon(Icons.Rounded.SkipPrevious, "Anterior", vm::previous, modifier = Modifier.size(48.dp))
                                FilledIconButton(onClick = vm::toggle, modifier = Modifier.size(72.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                                    if (state.buffering || analyzing == track.id) CircularProgressIndicator(Modifier.size(26.dp), color = MaterialTheme.colorScheme.onPrimary)
                                    else Icon(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (state.playing) "Pausar" else "Reproducir", Modifier.size(38.dp))
                                }
                                ActionIcon(Icons.Rounded.SkipNext, "Siguiente", vm::next, modifier = Modifier.size(48.dp))
                                ActionIcon(if (settings.repeat == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repetición: ${settings.repeat}", { vm.app.preferences.update { it.copy(repeat = (it.repeat + 1) % 3) } }, settings.repeat != 0)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                TextButton(onClick = { vm.skip(-1) }) { Icon(Icons.Rounded.Replay, null, Modifier.size(20.dp)); Text(" −${settings.skipSeconds} s") }
                                TextButton(onClick = { vm.skip(1) }) { Text("+${settings.skipSeconds} s "); Icon(Icons.Rounded.FastForward, null, Modifier.size(20.dp)) }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.VolumeDown, null, Modifier.size(18.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(volume, { volume = it; audio.setStreamVolume(AudioManager.STREAM_MUSIC, it.toInt(), 0) }, valueRange = 0f..maxVolume, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                                Icon(Icons.Rounded.VolumeUp, null, Modifier.size(18.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (track.offset(settings.detectSilence) > 0) Text("Inicio inteligente · ${"%.1f".format(track.offset(settings.detectSilence) / 1000.0)} s", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                        }
                    }
                    if (landscape) Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { artwork() }; Box(Modifier.weight(1f).verticalScroll(rememberScrollState())) { controls() }
                    } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.widthIn(max = 440.dp)) { artwork() }; controls()
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)) {
                    TextButton(onClick = { transition = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Tune, null, Modifier.size(18.dp))
                        Text("  Inicio y crossfade · ${track.crossfadeSeconds ?: settings.crossfade} s", maxLines = 1)
                    }
                    FilledTonalButton(onClick = { queue = true }, modifier = Modifier.fillMaxWidth().testTag("open-queue")) {
                        Icon(Icons.Rounded.QueueMusic, null)
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text("A continuación", maxLines = 1)
                            Text("${state.queue.size} pistas en la cola", fontSize = 11.sp)
                        }
                        Icon(Icons.Rounded.KeyboardArrowUp, null)
                    }
                }
            }
            if (queue) QueuePanel(vm, state, { queue = false })
            }
        }
        if (transition) OffsetDialog(vm, track, false, { transition = false })
    }
}

@Composable
private fun QueuePanel(vm: LibraryViewModel, state: PlaybackState, dismiss: () -> Unit) {
    var add by remember { mutableStateOf(false) }
    var lists by remember { mutableStateOf(false) }
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val scroll = rememberLazyListState(initialFirstVisibleItemIndex = state.index.coerceAtLeast(0))
    BackHandler { dismiss() }
    Surface(Modifier.fillMaxSize().testTag("queue-panel"), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("A continuación", fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2, modifier = Modifier.weight(1f))
                ActionIcon(Icons.Rounded.Close, "Volver al reproductor", dismiss)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = { add = true }) { Icon(Icons.Rounded.Add, null); Text("Canciones") }
                TextButton(onClick = { lists = true }) { Icon(Icons.Rounded.PlaylistAdd, null); Text("Playlists") }
            }
            Text(if (state.queue.size < 2) "Agrega otra pista para usar Anterior y Siguiente." else "${state.queue.size} pistas · toca una canción para escucharla.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(bottom = 14.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = scroll, contentPadding = PaddingValues(bottom = 24.dp)) {
                if (state.queue.isEmpty()) item { EmptyState("La cola está vacía", "Agrega canciones o una playlist para empezar.") }
                itemsIndexed(state.queue, key = { index, item -> "$index-${item.id}" }) { index, item ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (index == state.index) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).padding(horizontal = 8.dp)) {
                        if (index == state.index) Text("AHORA SUENA", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp))
                        Row(Modifier.fillMaxWidth().clickable { vm.queueSelect(index) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Cover("${item.title}|${item.artist}|${item.album}", item.title, item.cover, Modifier.size(46.dp))
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp); Text(item.artist, maxLines = 1, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Text(time(item.duration), fontSize = 11.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { vm.queueMove(index, index - 1) }, enabled = index > 0) { Icon(Icons.Rounded.ArrowUpward, "Subir canción") }
                            IconButton(onClick = { vm.queueMove(index, index + 1) }, enabled = index < state.queue.lastIndex) { Icon(Icons.Rounded.ArrowDownward, "Bajar canción") }
                            ActionIcon(Icons.Rounded.Close, "Quitar canción de la cola", { vm.queueRemove(index) })
                        }
                    }
                }
            }
        }
    }
    if (add) TrackPicker(vm, "Agregar a la cola", { add = false }) { vm.enqueue(it) }
    if (lists) AlertDialog(onDismissRequest = { lists = false }, title = { Text("Agregar playlist") }, text = { LazyColumn {
        if (playlists.isEmpty()) item { Text("Crea una playlist en la sección Playlists.") }
        itemsIndexed(playlists) { _, playlist -> TextButton(onClick = { vm.enqueuePlaylist(playlist); lists = false }) { Text(playlist.name) } }
    } }, confirmButton = { TextButton(onClick = { lists = false }) { Text("Cerrar") } })
}
