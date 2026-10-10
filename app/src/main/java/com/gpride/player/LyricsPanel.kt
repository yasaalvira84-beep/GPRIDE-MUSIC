package com.gpride.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val LEAD_MS = 400L
private const val WINDOW_BEFORE = 2
private const val WINDOW_AFTER = 3

/** Panel lirik otomatis: mencari lirik saat lagu berganti, lalu menyorot baris yang sedang dinyanyikan. */
@Composable
fun LyricsPanel(
    songKey: String,
    title: String,
    artist: String?,
    album: String?,
    durationMs: Long,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (title.isBlank()) return
    val app = LocalContext.current.applicationContext as GprideApplication
    val online by app.settings.lyricsOnline.collectAsState(initial = true)
    var state by remember(songKey) { mutableStateOf<LyricsState>(LyricsState.Loading) }
    var retry by remember(songKey) { mutableIntStateOf(0) }
    val durationKnown = durationMs > 0

    LaunchedEffect(songKey, durationKnown, online, retry) {
        state = LyricsState.Loading
        // durasi dipakai untuk mencocokkan versi lagu; tunggu sebentar bila belum diketahui
        if (!durationKnown) delay(1500)
        state = app.lyrics.load(songKey, title, artist, album, durationMs, online, force = retry > 0)
    }

    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Lirik",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = { retry++ }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Refresh, contentDescription = "Cari ulang lirik", modifier = Modifier.size(20.dp))
            }
        }
        when (val s = state) {
            LyricsState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Mencari lirik…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LyricsState.Instrumental -> Hint("Lagu ini instrumental.")
            LyricsState.NotFound -> {
                Hint("Lirik tidak ditemukan untuk lagu ini.")
                TextButton(onClick = { retry++ }) { Text("Coba lagi") }
            }
            LyricsState.Disabled -> Hint("Pencarian lirik otomatis dimatikan. Aktifkan di Pengaturan > Lirik.")
            is LyricsState.Error -> {
                Text(s.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { retry++ }) { Text("Coba lagi") }
            }
            is LyricsState.Found -> if (s.lyrics.synced) {
                SyncedLines(s.lyrics.lines, positionMs, onSeek)
            } else {
                Column(Modifier.fillMaxWidth().height(260.dp).verticalScroll(rememberScrollState())) {
                    s.lyrics.lines.forEach { line ->
                        Text(
                            line.text.ifBlank { " " },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
        if (state is LyricsState.Found) {
            Text(
                "Sumber: LRCLIB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SyncedLines(lines: List<LyricLine>, positionMs: Long, onSeek: (Long) -> Unit) {
    val active = activeLineIndex(lines, positionMs + LEAD_MS)
    val center = active.coerceAtLeast(0)
    val from = (center - WINDOW_BEFORE).coerceAtLeast(0)
    val to = (center + WINDOW_AFTER).coerceAtMost(lines.lastIndex)
    Column(
        Modifier.fillMaxWidth().height(220.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        for (i in from..to) {
            val line = lines[i]
            val isActive = i == active
            Text(
                line.text.ifBlank { "♪" },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeek(line.timeMs) }
                    .padding(vertical = 5.dp),
                textAlign = TextAlign.Center,
                style = if (isActive) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                },
                maxLines = 2,
            )
        }
    }
}
