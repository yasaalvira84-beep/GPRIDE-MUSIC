package com.gpride.player

import android.os.SystemClock
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

private val SleepOptionsMinutes = listOf(15, 30, 45, 60, 90)

/** Tombol timer tidur (ikon bulan) untuk bilah atas layar Sedang Diputar. */
@Composable
fun SleepTimerButton() {
    val state by PlaybackEffects.sleep.collectAsState()
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(
            Icons.Filled.Bedtime,
            contentDescription = "Timer tidur",
            tint = if (state.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (open) SleepTimerDialog(state = state, onDismiss = { open = false })
}

@Composable
private fun SleepTimerDialog(state: SleepTimerState, onDismiss: () -> Unit) {
    var remainingMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state.endAtMs) {
        while (true) {
            remainingMs = ((state.endAtMs ?: 0L) - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            delay(1000)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Timer tidur") },
        text = {
            Column {
                val status = when {
                    state.endOfSong -> "Musik berhenti setelah lagu ini selesai."
                    state.endAtMs != null -> "Musik berhenti dalam ${formatTime(remainingMs)}."
                    else -> "Musik berhenti otomatis setelah waktu yang dipilih (volume dipudarkan perlahan)."
                }
                Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SleepOptionsMinutes.forEach { minutes ->
                    TextButton(onClick = {
                        PlaybackEffects.sleepInMinutes(minutes)
                        onDismiss()
                    }) { Text("$minutes menit") }
                }
                TextButton(onClick = {
                    PlaybackEffects.sleepAtEndOfSong()
                    onDismiss()
                }) { Text("Setelah lagu ini selesai") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
        dismissButton = if (state.active) {
            {
                TextButton(onClick = {
                    PlaybackEffects.cancelSleep()
                    onDismiss()
                }) { Text("Matikan timer") }
            }
        } else {
            null
        },
    )
}
