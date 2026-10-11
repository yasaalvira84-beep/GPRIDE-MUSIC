package com.gpride.player

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private const val MAX_LYRICS_FILE_BYTES = 1_000_000

/**
 * Editor lirik layar penuh: perbaiki hasil otomatis, tempel lirik sendiri, atau impor file .lrc/.txt.
 * Format berwaktu: satu baris per lirik, diawali [menit:detik.centi]. Tanpa waktu juga boleh.
 */
@Composable
fun LyricsEditorDialog(songKey: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as GprideApplication
    var text by remember { mutableStateOf(app.lyrics.rawText(songKey)) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            when {
                bytes == null -> error = "File tidak dapat dibaca."
                bytes.size > MAX_LYRICS_FILE_BYTES -> error = "File terlalu besar untuk lirik (maks 1 MB)."
                else -> {
                    text = bytes.toString(Charsets.UTF_8)
                    error = null
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Edit lirik", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Button(onClick = {
                        if (app.lyrics.save(songKey, text)) onDismiss() else error = "Lirik masih kosong."
                    }) { Text("Simpan") }
                }
                Text(
                    "Format berwaktu: satu baris per lirik, contoh [01:23.45] teks lirik. " +
                        "Tanpa waktu juga boleh; lirik akan tampil biasa tanpa sorotan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    isError = error != null,
                    label = { Text("Lirik") },
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = { picker.launch(arrayOf("*/*")) }) { Text("Impor dari file .lrc atau .txt") }
            }
        }
    }
}
