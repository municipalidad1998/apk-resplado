package com.streamvault.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamvault.data.Track
import kotlin.math.absoluteValue

fun time(ms: Long): String { val s = ms.coerceAtLeast(0) / 1000; return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60) }

@Composable
fun Cover(key: String, title: String, uri: String?, modifier: Modifier = Modifier, voice: Boolean = false) {
    val palettes = listOf(Color(0xFF876EA7) to Color(0xFF282043), Color(0xFFDB8C69) to Color(0xFF693B51),
        Color(0xFF599F99) to Color(0xFF123F4D), Color(0xFFA1AB75) to Color(0xFF37453F), Color(0xFF748CBC) to Color(0xFF292D5D))
    val colors = palettes[(key.hashCode().toLong().absoluteValue % palettes.size).toInt()]
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(colors.first, colors.second)))) {
        Canvas(Modifier.fillMaxSize()) {
            for (i in 0..6) {
                drawCircle(Color.White.copy(alpha = .08f + i * .006f), size.minDimension * (.22f + i * .10f),
                    center = Offset(size.width * .80f, size.height * .25f), style = Stroke(size.minDimension * .025f))
            }
            drawCircle(Color.Black.copy(alpha = .08f), size.width * .6f, Offset(size.width * .1f, size.height * 1.1f))
        }
        if (uri == null) {
            Icon(if (voice) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote, null, Modifier.align(Alignment.Center).fillMaxSize(.34f), Color.White.copy(alpha = .85f))
        } else AsyncImage(model = uri, contentDescription = "Portada de $title", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
fun ActionIcon(icon: ImageVector, label: String, onClick: () -> Unit, selected: Boolean = false, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) { Icon(icon, label, tint = if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current) }
}

@Composable
fun TrackRow(track: Track, playing: Boolean = false, onPlay: () -> Unit, onMenu: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onPlay).padding(vertical = 9.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(track.artworkKey, track.displayName, track.cover, Modifier.size(54.dp), track.source == "whatsapp")
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(track.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium, color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (playing) Icon(Icons.Rounded.GraphicEq, "En reproducción", Modifier.padding(horizontal = 8.dp).size(18.dp), tint = MaterialTheme.colorScheme.primary)
        else Text(time(track.durationMs), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (trailing != null) trailing() else ActionIcon(Icons.Rounded.MoreVert, "Opciones de ${track.displayName}", onMenu)
    }
}

@Composable
fun SectionTitle(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (action != null) TextButton(onClick = onAction) { Text(action, fontSize = 12.sp) }
    }
}

@Composable
fun EmptyState(title: String, description: String, icon: ImageVector = Icons.Rounded.LibraryMusic, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(76.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(32.dp), MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (action != null) { Spacer(Modifier.height(20.dp)); Button(onClick = onAction) { Text(action) } }
    }
}

@Composable
fun PageHeader(eyebrow: String, title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(eyebrow.uppercase(), fontSize = 10.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(title, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
            }
            action?.invoke()
        }
        if (subtitle != null) { Spacer(Modifier.height(8.dp)); Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
