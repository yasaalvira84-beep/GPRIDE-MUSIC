package com.gpride.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

const val LYRICS_LEAD_MS = 400L
private const val WINDOW_BEFORE = 2
private const val WINDOW_AFTER = 3

/** Status lirik lagu: hasil pembuatan yang sedang berjalan, atau lirik tersimpan, atau belum ada. */
@Composable
fun rememberLyricsState(songKey: String): LyricsState {
    val app = LocalContext.current.applicationContext as GprideApplication
    val map by app.lyrics.status.collectAsState()
    val entry = map[songKey]
    val cachedState = remember(songKey, entry) {
        app.lyrics.cached(songKey)?.let { LyricsState.Found(it) } ?: LyricsState.Idle
    }
    return entry ?: cachedState
}

/** Panel lirik ringkas di layar Sedang Diputar (lima baris, baris aktif disorot). */
@Composable
fun LyricsPanel(
    songKey: String,
    title: String,
    uri: String?,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (title.isBlank()) return
    val state = rememberLyricsState(songKey)
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Lirik", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        when (state) {
            is LyricsState.Found -> {
                if (state.lyrics.synced) SyncedWindow(state.lyrics.lines, positionMs, onSeek) else PlainWindow(state.lyrics.lines)
                LyricsFoundFooter(songKey, uri)
            }
            else -> LyricsGenerateBlock(songKey, uri, state)
        }
    }
}

/** Isi saat lirik belum ada: tombol buat, progres, galat, atau pengisian kunci API. */
@Composable
fun LyricsGenerateBlock(songKey: String, uri: String?, state: LyricsState) {
    val app = LocalContext.current.applicationContext as GprideApplication
    val scope = rememberCoroutineScope()
    val prefs by app.settings.lyricsPrefs.collectAsState(initial = LyricsPrefs())
    var askConsent by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val provider = LyricsProviders[prefs.provider.coerceIn(0, LyricsProviders.lastIndex)]

    fun start() {
        if (uri != null) app.lyrics.generate(songKey, uri, prefs)
    }

    when (state) {
        LyricsState.Generating -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(
                "Membuat lirik dari suara lagu… biasanya 10–60 detik.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> {
            if (state is LyricsState.Error) {
                Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            } else {
                Text(
                    "Lirik belum ada. Aplikasi akan mengubah suara vokal di lagu ini menjadi teks berwaktu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (prefs.apiKey.isBlank()) {
                Text(
                    "Isi kunci API sekali saja untuk mengaktifkan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LyricsApiSetup()
            } else {
                Button(
                    onClick = { if (prefs.consent) start() else askConsent = true },
                    enabled = uri != null,
                ) { Text(if (state is LyricsState.Error) "Coba lagi" else "Buat lirik otomatis") }
                Text(
                    "Hasil terbaik untuk vokal yang jelas; musik ramai bisa menyebabkan salah dengar.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (state !is LyricsState.Generating) {
        TextButton(onClick = { editing = true }) { Text("Tulis atau impor lirik sendiri") }
    }
    if (editing) LyricsEditorDialog(songKey) { editing = false }

    if (askConsent) {
        AlertDialog(
            onDismissRequest = { askConsent = false },
            title = { Text("Unggah audio ke ${provider.label}?") },
            text = {
                Text(
                    "Untuk membuat lirik, file audio lagu ini dikirim ke ${provider.label} agar diubah menjadi teks. " +
                        "Penanganan file tunduk pada kebijakan privasi ${provider.label}. Kamu bisa mengganti penyedia di Pengaturan.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    askConsent = false
                    scope.launch { app.settings.setLyrics(prefs.copy(consent = true)) }
                    if (uri != null) app.lyrics.generate(songKey, uri, prefs.copy(consent = true))
                }) { Text("Lanjutkan") }
            },
            dismissButton = { TextButton(onClick = { askConsent = false }) { Text("Batal") } },
        )
    }
}

/** Tombol buat ulang, edit, dan hapus untuk lirik yang sudah ada. */
@Composable
fun LyricsFoundFooter(songKey: String, uri: String?) {
    val app = LocalContext.current.applicationContext as GprideApplication
    val prefs by app.settings.lyricsPrefs.collectAsState(initial = LyricsPrefs())
    var editing by remember { mutableStateOf(false) }
    Text(
        "Hasil otomatis bisa salah dengar. Ketuk Edit untuk memperbaiki.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row {
        TextButton(onClick = { editing = true }) { Text("Edit") }
        TextButton(
            onClick = { if (uri != null) app.lyrics.generate(songKey, uri, prefs) },
            enabled = uri != null && prefs.apiKey.isNotBlank(),
        ) { Text("Buat ulang") }
        TextButton(onClick = { app.lyrics.clear(songKey) }) { Text("Hapus") }
    }
    if (editing) LyricsEditorDialog(songKey) { editing = false }
}

/** Penyedia, kunci API, dan bahasa lagu; dipakai di layar lirik dan di Pengaturan. */
@Composable
fun LyricsApiSetup() {
    val app = LocalContext.current.applicationContext as GprideApplication
    val scope = rememberCoroutineScope()
    val prefs by app.settings.lyricsPrefs.collectAsState(initial = LyricsPrefs())
    var provider by remember(prefs.provider) { mutableIntStateOf(prefs.provider) }
    var key by remember(prefs.apiKey) { mutableStateOf(prefs.apiKey) }
    var lang by remember(prefs.language) { mutableStateOf(prefs.language) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Penyedia transkripsi", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LyricsProviders.forEachIndexed { index, p ->
                FilterChip(selected = provider == index, onClick = { provider = index }, label = { Text(p.label) })
            }
        }
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("Kunci API ${LyricsProviders[provider].label}") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Bahasa lagu", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LyricsLanguages.forEach { (code, label) ->
                FilterChip(selected = lang == code, onClick = { lang = code }, label = { Text(label) })
            }
        }
        Text(
            "Kunci dibuat di akun penyedia (Groq atau OpenAI) dan disimpan hanya di HP ini. " +
                "Cek biaya dan batas pemakaian di situs penyedia. Memilih bahasa yang tepat membuat hasil lebih akurat.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = {
            scope.launch {
                app.settings.setLyrics(
                    prefs.copy(
                        provider = provider,
                        apiKey = key.trim(),
                        language = lang,
                        // persetujuan unggah berlaku per penyedia
                        consent = if (provider == prefs.provider) prefs.consent else false,
                    ),
                )
            }
        }) { Text("Simpan") }
    }
}

