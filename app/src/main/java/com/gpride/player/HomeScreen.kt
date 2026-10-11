package com.gpride.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private fun homeAudioPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

private fun homeHasAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, homeAudioPermission()) == PackageManager.PERMISSION_GRANTED

private fun homeHasRecordPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/** Beranda: pencarian, visualizer, Lanjut Diputar, Koleksi, dan Playlist. */
@Composable
fun HomeScreen(
    player: PlayerViewModel,
    library: LibraryViewModel,
    playlists: PlaylistViewModel,
    currentSong: Song?,
    songsById: Map<Long, Song>,
    onNavigate: (Destination) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenLyrics: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as GprideApplication
    val state by player.state.collectAsState()
    val lib by library.ui.collectAsState()
    val summaries by playlists.summaries.collectAsState()
    val recentFlow = remember { app.dao.recentIds() }
    val recentIds by recentFlow.collectAsState(initial = emptyList())

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var granted by remember { mutableStateOf(homeHasAudioPermission(context)) }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(granted) { if (granted && songsById.isEmpty()) library.refresh() }

    val prefs by app.settings.visualizer.collectAsState(initial = VisualizerPrefs())
    var micGranted by remember { mutableStateOf(homeHasRecordPermission(context)) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { micGranted = it }
    val vizActive = prefs.enabled && micGranted && state.isPlaying
    val feed = rememberVisualizerFeed(vizActive, prefs.sensitivity / 100f, prefs.fps)
    val vizColor = VisualizerPalettes[prefs.colorIndex.coerceIn(0, VisualizerPalettes.lastIndex)]
    val vizAccent = VisualizerAccents[prefs.colorIndex.coerceIn(0, VisualizerAccents.lastIndex)]

    val visible = remember(songsById, lib.excludedFolders) {
        songsById.values.filter { it.folderPath !in lib.excludedFolders }
    }
    val albumCount = remember(visible) { visible.map { it.albumId to it.album }.distinct().size }
    val artistCount = remember(visible) { visible.map { it.artist }.distinct().size }
    val folderCount = remember(visible) { visible.map { it.folderPath }.distinct().size }

    val lastPlayed = recentIds.firstOrNull()?.let { songsById[it] }
    val hasCurrent = state.title.isNotEmpty()
    val shownSong = if (hasCurrent) currentSong else lastPlayed
    val shownTitle = if (hasCurrent) state.title else lastPlayed?.title
    val shownArtist = if (hasCurrent) state.artist else lastPlayed?.artist

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "GPRIDE",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                IconButton(onClick = library::refresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Pindai ulang library")
                }
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { onNavigate(Destination.Library) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text("Cari lagu, artis, album...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                VisualizerCanvas(prefs.style, feed, vizColor, vizAccent, Modifier.fillMaxSize(), frame = true)
                if (prefs.enabled && prefs.style == VisualizerStyle.Circular) {
                    AlbumArt(shownSong, Modifier.size(92.dp), shape = CircleShape)
                }
                when {
                    !prefs.enabled -> Text(
                        "Visualizer dimatikan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    !micGranted -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Butuh izin Rekam audio untuk membaca keluaran musik (mikrofon tidak direkam).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Izinkan visualizer") }
                    }
                    !state.isPlaying -> Text(
                        "Putar lagu untuk melihat visualizer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    feed.failed.value -> Text(
                        "Visualizer tidak tersedia di perangkat ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        if (!granted) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("GPRIDE MUSIC membutuhkan izin membaca file audio agar dapat menampilkan musik di perangkat Anda.")
                    Button(onClick = { audioLauncher.launch(homeAudioPermission()) }) { Text("Izinkan akses audio") }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Lanjut Diputar")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable {
                            when {
                                hasCurrent -> onOpenNowPlaying()
                                lastPlayed != null -> {
                                    player.playSongs(listOf(lastPlayed))
                                    onOpenNowPlaying()
                                }
                                else -> onNavigate(Destination.Library)
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArt(shownSong, Modifier.size(56.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            shownTitle ?: "Belum ada lagu",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            shownArtist ?: "Pilih lagu di Eksplorasi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (hasCurrent) {
                        TextButton(
                            onClick = onOpenLyrics,
                            contentPadding = PaddingValues(horizontal = 10.dp),
                        ) { Text("Lirik") }
                    }
                    if (shownTitle != null) {
                        FilledIconButton(onClick = {
                            if (hasCurrent) player.togglePlay() else lastPlayed?.let { player.playSongs(listOf(it)) }
                        }) {
                            Icon(
                                if (hasCurrent && state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(if (hasCurrent && state.isPlaying) R.string.cd_pause else R.string.cd_play),
                            )
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Koleksi")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CollectionTile(Icons.Filled.MusicNote, "Lagu", "${visible.size} lagu", Modifier.weight(1f)) {
                        onNavigate(Destination.Library)
                    }
                    CollectionTile(Icons.Filled.Album, "Album", "$albumCount album", Modifier.weight(1f)) {
                        onNavigate(Destination.Library)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CollectionTile(Icons.Filled.Person, "Artis", "$artistCount artis", Modifier.weight(1f)) {
                        onNavigate(Destination.Library)
                    }
                    CollectionTile(Icons.Filled.Folder, "Folder", "$folderCount folder", Modifier.weight(1f)) {
                        onNavigate(Destination.Library)
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Playlist", Modifier.weight(1f))
                    TextButton(onClick = { onNavigate(Destination.Playlist) }) { Text("Lihat semua") }
                }
                PlaylistTile(Icons.Filled.Favorite, "Favorit", "${lib.favoriteIds.size} lagu") {
                    playlists.open(ListKey.Favorites)
                    onNavigate(Destination.Playlist)
                }
                summaries.take(3).forEach { summary ->
                    PlaylistTile(Icons.Filled.MusicNote, summary.name, "${summary.count} lagu") {
                        playlists.open(ListKey.UserPlaylist(summary.id))
                        onNavigate(Destination.Playlist)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CollectionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlaylistTile(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
