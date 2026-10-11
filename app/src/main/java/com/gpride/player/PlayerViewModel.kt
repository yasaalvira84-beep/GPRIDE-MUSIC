package com.gpride.player

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class QueueEntry(val title: String, val artist: String?)

data class PlayerUiState(
    val connected: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffle: Boolean = false,
    val queue: List<QueueEntry> = emptyList(),
    val currentIndex: Int = -1,
    /** ID MediaStore lagu yang sedang diputar; null untuk berkas dari pemilih dokumen. */
    val songId: Long? = null,
    /** URI berkas lagu yang sedang diputar (dipakai membuat lirik otomatis). */
    val uri: String? = null,
)

private fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .build(),
    )
    .build()

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val gp = application as GprideApplication

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    /** Pesan galat singkat (mis. file rusak); hilang saat pemutaran berhasil berjalan. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var lastRecordedId: String? = null
    private var errorStreak = 0

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh(player)

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                errorStreak = 0
                _message.value = null
                record(controller?.currentMediaItem?.mediaId)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) lastRecordedId = null
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) lastRecordedId = null
            if (mediaItem != null && controller?.playWhenReady == true) record(mediaItem.mediaId)
        }

        override fun onPlayerError(error: PlaybackException) {
            val c = controller ?: return
            val name = c.currentMediaItem?.mediaMetadata?.title?.toString() ?: "lagu ini"
            _message.value = "Tidak dapat memutar $name"
            errorStreak++
            // Lewati file rusak/hilang, tetapi berhenti jika semua lagu di antrean gagal berturut-turut.
            if (c.hasNextMediaItem() && errorStreak < c.mediaItemCount) {
                c.seekToNextMediaItem()
                c.prepare()
                c.play()
            }
        }
    }

    init {
        connect()
        // Perbarui posisi hanya saat memutar; antrean dibangun ulang hanya saat ada event.
        viewModelScope.launch {
            while (isActive) {
                delay(500)
                controller?.takeIf { it.isPlaying }?.let { c ->
                    _state.update { it.copy(positionMs = c.currentPosition) }
                }
            }
        }
    }

    private fun connect() {
        val app = getApplication<Application>()
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching { future.get() }.onSuccess { c ->
                    controller = c
                    c.addListener(listener)
                    refresh(c)
                }
            },
            ContextCompat.getMainExecutor(app),
        )
    }

    private fun refresh(p: Player) {
        val queue = (0 until p.mediaItemCount).map { i ->
            val md = p.getMediaItemAt(i).mediaMetadata
            QueueEntry(md.title?.toString() ?: "Tanpa judul", md.artist?.toString())
        }
        val current = p.currentMediaItem?.mediaMetadata
        _state.value = PlayerUiState(
            connected = true,
            isPlaying = p.isPlaying,
            title = current?.title?.toString().orEmpty(),
            artist = current?.artist?.toString(),
            positionMs = p.currentPosition.coerceAtLeast(0L),
            durationMs = p.duration.takeIf { it != C.TIME_UNSET } ?: 0L,
            repeatMode = p.repeatMode,
            shuffle = p.shuffleModeEnabled,
            queue = queue,
            currentIndex = if (p.mediaItemCount == 0) -1 else p.currentMediaItemIndex,
            songId = p.currentMediaItem?.mediaId?.toLongOrNull(),
            uri = p.currentMediaItem?.localConfiguration?.uri?.toString(),
        )
    }

    /** Catat riwayat bila diaktifkan di pengaturan; satu catatan per pemutaran lagu. */
    private fun record(mediaId: String?) {
        val songId = mediaId?.toLongOrNull() ?: return
        if (mediaId == lastRecordedId) return
        lastRecordedId = mediaId
        viewModelScope.launch {
            if (gp.settings.historyEnabled.first()) {
                gp.dao.addHistory(HistoryEntity(songId = songId, playedAt = System.currentTimeMillis()))
            }
        }
    }

    // --- Aksi dari UI ---

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() { controller?.seekToNext() }
    fun previous() { controller?.seekToPrevious() }
    fun seekTo(ms: Long) { controller?.seekTo(ms) }

    fun playAt(index: Int) {
        controller?.run {
            seekToDefaultPosition(index)
            prepare()
            play()
        }
    }

    fun toggleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun removeAt(index: Int) { controller?.removeMediaItem(index) }

    fun move(index: Int, delta: Int) {
        val c = controller ?: return
        val target = index + delta
        if (target in 0 until c.mediaItemCount) c.moveMediaItem(index, target)
    }

    /** Ganti antrean dengan [songs] dan putar mulai dari [startIndex]. */
    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        c.setMediaItems(songs.map { it.toMediaItem() }, startIndex.coerceIn(songs.indices), 0L)
        c.prepare()
        c.play()
    }

    /** Sisipkan tepat setelah lagu yang sedang diputar. */
    fun playNext(songs: List<Song>) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        val items = songs.map { it.toMediaItem() }
        if (c.mediaItemCount == 0) {
            c.setMediaItems(items)
            c.prepare()
        } else {
            c.addMediaItems(c.currentMediaItemIndex + 1, items)
        }
    }

    fun enqueue(songs: List<Song>) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        val wasEmpty = c.mediaItemCount == 0
        c.addMediaItems(songs.map { it.toMediaItem() })
        if (wasEmpty) c.prepare()
    }

    /** Pemilih berkas sementara dari Tahap 2 (untuk uji tanpa izin media). */
    fun addUris(uris: List<Uri>) {
        val c = controller ?: return
        if (uris.isEmpty()) return
        val resolver = getApplication<Application>().contentResolver
        val items = uris.map { uri ->
            runCatching {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(displayName(uri)).build())
                .build()
        }
        val wasEmpty = c.mediaItemCount == 0
        c.addMediaItems(items)
        if (wasEmpty) c.prepare()
    }

    private fun displayName(uri: Uri): String {
        val resolver = getApplication<Application>().contentResolver
        val raw = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cur ->
                if (cur.moveToFirst()) cur.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment ?: "Tanpa judul"
        return raw.substringBeforeLast('.', raw)
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onCleared()
    }
}