@Composable
private fun SyncedWindow(lines: List<LyricLine>, positionMs: Long, onSeek: (Long) -> Unit) {
    val active = activeLineIndex(lines, positionMs + LYRICS_LEAD_MS)
    val center = active.coerceAtLeast(0)
    val from = (center - WINDOW_BEFORE).coerceAtLeast(0)
    val to = (center + WINDOW_AFTER).coerceAtMost(lines.lastIndex)
    Column(Modifier.fillMaxWidth().height(220.dp), verticalArrangement = Arrangement.Center) {
        for (i in from..to) {
            LyricLineText(lines[i], i == active, onSeek)
        }
    }
}

@Composable
private fun PlainWindow(lines: List<LyricLine>) {
    Column(Modifier.fillMaxWidth().height(220.dp).verticalScroll(rememberScrollState())) {
        lines.forEach { LyricLineText(it, isActive = false, onSeek = {}, plain = true) }
    }
}

@Composable
fun LyricLineText(line: LyricLine, isActive: Boolean, onSeek: (Long) -> Unit, plain: Boolean = false) {
    Text(
        line.text.ifBlank { "♪" },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSeek(line.timeMs) }
            .padding(vertical = 5.dp),
        textAlign = TextAlign.Center,
        style = if (isActive) MaterialTheme.typography.titleMedium else if (plain) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
        color = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else if (plain) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        },
        maxLines = 2,
    )
}
