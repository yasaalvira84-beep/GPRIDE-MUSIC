package com.gpride.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

private fun micGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/** Pengaturan visualizer dengan pratinjau langsung (pratinjau bergerak saat ada lagu diputar). */
@Composable
fun VisualizerSettingsScreen(player: PlayerViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as GprideApplication
    val scope = rememberCoroutineScope()
    val prefs by app.settings.visualizer.collectAsState(initial = VisualizerPrefs())
    val playerState by player.state.collectAsState()

    var granted by remember { mutableStateOf(micGranted(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val active = prefs.enabled && granted && playerState.isPlaying
    val feed = rememberVisualizerFeed(active, prefs.sensitivity / 100f, prefs.fps)
    val color = VisualizerPalettes[prefs.colorIndex.coerceIn(0, VisualizerPalettes.lastIndex)]
    val accent = VisualizerAccents[prefs.colorIndex.coerceIn(0, VisualizerAccents.lastIndex)]

    fun save(updated: VisualizerPrefs) {
        scope.launch { app.settings.setVisualizer(updated) }
    }

    var sensitivity by remember(prefs.sensitivity) { mutableFloatStateOf(prefs.sensitivity.toFloat()) }
    var fps by remember(prefs.fps) { mutableFloatStateOf(prefs.fps.toFloat()) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Text("Pengaturan Visualizer", style = MaterialTheme.typography.titleLarge)
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            VisualizerCanvas(prefs.style, feed, color, accent, Modifier.fillMaxSize(), frame = true)
            if (!playerState.isPlaying) {
                Text(
                    "Putar lagu untuk melihat pratinjau",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (prefs.enabled && !granted) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Visualizer butuh izin Rekam audio agar bisa membaca keluaran musik aplikasi ini. " +
                        "Mikrofon tidak direkam.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Izinkan visualizer") }
            }
        } else if (active && feed.failed.value) {
            Text(
                "Visualizer tidak tersedia di perangkat ini.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VisualizerStyle.entries.forEach { style ->
                FilterChip(
                    selected = prefs.style == style,
                    onClick = { save(prefs.copy(style = style)) },
                    label = { Text(style.label) },
                )
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Sensitivitas", Modifier.weight(1f))
            Text("${sensitivity.toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = sensitivity,
            onValueChange = { sensitivity = it },
            onValueChangeFinished = { save(prefs.copy(sensitivity = sensitivity.toInt())) },
            valueRange = 0f..100f,
        )

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("FPS", Modifier.weight(1f))
            Text("${fps.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = fps,
            onValueChange = { fps = it },
            onValueChangeFinished = { save(prefs.copy(fps = fps.toInt())) },
            valueRange = 15f..60f,
            steps = 2,
        )

        Text("Warna")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VisualizerPalettes.forEachIndexed { index, swatch ->
                val selected = index == prefs.colorIndex
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(swatch, VisualizerAccents[index])))
                        .then(
                            if (selected) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            } else {
                                Modifier
                            },
                        )
                        .clickable { save(prefs.copy(colorIndex = index)) },
                )
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Aktifkan visualizer", Modifier.weight(1f))
            Switch(checked = prefs.enabled, onCheckedChange = { save(prefs.copy(enabled = it)) })
        }
    }
}
