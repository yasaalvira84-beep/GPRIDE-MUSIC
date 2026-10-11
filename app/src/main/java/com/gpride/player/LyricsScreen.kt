package com.gpride.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Layar lirik penuh (dibuka dari tombol Lirik di Beranda): daftar lirik berwaktu yang bergulir mengikuti lagu. */
@Composable
fun LyricsScreen(player: PlayerViewModel, onBack: () -> Unit) {
    val state by player.state.collectAsState()
    val hasSong = state.title.isNotBlank()
    val songKey = lyricsKey(state.songId, state.artist, state.title)
    val lyricsState = rememberLyricsState(songKey)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Column(Modifier.weight(1f)) {
                Text("Lirik", style = MaterialTheme.typography.titleLarge)
                if (hasSong) {
                    Text(
                        listOfNotNull(state.title, state.artist).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (!hasSong) {
            Text(
                "Putar lagu untuk melihat atau membuat liriknya.",
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        when (val s = lyricsState) {
            is LyricsState.Found -> {
                val lines = s.lyrics.lines
                val active = if (s.lyrics.synced) activeLineIndex(lines, state.positionMs + LYRICS_LEAD_MS) else -1
                val onLineClick: (Long) -> Unit = { t -> if (s.lyrics.synced) player.seekTo(t) }
                val listState = rememberLazyListState()
                LaunchedEffect(active) {
                    if (active >= 0) listState.animateScrollToItem((active - 2).coerceAtLeast(0))
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(lines) { index, line ->
                        LyricLineText(line, index == active, onLineClick, plain = !s.lyrics.synced)
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    LyricsFoundFooter(songKey, state.uri)
                }
            }
            else -> Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LyricsGenerateBlock(songKey, state.uri, s)
            }
        }
    }
}
