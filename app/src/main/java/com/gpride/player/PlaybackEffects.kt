package com.gpride.player

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow

data class SleepTimerState(
    /** Waktu berhenti (basis SystemClock.elapsedRealtime), atau null. */
    val endAtMs: Long? = null,
    /** Berhenti setelah lagu yang sedang diputar selesai. */
    val endOfSong: Boolean = false,
) {
    val active: Boolean get() = endAtMs != null || endOfSong
}

/**
 * Jembatan UI dan PlaybackService (satu proses, pola yang sama dengan AudioSessionHolder).
 * Timer dan equalizer dijalankan di layanan agar tetap bekerja saat aplikasi ditutup.
 */
object PlaybackEffects {
    val sleep = MutableStateFlow(SleepTimerState())

    /** Diisi layanan saat equalizer tersedia; null bila perangkat tidak mendukung. */
    val eqInfo = MutableStateFlow<EqInfo?>(null)

    fun sleepInMinutes(minutes: Int) {
        sleep.value = SleepTimerState(endAtMs = SystemClock.elapsedRealtime() + minutes * 60_000L)
    }

    fun sleepAtEndOfSong() {
        sleep.value = SleepTimerState(endOfSong = true)
    }

    fun cancelSleep() {
        sleep.value = SleepTimerState()
    }
}
