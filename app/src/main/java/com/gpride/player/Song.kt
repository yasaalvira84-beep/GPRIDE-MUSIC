package com.gpride.player

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import java.io.File

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val dateAddedSec: Long,
    val folderPath: String,
) {
    /** URI konten MediaStore; dihitung saat dibutuhkan agar Song mudah diuji di JVM. */
    val uri: Uri get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
}

fun Song.subtitle(): String = "$artist • $album • ${formatTime(durationMs)}"

enum class SortField(val label: String) {
    Title("Judul"),
    Artist("Artis"),
    Album("Album"),
    Duration("Durasi"),
    DateAdded("Tanggal ditambahkan"),
}

data class SortOrder(val field: SortField = SortField.Title, val ascending: Boolean = true)

/** Cari di judul, artis, dan album (tanpa membedakan huruf besar/kecil). */
fun List<Song>.search(query: String): List<Song> {
    val q = query.trim()
    if (q.isEmpty()) return this
    return filter {
        it.title.contains(q, ignoreCase = true) ||
            it.artist.contains(q, ignoreCase = true) ||
            it.album.contains(q, ignoreCase = true)
    }
}

fun List<Song>.sortedByOrder(order: SortOrder): List<Song> {
    val ci = String.CASE_INSENSITIVE_ORDER
    val cmp: Comparator<Song> = when (order.field) {
        SortField.Title -> compareBy(ci, Song::title)
        SortField.Artist -> compareBy(ci, Song::artist).thenBy(ci, Song::title)
        SortField.Album -> compareBy(ci, Song::album).thenBy(ci, Song::title)
        SortField.Duration -> compareBy<Song> { it.durationMs }
        SortField.DateAdded -> compareBy<Song> { it.dateAddedSec }
    }
    return sortedWith(if (order.ascending) cmp else cmp.reversed())
}

data class SongGroup(val key: String, val title: String, val subtitle: String, val songs: List<Song>)

private fun List<SongGroup>.byTitle(): List<SongGroup> =
    sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, SongGroup::title))

fun List<Song>.groupedByAlbum(): List<SongGroup> =
    groupBy { it.albumId to it.album }.map { (k, s) ->
        SongGroup("${k.first}:${k.second}", k.second, "${s.first().artist} • ${s.size} lagu", s)
    }.byTitle()

fun List<Song>.groupedByArtist(): List<SongGroup> =
    groupBy { it.artist }.map { (artist, s) ->
        val albums = s.map { it.album }.distinct().size
        SongGroup(artist, artist, "$albums album • ${s.size} lagu", s)
    }.byTitle()

fun List<Song>.groupedByFolder(): List<SongGroup> =
    groupBy { it.folderPath }.map { (path, s) ->
        SongGroup(path, File(path).name.ifEmpty { path }, "$path • ${s.size} lagu", s)
    }.byTitle()

/** Pindahkan elemen di [index] sebanyak [delta] posisi; tidak berubah jika di luar batas. */
fun <T> List<T>.movedBy(index: Int, delta: Int): List<T> {
    val target = index + delta
    if (index !in indices || target !in indices) return this
    return toMutableList().also { list ->
        val item = list.removeAt(index)
        list.add(target, item)
    }
}
