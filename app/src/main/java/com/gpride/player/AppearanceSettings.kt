package com.gpride.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Pilihan warna aksen dan mode AMOLED; perubahan langsung terlihat di seluruh aplikasi. */
@Composable
fun AppearanceSettings() {
    val app = LocalContext.current.applicationContext as GprideApplication
    val scope = rememberCoroutineScope()
    val prefs by app.settings.theme.collectAsState(initial = ThemePrefs())
    val selected = prefs.accent.coerceIn(0, AccentOptions.lastIndex)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Warna aksen", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccentOptions.forEachIndexed { index, option ->
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(option.color)
                        .then(
                            if (index == selected) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            } else {
                                Modifier
                            },
                        )
                        .clickable(onClickLabel = option.name) {
                            scope.launch { app.settings.setTheme(prefs.copy(accent = index)) }
                        },
                )
            }
        }
        Text(
            AccentOptions[selected].name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Mode AMOLED")
                Text(
                    "Latar hitam pekat agar layar AMOLED lebih hemat daya dan lebih kontras.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = prefs.amoled,
                onCheckedChange = { scope.launch { app.settings.setTheme(prefs.copy(amoled = it)) } },
            )
        }
    }
}
