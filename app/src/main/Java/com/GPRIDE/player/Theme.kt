package com.gpride.player

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFF1DB9A6)

private val DarkColors = darkColorScheme(
    primary = Accent,
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
)

private val LightColors = lightColorScheme(primary = Color(0xFF00796B))

/** Tema gelap sebagai tampilan awal; opsi AMOLED/terang dan aksen kustom menyusul di Tahap 4. */
@Composable
fun GprideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
