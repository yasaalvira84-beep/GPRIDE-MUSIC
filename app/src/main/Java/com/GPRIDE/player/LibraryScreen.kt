package com.gpride.player

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

private fun hasAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, audioPermission()) == PackageManager.PERMISSION_GRANTED

@Composable
fun LibraryScreen(vm: LibraryViewModel, player: PlayerViewModel, playlists: PlaylistViewModel) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasAudioPermission(context)) }
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        asked = true
    }

    // Izin bisa diubah dari Pengaturan sistem; periksa ulang saat layar kembali aktif.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = hasAudioPermission(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(granted) { if (granted) vm.refresh() }

    if (granted) {
        LibraryContent(vm, player, playlists)
    } else {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "GPRIDE MUSIC membutuhkan izin membaca file audio agar dapat menampilkan musik di perangkat Anda. " +
                    "File tidak diunggah ke mana pun.",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (asked) {
                Text(
                    "Izin ditolak. Aktifkan izin Musik dan audio di pengaturan aplikasi.",
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.error,
                )
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    },
                    modifier = Modifier.padding(top = 12.dp),
                ) { Text("Buka pengaturan aplikasi") }
            }
            Button(
                onClick = { launcher.launch(audioPermission()) },
                modifier = Modifier.padding(top = 12.dp),
            ) { Text("Beri izin") }
        }
    }
}

@Composable
private fun LibraryContent(vm: LibraryViewModel, player: PlayerViewModel, playlists: PlaylistViewModel) {
    val context = LocalContext.current
    val ui by vm.ui.collectAsState()
    val tabs = listOf("Lagu", "Album", "Artis", "Folder", "Terbaru")
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var openKind by rememberSaveable { mutableStateOf("") }
    var openKey by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    var playlistTarget by remember { mutableStateOf<List<Song>?>(null) }

    val group: SongGroup? = when (openKind) {
        "album" -> ui.albums
        "artist" -> ui.artists
        "folder" -> ui.folders
        else -> emptyList()
    }.firstOrNull { it.key == openKey }

    val shown: List<Song> = when {
        group != null -> group.songs
        tab == 0 -> ui.songs
        tab == 4 -> ui.recent
        else -> emptyList()
    }
    val selecting = selected.isNotEmpty()

    BackHandler(enabled = group != null || selecting) {
        if (selecting) selected = emptySet() else { openKind = ""; openKey = "" }
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = ui.query,
            onValueChange = vm::setQuery,
            label = { Text("Cari judul, artis, atau album") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        )

        if (selecting) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${selected.size} dipilih", Modifier.weight(1f))
                TextButton(onClick = {
                    player.enqueue(shown.filter { it.id in selected })
                    selected = emptySet()
                }) { Text("Antrean") }
                TextButton(onClick = { playlistTarget = shown.filter { it.id in selected } }) { Text("Playlist") }
                TextButton(onClick = { selected = emptySet() }) { Text("Batal") }
            }
        } else if (group != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { openKind = ""; openKey = "" }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
                Text(group.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else if (tab == 0) {
            SortRow(ui.sort, vm::setSort)
        }

        if (group == null) {
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
                tabs.forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                }
            }
        }

        if (ui.loading && ui.totalSongs == 0) {
            Text("Memindai library…", Modifier.padding(16.dp))
        } else if (ui.totalSongs == 0) {
            Text("Tidak ada lagu ditemukan di perangkat.", Modifier.padding(16.dp))
        } else if (shown.isEmpty() && (group != null || tab == 0 || tab == 4)) {
            Text("Tidak ada hasil.", Modifier.padding(16.dp))
        }

        if (group != null || tab == 0 || tab == 4) {
            SongList(
                songs = shown,
                favorites = ui.favoriteIds,
                selected = selected,
                onPlay = { index -> player.playSongs(shown, index) },
                onToggleSelect = { song ->
                    selected = if (song.id in selected) selected - song.id else selected + song.id
                },
                onFavorite = vm::toggleFavorite,
                onPlayNext = { player.playNext(listOf(it)) },
                onEnqueue = { player.enqueue(listOf(it)) },
                onAddToPlaylist = { playlistTarget = listOf(it) },
            )
        } else {
            val groups = when (tab) {
                1 -> ui.albums
                2 -> ui.artists
                else -> ui.folders
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(groups, key = { it.key }) { g ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            openKind = when (tab) { 1 -> "album"; 2 -> "artist"; else -> "folder" }
                            openKey = g.key
                        }.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(g.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(g.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (tab == 3) {
                            TextButton(onClick = { vm.excludeFolder(g.key) }) { Text("Sembunyikan") }
                        }
                    }
                }
                if (tab == 3 && ui.excludedFolders.isNotEmpty()) {
                    item {
                        HorizontalDivider()
                        Text("Folder disembunyikan", Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall)
                    }
                    items(ui.excludedFolders, key = { "x$it" }) { path ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(path, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { vm.includeFolder(path) }) { Text("Tampilkan") }
                        }
                    }
                }
            }
        }
    }

    playlistTarget?.let { songs ->
        AddToPlaylistDialog(
            playlists = playlists,
            songs = songs,
            onDone = { name ->
                Toast.makeText(context, "Ditambahkan ke $name", Toast.LENGTH_SHORT).show()
                playlistTarget = null
                selected = emptySet()
            },
            onDismiss = { playlistTarget = null },
        )
    }
}

@Composable
private fun SortRow(sort: SortOrder, onChange: (SortOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box {
            TextButton(onClick = { open = true }) { Text("Urutkan: ${sort.field.label}") }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                SortField.entries.forEach { field ->
                    DropdownMenuItem(
                        text = { Text(field.label) },
                        onClick = {
                            open = false
                            onChange(sort.copy(field = field))
                        },
                    )
                }
            }
        }
        IconButton(onClick = { onChange(sort.copy(ascending = !sort.ascending)) }) {
            Icon(
                if (sort.ascending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = if (sort.ascending) "Urutan menaik" else "Urutan menurun",
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongList(
    songs: List<Song>,
    favorites: Set<Long>,
    selected: Set<Long>,
    onPlay: (Int) -> Unit,
    onToggleSelect: (Song) -> Unit,
    onFavorite: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onEnqueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
) {
    val selecting = selected.isNotEmpty()
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
            var menu by remember { mutableStateOf(false) }
            Row(
                Modifier.fillMaxWidth()
                    .combinedClickable(
                        onClick = { if (selecting) onToggleSelect(song) else onPlay(index) },
                        onLongClick = { onToggleSelect(song) },
                    )
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.subtitle(), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (selecting) {
                    Checkbox(checked = song.id in selected, onCheckedChange = { onToggleSelect(song) })
                } else {
                    val fav = song.id in favorites
                    IconButton(onClick = { onFavorite(song) }) {
                        Icon(
                            if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (fav) "Hapus dari favorit" else "Tambah ke favorit",
                            tint = if (fav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Opsi lagu")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Putar berikutnya") }, onClick = { menu = false; onPlayNext(song) })
                            DropdownMenuItem(text = { Text("Tambah ke antrean") }, onClick = { menu = false; onEnqueue(song) })
                            DropdownMenuItem(text = { Text("Tambah ke playlist") }, onClick = { menu = false; onAddToPlaylist(song) })
                        }
                    }
                }
            }
        }
    }
}
