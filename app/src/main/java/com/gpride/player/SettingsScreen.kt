package com.gpride.player

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Menu Lainnya: daftar kategori; Pemutaran, Library, dan Tentang tampil sebagai halaman dalam layar ini. */
@Composable
fun SettingsScreen(library: LibraryViewModel, onOpenVisualizer: () -> Unit) {
    var page by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = page != null) { page = null }
    when (page) {
        "playback" -> PlaybackSettings(onBack = { page = null })
        "library" -> LibrarySettings(library, onBack = { page = null })
        "about" -> AboutSettings(onBack = { page = null })
        else -> SettingsMenu(onOpen = { page = it }, onOpenVisualizer = onOpenVisualizer)
    }
}

@Composable
private fun SettingsMenu(onOpen: (String) -> Unit, onOpenVisualizer: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Pengaturan", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 6.dp))
        MenuRow(Icons.Filled.GraphicEq, "Visualizer", "Gaya, warna, sensitivitas, FPS", onOpenVisualizer)
        MenuRow(Icons.Filled.PlayCircle, "Pemutaran", "Riwayat pemutaran") { onOpen("playback") }
        MenuRow(Icons.Filled.LibraryMusic, "Library", "Pindai ulang lagu di perangkat") { onOpen("library") }
        MenuRow(Icons.Filled.Info, "Tentang", "Versi aplikasi") { onOpen("about") }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SubPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        content()
    }
}

@Composable
private fun PlaybackSettings(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as GprideApplication
    val scope = rememberCoroutineScope()
    val historyEnabled by app.settings.historyEnabled.collectAsState(initial = true)
    var confirmClear by remember { mutableStateOf(false) }

    SubPage("Pemutaran", onBack) {
        SettingsCard("Riwayat") {
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
private fun LibrarySettings(library: LibraryViewModel, onBack: () -> Unit) {
    SubPage("Library", onBack) {
        SettingsCard("Pindai library") {
            Text(
                "Cari ulang file audio di perangkat setelah menambah atau menghapus lagu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = library::refresh) { Text("Pindai ulang library") }
        }
    }
}

@Composable
private fun AboutSettings(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember { appVersion(context) }
    SubPage("Tentang", onBack) {
        SettingsCard("GPRIDE MUSIC") {
            Text("Versi $version")
            Text(
                "Pemutar musik lokal tanpa akun dan tanpa iklan. " +
                    "Playlist, favorit, dan riwayat hanya disimpan di perangkat ini.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Suppress("DEPRECATION")
private fun appVersion(context: Context): String = runCatching {
    val pm = context.packageManager
    val info = if (Build.VERSION.SDK_INT >= 33) {
        pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        pm.getPackageInfo(context.packageName, 0)
    }
    info.versionName
}.getOrNull() ?: "-"

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
