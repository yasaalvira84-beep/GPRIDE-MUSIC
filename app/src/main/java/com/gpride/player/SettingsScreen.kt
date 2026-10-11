package com.gpride.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Pengaturan sementara; Tampilan, Audio, dan lainnya menyusul di tahap berikutnya. */
@Composable
fun SettingsScreen(library: LibraryViewModel, onOpenVisualizer: () -> Unit, onOpenAudio: () -> Unit) {
    val app = LocalContext.current.applicationContext as GprideApplication
    val scope = rememberCoroutineScope()
    val historyEnabled by app.settings.historyEnabled.collectAsState(initial = true)
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Pengaturan", style = MaterialTheme.typography.titleLarge)

        SettingsCard("Tampilan") {
            AppearanceSettings()
        }

        SettingsCard("Visualizer") {
            Text(
                "Gaya, sensitivitas, warna, dan FPS efek visual yang mengikuti musik.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onOpenVisualizer) { Text("Buka pengaturan visualizer") }
        }

        SettingsCard("Audio") {
            Text(
                "Equalizer, bass boost, pudar volume antarlagu, dan lewati bagian hening.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onOpenAudio) { Text("Buka pengaturan audio") }
        }

        SettingsCard("Pemutaran") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Simpan riwayat pemutaran")
                    Text(
                        "Dipakai untuk daftar Riwayat dan Sering diputar. Disimpan hanya di perangkat ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = historyEnabled,
                    onCheckedChange = { scope.launch { app.settings.setHistoryEnabled(it) } },
                )
            }
            OutlinedButton(onClick = { confirmClear = true }) { Text("Hapus riwayat") }
        }

        SettingsCard("Lirik otomatis") {
            Text(
                "Lirik dibuat dari suara lagu dengan Whisper (tombol Lirik di Beranda). Butuh kunci API milik Anda; " +
                    "audio lagu diunggah ke penyedia yang dipilih hanya saat Anda menekan Buat lirik otomatis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LyricsApiSetup()
            OutlinedButton(onClick = { app.lyrics.clearAll() }) { Text("Hapus semua lirik tersimpan") }
        }

        SettingsCard("Library") {
            Button(onClick = library::refresh) { Text("Pindai ulang library") }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Hapus riwayat?") },
            text = { Text("Daftar Riwayat dan Sering diputar akan dikosongkan.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { app.dao.clearHistory() }
                    confirmClear = false
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Batal") } },
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}
