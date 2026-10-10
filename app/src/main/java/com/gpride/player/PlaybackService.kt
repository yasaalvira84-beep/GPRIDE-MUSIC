package com.gpride.player

import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val SLEEP_FADE_MS = 8_000L

/**
 * Satu-satunya pemilik ExoPlayer dan antrean. UI hanya terhubung lewat MediaController.
 * Media3 mengurus notifikasi media, lock screen, headset/Bluetooth, dan foreground service.
 * Layanan ini juga menjalankan timer tidur, equalizer/bass boost, dan pudar volume antarlagu.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exo: ExoPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var effectsSessionId = 0
    private var audioPrefs = AudioPrefs()
    private var fadeMs = 0L

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
        exo = player
        // ID sesi audio dipakai UI untuk menempelkan Visualizer ke keluaran pemutar ini.
        AudioSessionHolder.id = player.audioSessionId
        attachEffects(player.audioSessionId)
        player.addListener(
            object : Player.Listener {
                override fun onEvents(p: Player, events: Player.Events) {
                    (p as? ExoPlayer)?.let {
                        AudioSessionHolder.id = it.audioSessionId
                        if (it.audioSessionId != effectsSessionId) attachEffects(it.audioSessionId)
                    }
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    // Pemutar berhenti sendiri di akhir lagu karena timer "setelah lagu ini".
                    if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM &&
                        PlaybackEffects.sleep.value.endOfSong
                    ) {
                        PlaybackEffects.cancelSleep()
                    }
                }
            },
        )
        mediaSession = MediaSession.Builder(this, player).build()

        val app = application as GprideApplication
        scope.launch {
            app.settings.audio.collect {
                audioPrefs = it
                applyAudioPrefs()
            }
        }
        scope.launch {
            PlaybackEffects.sleep.collect { exo?.pauseAtEndOfMediaItems = it.endOfSong }
        }
        scope.launch { volumeLoop() }
    }

    /** Mengatur volume dari pudar antarlagu dan pudar timer tidur, serta menghentikan musik saat timer habis. */
    private suspend fun volumeLoop() {
        while (scope.isActive) {
            val p = exo ?: return
            var sleepFactor = 1f
            PlaybackEffects.sleep.value.endAtMs?.let { end ->
                val remaining = end - SystemClock.elapsedRealtime()
                if (remaining <= 0L) {
                    p.pause()
                    PlaybackEffects.cancelSleep()
                } else {
                    sleepFactor = sleepFadeFactor(remaining, SLEEP_FADE_MS)
                }
            }
            val duration = p.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            val trackFactor = fadeFactor(p.currentPosition, duration, fadeMs)
            val target = minOf(trackFactor, sleepFactor)
            if (abs(p.volume - target) > 0.01f) p.volume = target
            delay(if (p.isPlaying) 100L else 500L)
        }
    }

    private fun attachEffects(sessionId: Int) {
        releaseEffects()
        effectsSessionId = sessionId
        if (sessionId == 0) {
            PlaybackEffects.eqInfo.value = null
            return
        }
        equalizer = runCatching { Equalizer(0, sessionId) }.getOrNull()
        bassBoost = runCatching { BassBoost(0, sessionId) }.getOrNull()
        PlaybackEffects.eqInfo.value = equalizer?.let { eq ->
            runCatching {
                val range = eq.bandLevelRange
                val bands = eq.numberOfBands.toInt()
                EqInfo(
                    bandCount = bands,
                    minMb = range[0].toInt(),
                    maxMb = range[1].toInt(),
                    centerHz = (0 until bands).map { eq.getCenterFreq(it.toShort()) / 1000 },
                )
            }.getOrNull()
        }
        applyAudioPrefs()
    }

    private fun applyAudioPrefs() {
        val p = audioPrefs
        exo?.skipSilenceEnabled = p.skipSilence
        fadeMs = p.fadeSec * 1000L
        val info = PlaybackEffects.eqInfo.value
        runCatching {
            equalizer?.let { eq ->
                if (p.eqEnabled && info != null) {
                    val levels = eqLevels(p, info)
                    for (i in 0 until info.bandCount) eq.setBandLevel(i.toShort(), levels[i].toShort())
                    eq.setEnabled(true)
                } else {
                    eq.setEnabled(false)
                }
            }
        }
        runCatching {
            bassBoost?.let { bb ->
                if (p.eqEnabled && p.bassBoost > 0 && bb.strengthSupported) {
                    bb.setStrength(p.bassBoost.coerceIn(0, 1000).toShort())
                    bb.setEnabled(true)
                } else {
                    bb.setEnabled(false)
                }
            }
        }
    }

    private fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        equalizer = null
        bassBoost = null
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        releaseEffects()
        PlaybackEffects.eqInfo.value = null
        PlaybackEffects.cancelSleep()
        AudioSessionHolder.id = 0
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        exo = null
        super.onDestroy()
    }
}
