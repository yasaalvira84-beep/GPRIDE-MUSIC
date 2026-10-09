package com.gpride.player

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryLogicTest {
    private fun song(
        id: Long,
        title: String,
        artist: String = "Artis A",
        album: String = "Album 1",
        albumId: Long = 1,
        durationMs: Long = 180_000,
        dateAdded: Long = id,
        folder: String = "/storage/emulated/0/Music",
    ) = Song(id, title, artist, album, albumId, durationMs, dateAdded, folder)

    private val songs = listOf(
        song(1, "beta", artist = "Zed", durationMs = 200_000),
        song(2, "Alpha", artist = "Yan", album = "Album 2", albumId = 2, durationMs = 100_000),
        song(3, "Charlie", artist = "Zed", folder = "/storage/emulated/0/Download"),
    )

    @Test fun searchMatchesTitleArtistAlbumIgnoringCase() {
        assertEquals(listOf(2L), songs.search("alpha").map { it.id })
        assertEquals(listOf(1L, 3L), songs.search("ZED").map { it.id })
        assertEquals(listOf(2L), songs.search("album 2").map { it.id })
        assertEquals(songs, songs.search("   "))
    }

    @Test fun sortByTitleIgnoresCase() {
        assertEquals(listOf("Alpha", "beta", "Charlie"), songs.sortedByOrder(SortOrder(SortField.Title)).map { it.title })
        assertEquals(listOf("Charlie", "beta", "Alpha"), songs.sortedByOrder(SortOrder(SortField.Title, false)).map { it.title })
    }

    @Test fun sortByDurationAndDate() {
        assertEquals(listOf(2L, 3L, 1L), songs.sortedByOrder(SortOrder(SortField.Duration)).map { it.id })
        assertEquals(listOf(3L, 2L, 1L), songs.sortedByOrder(SortOrder(SortField.DateAdded, false)).map { it.id })
    }

    @Test fun groupsByAlbumArtistAndFolder() {
        assertEquals(listOf("Album 1", "Album 2"), songs.groupedByAlbum().map { it.title })
        val artists = songs.groupedByArtist()
        assertEquals(listOf("Yan", "Zed"), artists.map { it.title })
        assertEquals(2, artists.last().songs.size)
        val folders = songs.groupedByFolder()
        assertEquals(listOf("Download", "Music"), folders.map { it.title })
    }

    @Test fun movedByStaysInBounds() {
        val list = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), list.movedBy(0, 1))
        assertEquals(listOf("a", "c", "b"), list.movedBy(2, -1))
        assertEquals(list, list.movedBy(0, -1))
        assertEquals(list, list.movedBy(2, 1))
        assertEquals(list, list.movedBy(5, 1))
    }
}
