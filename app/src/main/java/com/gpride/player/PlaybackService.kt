package com.gpride.player

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Satu-satunya pemilik ExoPlayer dan antrean. UI hanya terhubung lewat MediaController.
 * Media3 mengurus notifikasi media, lock screen, headset/Bluetooth, dan foreground service.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true) // jeda saat headset dicabut
            .build()
        // ID sesi audio dipakai UI untuk menempelkan Visualizer ke keluaran pemutar ini.
        AudioSessionHolder.id = player.audioSessionId
        player.addListener(
            object : Player.Listener {
                override fun onEvents(p: Player, events: Player.Events) {
                    (p as? ExoPlayer)?.let { AudioSessionHolder.id = it.audioSessionId }
                }
            },
        )
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        AudioSessionHolder.id = 0
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
