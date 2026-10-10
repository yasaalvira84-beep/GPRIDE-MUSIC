package com.gpride.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Layar Now Playing penuh: sampul album bercincin neon, kontrol, dan antrean. */
@Composable
fun NowPlayingScreen(vm: PlayerViewModel, song: Song?, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val message by vm.message.collectAsState()
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(1L).toFloat()

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
            ArtWithRing(song = song, playing = state.isPlaying, modifier = Modifier.padding(vertical = 16.dp))
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
                Box(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    androidx.compose.foundation.layout.Column {
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

/**
 * Sampul bulat dengan cincin neon. Animasi denyut hanya tanda sedang memutar;
 * cincin belum mengikuti audio (visualizer sungguhan menyusul).
 */
@Composable
private fun ArtWithRing(song: Song?, playing: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val glow = if (playing) pulse else 0.25f
    Box(modifier.size(290.dp), contentAlignment = Alignment.Center) {
        val green = MaterialTheme.colorScheme.primary
        Canvas(Modifier.size(290.dp)) {
            val radius = size.minDimension / 2f
            drawCircle(
                color = green.copy(alpha = 0.08f + 0.14f * glow),
                radius = radius * 0.92f,
                style = Stroke(width = 22.dp.toPx()),
            )
            drawCircle(
                color = green.copy(alpha = 0.45f + 0.45f * glow),
                radius = radius * 0.80f,
                style = Stroke(width = 3.dp.toPx()),
            )
            val ticks = 72
            for (i in 0 until ticks) {
                val angle = 2.0 * PI * i / ticks
                val length = (5f + 9f * (0.5f + 0.5f * sin(i * 0.9f))) * (0.6f + 0.6f * glow)
                val inner = radius * 0.86f
                val outer = inner + length.dp.toPx()
                drawLine(
                    color = green.copy(alpha = 0.85f),
                    start = Offset(center.x + inner * cos(angle).toFloat(), center.y + inner * sin(angle).toFloat()),
                    end = Offset(center.x + outer * cos(angle).toFloat(), center.y + outer * sin(angle).toFloat()),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
        AlbumArt(song, Modifier.size(200.dp), shape = CircleShape)
    }
}
