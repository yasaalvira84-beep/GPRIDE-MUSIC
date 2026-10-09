package com.gpride.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val loading: Boolean = false,
    val query: String = "",
    val sort: SortOrder = SortOrder(),
    val songs: List<Song> = emptyList(),
    val albums: List<SongGroup> = emptyList(),
    val artists: List<SongGroup> = emptyList(),
    val folders: List<SongGroup> = emptyList(),
    val recent: List<Song> = emptyList(),
    val excludedFolders: List<String> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val totalSongs: Int = 0,
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val gp = application as GprideApplication
    private val dao = gp.dao

    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(SortOrder())

    private val core: StateFlow<LibraryUiState> = combine(
        gp.library.songs, query, sort, dao.excludedFolders(), dao.favoriteIds(),
    ) { songs, q, order, excluded, favorites ->
        val hidden = excluded.toSet()
        val base = songs.filter { it.folderPath !in hidden }
        val sorted = base.search(q).sortedByOrder(order)
        LibraryUiState(
            query = q,
            sort = order,
            songs = sorted,
            albums = sorted.groupedByAlbum(),
            artists = sorted.groupedByArtist(),
            folders = sorted.groupedByFolder(),
            recent = sorted.sortedByOrder(SortOrder(SortField.DateAdded, ascending = false)).take(50),
            excludedFolders = excluded.sorted(),
            favoriteIds = favorites.toSet(),
            totalSongs = base.size,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    val ui: StateFlow<LibraryUiState> = combine(core, gp.library.loading) { state, loading ->
        state.copy(loading = loading)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    fun refresh() {
        viewModelScope.launch { gp.library.refresh() }
    }

    fun setQuery(value: String) { query.value = value }
    fun setSort(order: SortOrder) { sort.value = order }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            if (song.id in ui.value.favoriteIds) {
                dao.removeFavorite(song.id)
            } else {
                dao.addFavorite(FavoriteEntity(song.id, System.currentTimeMillis()))
            }
        }
    }

    fun excludeFolder(path: String) {
        viewModelScope.launch { dao.excludeFolder(ExcludedFolderEntity(path)) }
    }

    fun includeFolder(path: String) {
        viewModelScope.launch { dao.includeFolder(path) }
    }
}
