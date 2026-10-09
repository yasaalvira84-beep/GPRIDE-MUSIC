package com.gpride.player

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// Room hanya menyimpan data milik pengguna (ID lagu MediaStore + koleksi). Metadata lagu tidak diduplikasi.

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val songId: Long, val addedAt: Long)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(
    tableName = "playlist_songs",
    indices = [Index("playlistId")],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PlaylistSongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val songId: Long,
    val position: Int,
)

@Entity(tableName = "history", indices = [Index("songId")])
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val playedAt: Long,
)

@Entity(tableName = "excluded_folders")
data class ExcludedFolderEntity(@PrimaryKey val path: String)

data class PlaylistCount(val playlistId: Long, val count: Int)

@Dao
abstract class GprideDao {
    // Favorit
    @Query("SELECT songId FROM favorites ORDER BY addedAt DESC")
    abstract fun favoriteIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    abstract suspend fun removeFavorite(songId: Long)

    // Playlist
    @Query("SELECT * FROM playlists ORDER BY createdAt")
    abstract fun playlists(): Flow<List<PlaylistEntity>>

    @Query("SELECT playlistId, COUNT(*) AS count FROM playlist_songs GROUP BY playlistId")
    abstract fun playlistCounts(): Flow<List<PlaylistCount>>

    @Insert
    abstract suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    abstract suspend fun renamePlaylist(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    abstract suspend fun deletePlaylist(id: Long)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position, id")
    abstract fun playlistEntries(playlistId: Long): Flow<List<PlaylistSongEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    abstract suspend fun maxPosition(playlistId: Long): Int

    @Insert
    abstract suspend fun insertPlaylistSongs(entries: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE id = :entryId")
    abstract suspend fun removeEntry(entryId: Long)

    @Query("UPDATE playlist_songs SET position = :position WHERE id = :entryId")
    abstract suspend fun updatePosition(entryId: Long, position: Int)

    @Transaction
    open suspend fun addSongs(playlistId: Long, songIds: List<Long>) {
        var pos = maxPosition(playlistId) + 1
        insertPlaylistSongs(songIds.map { PlaylistSongEntity(playlistId = playlistId, songId = it, position = pos++) })
    }

    @Transaction
    open suspend fun setOrder(entryIds: List<Long>) {
        entryIds.forEachIndexed { index, id -> updatePosition(id, index) }
    }

    // Riwayat
    @Insert
    abstract suspend fun addHistory(entry: HistoryEntity)

    @Query("SELECT songId FROM history GROUP BY songId ORDER BY MAX(playedAt) DESC LIMIT 100")
    abstract fun recentIds(): Flow<List<Long>>

    @Query("SELECT songId FROM history GROUP BY songId ORDER BY COUNT(*) DESC, MAX(playedAt) DESC LIMIT 50")
    abstract fun mostPlayedIds(): Flow<List<Long>>

    @Query("DELETE FROM history")
    abstract suspend fun clearHistory()

    // Folder yang dikecualikan
    @Query("SELECT path FROM excluded_folders")
    abstract fun excludedFolders(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun excludeFolder(folder: ExcludedFolderEntity)

    @Query("DELETE FROM excluded_folders WHERE path = :path")
    abstract suspend fun includeFolder(path: String)
}

@Database(
    entities = [
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        HistoryEntity::class,
        ExcludedFolderEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class GprideDatabase : RoomDatabase() {
    abstract fun dao(): GprideDao
}
