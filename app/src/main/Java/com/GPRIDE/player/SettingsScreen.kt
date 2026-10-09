package com.gpride.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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

/** Pengaturan sementara Tahap 3; tema, visualizer, dan lainnya menyusul di tahap berikutnya. */
@Composable
fun SettingsScreen(library: LibraryViewModel) {
    val app = LocalContext.current.applicationContext as GprideApplication
    val scope = rememberCoroutineScope()
    val historyEnabled by app.settings.historyEnabled.collectAsState(initial = true)
    var confirmClear by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Simpan riwayat pemutaran")
                Text(
                    "Dipakai untuk daftar Riwayat dan Sering diputar. Disimpan hanya di perangkat ini.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = historyEnabled,
                onCheckedChange = { scope.launch { app.settings.setHistoryEnabled(it) } },
            )
        }
        OutlinedButton(onClick = { confirmClear = true }) { Text("Hapus riwayat") }
        Button(onClick = library::refresh) { Text("Pindai ulang library") }
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
