package com.gpride.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun PlaylistScreen(vm: PlaylistViewModel, player: PlayerViewModel) {
    val opened by vm.opened.collectAsState()
    BackHandler(enabled = opened != null) { vm.close() }
    val current = opened
    if (current == null) {
        PlaylistOverview(vm)
    } else {
        PlaylistDetail(current, vm, player)
    }
}

@Composable
private fun PlaylistOverview(vm: PlaylistViewModel) {
    val lists by vm.summaries.collectAsState()
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<PlaylistSummary?>(null) }
    var deleting by remember { mutableStateOf<PlaylistSummary?>(null) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            item { OverviewRow("Favorit", null, onClick = { vm.open(ListKey.Favorites) }) }
            item { OverviewRow("Sering diputar", null, onClick = { vm.open(ListKey.MostPlayed) }) }
            item { OverviewRow("Riwayat", null, onClick = { vm.open(ListKey.History) }) }
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            if (lists.isEmpty()) {
                item { Text("Belum ada playlist.", Modifier.padding(16.dp)) }
            }
            items(lists, key = { it.id }) { pl ->
                OverviewRow(
                    title = pl.name,
                    subtitle = "${pl.count} lagu",
                    onClick = { vm.open(ListKey.UserPlaylist(pl.id)) },
                ) {
                    IconButton(onClick = { renaming = pl }) { Icon(Icons.Filled.Edit, "Ubah nama playlist") }
                    IconButton(onClick = { deleting = pl }) { Icon(Icons.Filled.Delete, "Hapus playlist") }
                }
            }
        }
        Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Playlist baru")
        }
    }

    if (creating) {
        TextInputDialog("Playlist baru", "", "Buat", { vm.createPlaylist(it); creating = false }, { creating = false })
    }
    renaming?.let { pl ->
        TextInputDialog("Ubah nama playlist", pl.name, "Simpan", { vm.rename(pl.id, it); renaming = null }, { renaming = null })
    }
    deleting?.let { pl ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Hapus playlist?") },
            text = { Text("Playlist \"${pl.name}\" akan dihapus. Lagu di perangkat tidak terhapus.") },
            confirmButton = { TextButton(onClick = { vm.delete(pl.id); deleting = null }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Batal") } },
        )
    }
}

@Composable
private fun OverviewRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        trailing()
    }
}

@Composable
private fun PlaylistDetail(list: OpenedList, vm: PlaylistViewModel, player: PlayerViewModel) {
    val songs = list.items.map { it.song }
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::close) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") }
            Text(list.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = { player.playSongs(songs, 0) }, enabled = songs.isNotEmpty()) { Text("Putar semua") }
        }
        if (songs.isEmpty()) {
            Text("Belum ada lagu.", Modifier.padding(16.dp))
        }
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(list.items, key = { _, item -> item.entryId ?: -(item.song.id + 1) }) { index, item ->
                Row(
                    Modifier.fillMaxWidth().clickable { player.playSongs(songs, index) }.padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text(item.song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.song.subtitle(), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    val entryId = item.entryId
                    if (list.editable && entryId != null) {
                        IconButton(onClick = { vm.move(index, -1) }) { Icon(Icons.Filled.ArrowUpward, "Naikkan") }
                        IconButton(onClick = { vm.move(index, 1) }) { Icon(Icons.Filled.ArrowDownward, "Turunkan") }
                        IconButton(onClick = { vm.removeEntry(entryId) }) { Icon(Icons.Filled.Close, "Hapus dari playlist") }
                    }
                }
            }
        }
    }
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Nama playlist") }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

/** Pilih playlist yang ada, atau buat baru dan langsung tambahkan lagunya. [onDone] menerima nama playlist tujuan. */
@Composable
fun AddToPlaylistDialog(
    playlists: PlaylistViewModel,
    songs: List<Song>,
    onDone: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val lists by playlists.summaries.collectAsState()
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah ${songs.size} lagu ke playlist") },
        text = {
            Column {
                LazyColumn(Modifier.heightIn(max = 220.dp)) {
                    items(lists, key = { it.id }) { pl ->
                        TextButton(
                            onClick = {
                                playlists.addToPlaylist(pl.id, songs)
                                onDone(pl.name)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("${pl.name} (${pl.count})", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                }
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Atau buat playlist baru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    playlists.createPlaylistWith(newName, songs)
                    onDone(newName.trim())
                },
                enabled = newName.isNotBlank(),
            ) { Text("Buat & tambah") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

