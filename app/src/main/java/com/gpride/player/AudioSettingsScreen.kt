package com.gpride.player

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Pengaturan audio: equalizer + preset + bass boost, pudar volume antarlagu, dan lewati hening. */
@Composable
fun AudioSettingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as GprideApplication
    val scope = rememberCoroutineScope()
    val prefs by app.settings.audio.collectAsState(initial = AudioPrefs())
    val info by PlaybackEffects.eqInfo.collectAsState()

    fun save(updated: AudioPrefs) {
        scope.launch { app.settings.setAudio(updated) }
    }

    var fade by remember(prefs.fadeSec) { mutableFloatStateOf(prefs.fadeSec.toFloat()) }
    var bass by remember(prefs.bassBoost) { mutableFloatStateOf(prefs.bassBoost.toFloat()) }
    val levels = remember(prefs.eqPreset, prefs.eqCustomMb, info) {
        mutableStateListOf<Float>().apply {
            info?.let { i -> eqLevels(prefs, i).forEach { add(it.toFloat()) } }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Text("Pengaturan Audio", style = MaterialTheme.typography.titleLarge)
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Equalizer", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = prefs.eqEnabled, onCheckedChange = { save(prefs.copy(eqEnabled = it)) })
        }

        val eq = info
        if (eq == null) {
            Text(
                "Equalizer belum tersedia. Putar lagu terlebih dahulu; sebagian perangkat memang tidak mendukungnya.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EqPresets.forEachIndexed { index, preset ->
                    FilterChip(
                        selected = prefs.eqPreset == index,
                        onClick = { save(prefs.copy(eqPreset = index)) },
                        label = { Text(preset.label) },
                    )
                }
                FilterChip(
                    selected = prefs.eqPreset == -1,
                    onClick = { save(prefs.copy(eqPreset = -1, eqCustomMb = levels.map { it.roundToInt() })) },
                    label = { Text("Kustom") },
                )
            }
            for (i in 0 until eq.bandCount) {
                if (i >= levels.size) break
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatHz(eq.centerHz.getOrElse(i) { 0 }), Modifier.width(64.dp), style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = levels[i].coerceIn(eq.minMb.toFloat(), eq.maxMb.toFloat()),
                        onValueChange = { levels[i] = it },
                        onValueChangeFinished = {
                            save(prefs.copy(eqPreset = -1, eqCustomMb = levels.map { it.roundToInt() }))
                        },
                        valueRange = eq.minMb.toFloat()..eq.maxMb.toFloat(),
                        enabled = prefs.eqEnabled,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${(levels[i] / 100f).roundToInt()} dB", Modifier.width(52.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Bass boost", Modifier.weight(1f))
            Text("${(bass / 10f).roundToInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = bass,
            onValueChange = { bass = it },
            onValueChangeFinished = { save(prefs.copy(bassBoost = bass.roundToInt())) },
            valueRange = 0f..1000f,
            enabled = prefs.eqEnabled,
        )
        Text(
            "Bass boost ikut aktif bersama equalizer; tidak semua perangkat mendukungnya.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("Pemutaran", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Pudar volume antarlagu", Modifier.weight(1f))
            Text(if (fade.roundToInt() == 0) "Mati" else "${fade.roundToInt()} dtk", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = fade,
            onValueChange = { fade = it },
            onValueChangeFinished = { save(prefs.copy(fadeSec = fade.roundToInt())) },
            valueRange = 0f..8f,
            steps = 7,
        )
        Text(
            "Volume dipelankan di akhir lagu dan dinaikkan di awal lagu berikutnya. Ini pudar bergantian, bukan crossfade yang saling tumpang-tindih. " +
                "Pemutaran tanpa jeda (gapless) sudah otomatis jika file-nya mendukung.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Lewati bagian hening")
                Text(
                    "Melewati senyap panjang di dalam lagu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = prefs.skipSilence, onCheckedChange = { save(prefs.copy(skipSilence = it)) })
        }
    }
}
