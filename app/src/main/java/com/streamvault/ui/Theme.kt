package com.streamvault.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Lilac = Color(0xFFC7B5FF)
val Mint = Color(0xFFB9E5CA)
private val Dark = darkColorScheme(
    primary = Lilac, onPrimary = Color(0xFF2F1C55), primaryContainer = Color(0xFF302641), onPrimaryContainer = Color(0xFFE8DFFF),
    secondary = Mint, background = Color(0xFF101014), surface = Color(0xFF101014), surfaceVariant = Color(0xFF222127),
    onBackground = Color(0xFFF6F3FC), onSurface = Color(0xFFF6F3FC), onSurfaceVariant = Color(0xFFAAA5B5), outline = Color(0xFF49444F)
)
private val Light = lightColorScheme(
    primary = Color(0xFF6845A8), onPrimary = Color.White, primaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF356949), background = Color(0xFFF9F7FC), surface = Color(0xFFF9F7FC), surfaceVariant = Color(0xFFEEE9F4),
    onBackground = Color(0xFF211D2B), onSurface = Color(0xFF211D2B), onSurfaceVariant = Color(0xFF6C6378), outline = Color(0xFFABA2B9)
)
@Composable
fun LuminaTheme(mode: String, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (mode == "dark" || mode == "system" && isSystemInDarkTheme()) Dark else Light, content = content)
}
