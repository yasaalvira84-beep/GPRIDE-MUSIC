package com.gpride.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ListKey {
    data object Favorites : ListKey
    data object MostPlayed : ListKey
    data object History : ListKey
    data class UserPlaylist(val id: Long) : ListKey
}

data class PlaylistSummary(val id: Long, val name: String, val count: Int)

/** [entryId] terisi hanya untuk isi playlist pengguna (dipakai untuk hapus/susun ulang). */
data class OpenedItem(val entryId: Long?, val song: Song)

data class OpenedList(
    val key: ListKey,
    val title: String,
    val items: List<OpenedItem>,
    val editable: Boolean,
)

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {
    private val gp = application as GprideApplication
    private val dao = gp.dao

    val summaries: StateFlow<List<PlaylistSummary>> = combine(dao.playlists(), dao.playlistCounts()) { lists, counts ->
        val byId = counts.associate { it.playlistId to it.count }
        lists.map { PlaylistSummary(it.id, it.name, byId[it.id] ?: 0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val openKey = MutableStateFlow<ListKey?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val opened: StateFlow<OpenedList?> = openKey.flatMapLatest<ListKey?, OpenedList?> { key ->
        if (key == null) return@flatMapLatest flowOf<OpenedList?>(null)
        when (key) {
            ListKey.Favorites -> combine(dao.favoriteIds(), gp.library.songs) { ids, songs ->
                OpenedList(key, "Favorit", plain(ids, songs), editable = false)
            }
            ListKey.MostPlayed -> combine(dao.mostPlayedIds(), gp.library.songs) { ids, songs ->
                OpenedList(key, "Sering diputar", plain(ids, songs), editable = false)
            }
            ListKey.History -> combine(dao.recentIds(), gp.library.songs) { ids, songs ->
                OpenedList(key, "Riwayat", plain(ids, songs), editable = false)
            }
            is ListKey.UserPlaylist -> combine(
                dao.playlistEntries(key.id), gp.library.songs, dao.playlists(),
            ) { entries, songs, lists ->
                val byId = songs.associateBy { it.id }
                OpenedList(
                    key = key,
                    title = lists.firstOrNull { it.id == key.id }?.name ?: "Playlist",
                    items = entries.mapNotNull { e -> byId[e.songId]?.let { OpenedItem(e.id, it) } },
                    editable = true,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private fun plain(ids: List<Long>, songs: List<Song>): List<OpenedItem> {
        val byId = songs.associateBy { it.id }
        return ids.mapNotNull { byId[it] }.map { OpenedItem(null, it) }
    }

    fun open(key: ListKey) { openKey.value = key }
    fun close() { openKey.value = null }

    fun createPlaylist(name: String) {
        viewModelScope.launch { dao.insertPlaylist(PlaylistEntity(name = name.trim(), createdAt = System.currentTimeMillis())) }
    }

    fun createPlaylistWith(name: String, songs: List<Song>) {
        viewModelScope.launch {
            val id = dao.insertPlaylist(PlaylistEntity(name = name.trim(), createdAt = System.currentTimeMillis()))
            dao.addSongs(id, songs.map { it.id })
        }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { dao.renamePlaylist(id, name.trim()) }
    }

    fun delete(id: Long) {
        if ((openKey.value as? ListKey.UserPlaylist)?.id == id) close()
        viewModelScope.launch { dao.deletePlaylist(id) }
    }

    fun addToPlaylist(playlistId: Long, songs: List<Song>) {
        if (songs.isEmpty()) return
        viewModelScope.launch { dao.addSongs(playlistId, songs.map { it.id }) }
    }

    fun removeEntry(entryId: Long) {
        viewModelScope.launch { dao.removeEntry(entryId) }
    }

    fun move(index: Int, delta: Int) {
        val list = opened.value ?: return
        if (!list.editable) return
        val ids = list.items.mapNotNull { it.entryId }
        val reordered = ids.movedBy(index, delta)
        if (reordered != ids) viewModelScope.launch { dao.setOrder(reordered) }
    }
}
