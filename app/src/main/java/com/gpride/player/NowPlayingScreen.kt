package com.gpride.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.Player

/** Layar Now Playing penuh: sampul album bercincin neon, visualizer, kontrol, dan antrean. */
@Composable
fun NowPlayingScreen(vm: PlayerViewModel, song: Song?, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val message by vm.message.collectAsState()
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(1L).toFloat()

    val color = MaterialTheme.colorScheme.primary

    LazyColumn(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Tutup")
                }
                Text(
                    "Sedang Diputar",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                Box(Modifier.size(48.dp))
            }
        }
        item {
            ArtWithRing(song = song, color = color, modifier = Modifier.padding(vertical = 16.dp))
        }
        item {
            Text(
                text = state.title.ifEmpty { stringResource(R.string.player_nothing_playing) },
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            state.artist?.let {
                Text(
                    it,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            message?.let {
                Text(
                    it,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            Slider(
                value = (dragging ?: state.positionMs.toFloat()).coerceIn(0f, duration),
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { vm.seekTo(it.toLong()) }
                    dragging = null
                },
                valueRange = 0f..duration,
                enabled = state.queue.isNotEmpty(),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime((dragging ?: state.positionMs.toFloat()).toLong()), style = MaterialTheme.typography.bodySmall)
                Text(formatTime(state.durationMs), style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = vm::toggleShuffle) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = stringResource(R.string.cd_shuffle),
                        tint = if (state.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = vm::previous) {
                    Icon(Icons.Filled.SkipPrevious, stringResource(R.string.cd_previous), modifier = Modifier.size(32.dp))
                }
                FilledIconButton(onClick = vm::togglePlay, modifier = Modifier.size(72.dp)) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(if (state.isPlaying) R.string.cd_pause else R.string.cd_play),
                        modifier = Modifier.size(38.dp),
                    )
                }
                IconButton(onClick = vm::next) {
                    Icon(Icons.Filled.SkipNext, stringResource(R.string.cd_next), modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = vm::toggleRepeat) {
                    Icon(
                        if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = stringResource(R.string.cd_repeat),
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
        item {
            LyricsPanel(
                songKey = state.songId?.toString() ?: "t${"${state.artist}|${state.title}".hashCode()}",
                title = state.title,
                artist = state.artist,
                album = song?.album,
                durationMs = state.durationMs,
                positionMs = state.positionMs,
                onSeek = vm::seekTo,
            )
        }
        item {
            Text(
                "${stringResource(R.string.player_queue)} (${state.queue.size})",
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (state.queue.isEmpty()) {
                Text(
                    stringResource(R.string.player_queue_empty),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(state.queue) { index, entry ->
            val isCurrent = index == state.currentIndex
            Row(
                Modifier.fillMaxWidth().clickable { vm.playAt(index) }.padding(start = 24.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text(
                        entry.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    entry.artist?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                IconButton(onClick = { vm.move(index, -1) }) {
                    Icon(Icons.Filled.ArrowUpward, stringResource(R.string.cd_move_up))
                }
                IconButton(onClick = { vm.move(index, 1) }) {
                    Icon(Icons.Filled.ArrowDownward, stringResource(R.string.cd_move_down))
                }
                IconButton(onClick = { vm.removeAt(index) }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.cd_remove))
                }
            }
        }
    }
}

/** Sampul bulat dengan cincin neon statis. */
@Composable
private fun ArtWithRing(song: Song?, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(290.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(290.dp)) {
            val radius = size.minDimension / 2f
            drawCircle(
                color = color.copy(alpha = 0.12f),
                radius = radius * 0.86f,
                style = Stroke(width = 20.dp.toPx()),
            )
            drawCircle(
                color = color.copy(alpha = 0.8f),
                radius = radius * 0.74f,
                style = Stroke(width = 3.dp.toPx()),
            )
        }
        AlbumArt(song, Modifier.size(200.dp), shape = CircleShape)
    }
}
