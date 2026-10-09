package com.gpride.player

import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/** Sumber tunggal daftar lagu dari MediaStore. Dipindai ulang hanya saat diminta. */
class LibraryRepository(private val context: Context) {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    suspend fun refresh() {
        _loading.value = true
        try {
            _songs.value = withContext(Dispatchers.IO) { querySongs() }
        } finally {
            _loading.value = false
        }
    }

    private fun querySongs(): List<Song> {
        val media = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
        )
        val result = ArrayList<Song>()
        try {
            context.contentResolver.query(
                media,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                null,
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                while (c.moveToNext()) {
                    val data = c.getString(dataCol).orEmpty()
                    result += Song(
                        id = c.getLong(idCol),
                        title = clean(c.getString(titleCol), File(data).nameWithoutExtension.ifEmpty { "Tanpa judul" }),
                        artist = clean(c.getString(artistCol), "Artis tidak diketahui"),
                        album = clean(c.getString(albumCol), "Album tidak diketahui"),
                        albumId = c.getLong(albumIdCol),
                        durationMs = c.getLong(durationCol),
                        dateAddedSec = c.getLong(dateCol),
                        folderPath = File(data).parent.orEmpty(),
                    )
                }
            }
        } catch (_: SecurityException) {
            // Izin dicabut saat pemindaian: tampilkan library kosong, bukan crash.
        }
        return result
    }

    private fun clean(value: String?, fallback: String): String =
        value?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: fallback
}
