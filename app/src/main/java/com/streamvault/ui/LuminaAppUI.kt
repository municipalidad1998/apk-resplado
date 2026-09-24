@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.streamvault.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streamvault.data.Track

@Composable
fun LuminaAppUI(vm: LibraryViewModel, requestPermission: () -> Unit, chooseFolder: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()
    val current by vm.current.collectAsStateWithLifecycle()
    val analyzing by vm.analyzing.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var fullPlayer by rememberSaveable { mutableStateOf(false) }
    var menuTrack by remember { mutableStateOf<Track?>(null) }
    LaunchedEffect(message, fullPlayer, menuTrack) { if (!fullPlayer && menuTrack == null) message?.let { snackbar.showSnackbar(it); vm.message.value = null } }
    fun library(filter: LibraryFilter = LibraryFilter()) { vm.filter.value = filter; vm.query.value = ""; page = 1 }
    val labels = listOf("Inicio", "Biblioteca", "Playlists", "Buscar", "Ajustes")
    val icons = listOf(Icons.Rounded.Home, Icons.Rounded.LibraryMusic, Icons.Rounded.QueueMusic, Icons.Rounded.Search, Icons.Rounded.Tune)
    LuminaTheme(settings.theme) {
        Surface(Modifier.fillMaxSize()) {
            BoxWithConstraints {
                val wide = maxWidth >= 720.dp
                Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
                    if (wide) NavigationRail(containerColor = MaterialTheme.colorScheme.background) {
                        Spacer(Modifier.height(28.dp)); Icon(Icons.Rounded.GraphicEq, "Lúmina", tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(36.dp))
                        labels.forEachIndexed { i, label -> NavigationRailItem(selected = page == i, onClick = { page = i; if (i == 3) vm.filter.value = LibraryFilter() }, icon = { Icon(icons[i], label) }, label = { Text(label) }) }
                    }
                    Scaffold(modifier = Modifier.weight(1f), snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
                        Column {
                            if (current != null) MiniPlayer(current!!, playback, vm::toggle, vm::next, analyzing == current?.id) { fullPlayer = true }
                            if (!wide) NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                                labels.forEachIndexed { i, label -> NavigationBarItem(selected = page == i,
                                    onClick = { page = i; if (i == 3) vm.filter.value = LibraryFilter() },
                                    icon = { Icon(icons[i], label, Modifier.size(23.dp)) }, label = { Text(label, fontSize = 10.sp) }) }
                            } else Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                        }
                    }) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)) {
                            Crossfade(targetState = page, animationSpec = tween(if (settings.animations) 220 else 0), label = "Pantalla") { destination ->
                            when (destination) {
                                0 -> HomeScreen(vm, requestPermission, chooseFolder, ::library, { page = 2 }, { menuTrack = it })
                                1 -> LibraryScreen(vm, false, requestPermission, chooseFolder, { menuTrack = it })
                                2 -> PlaylistsScreen(vm, { menuTrack = it }, { library() })
                                3 -> LibraryScreen(vm, true, requestPermission, chooseFolder, { menuTrack = it })
                                else -> SettingsScreen(vm, requestPermission, chooseFolder)
                            }
                            }
                        }
                    }
                }
                if (fullPlayer && current != null) FullPlayer(vm, current!!, playback, { fullPlayer = false }, { menuTrack = it }, { library(); fullPlayer = false })
                menuTrack?.let { track -> TrackActions(vm, track, { menuTrack = null }) }
                if (message != null && (fullPlayer || menuTrack != null)) AlertDialog(
                    onDismissRequest = { vm.message.value = null }, title = { Text("Lúmina") },
                    text = { Text(message.orEmpty()) }, confirmButton = { TextButton(onClick = { vm.message.value = null }) { Text("Aceptar") } }
                )
            }
        }
    }
    BackHandler(fullPlayer) { fullPlayer = false }
}
