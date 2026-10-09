package com.gpride.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player

/** Layar uji mesin playback Tahap 2. Akan digantikan UI Now Playing penuh di Tahap 4. */
@Composable
fun PlayerScreen(vm: PlayerViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val message by vm.message.collectAsState()
    val context = LocalContext.current

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        vm.addUris(uris)
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = state.title.ifEmpty { stringResource(R.string.player_nothing_playing) },
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        state.artist?.let { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1) }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

        var dragging by remember { mutableStateOf<Float?>(null) }
        val duration = state.durationMs.coerceAtLeast(1L).toFloat()
        Slider(
            value = (dragging ?: state.positionMs.toFloat()).coerceIn(0f, duration),
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { vm.seekTo(it.toLong()) }
                dragging = null
            },
            valueRange = 0f..duration,
            enabled = state.queue.isNotEmpty(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime((dragging ?: state.positionMs.toFloat()).toLong()))
            Text(formatTime(state.durationMs))
        }

        Row(
            Modifier.fillMaxWidth(),
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
                Icon(Icons.Filled.SkipPrevious, stringResource(R.string.cd_previous))
            }
            FilledIconButton(onClick = vm::togglePlay) {
                Icon(
                    if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(if (state.isPlaying) R.string.cd_pause else R.string.cd_play),
                )
            }
            IconButton(onClick = vm::next) {
                Icon(Icons.Filled.SkipNext, stringResource(R.string.cd_next))
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

        Button(onClick = { picker.launch(arrayOf("audio/*")) }, enabled = state.connected) {
            Text(stringResource(R.string.player_add_songs))
        }

        Text(stringResource(R.string.player_queue), style = MaterialTheme.typography.titleMedium)
        if (state.queue.isEmpty()) {
            Text(stringResource(R.string.player_queue_empty), style = MaterialTheme.typography.bodyMedium)
        }
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(state.queue) { index, entry ->
                val isCurrent = index == state.currentIndex
                Row(
                    Modifier.fillMaxWidth().clickable { vm.playAt(index) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text(
                            entry.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        entry.artist?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
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
}
