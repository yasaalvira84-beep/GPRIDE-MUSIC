package com.gpride.player

import kotlin.math.roundToInt

/** Preset equalizer: [gainsDb] adalah kurva rendah ke tinggi (5 titik) yang dipetakan ke jumlah band perangkat. */
class EqPreset(val label: String, val gainsDb: FloatArray)

val EqPresets: List<EqPreset> = listOf(
    EqPreset("Flat", floatArrayOf(0f, 0f, 0f, 0f, 0f)),
    EqPreset("Bass Boost", floatArrayOf(6f, 4f, 1f, 0f, 0f)),
    EqPreset("Vokal", floatArrayOf(-2f, -1f, 3f, 3f, 0f)),
    EqPreset("Treble", floatArrayOf(-1f, 0f, 1f, 4f, 6f)),
    EqPreset("Rock", floatArrayOf(5f, 3f, -1f, 3f, 5f)),
    EqPreset("Pop", floatArrayOf(-1f, 2f, 4f, 2f, -1f)),
    EqPreset("Elektronik", floatArrayOf(5f, 3f, 0f, 2f, 5f)),
    EqPreset("Akustik", floatArrayOf(3f, 2f, 1f, 2f, 3f)),
)

/** Info kemampuan equalizer perangkat; level dalam millibel (100 mB = 1 dB). */
data class EqInfo(val bandCount: Int, val minMb: Int, val maxMb: Int, val centerHz: List<Int>)

data class AudioPrefs(
    val eqEnabled: Boolean = false,
    /** Indeks di [EqPresets]; -1 berarti kustom (pakai [eqCustomMb]). */
    val eqPreset: Int = 0,
    val eqCustomMb: List<Int> = emptyList(),
    /** Kekuatan bass boost 0..1000. */
    val bassBoost: Int = 0,
    /** Lama pudar volume di awal/akhir lagu dalam detik; 0 = mati. */
    val fadeSec: Int = 0,
    val skipSilence: Boolean = false,
)

/** Memetakan kurva preset (interpolasi linear) ke [bandCount] band, dibatasi rentang perangkat. */
fun presetBandLevels(gainsDb: FloatArray, bandCount: Int, minMb: Int, maxMb: Int): IntArray {
    if (bandCount <= 0 || gainsDb.isEmpty()) return IntArray(maxOf(bandCount, 0))
    return IntArray(bandCount) { i ->
        val pos = if (bandCount == 1 || gainsDb.size == 1) 0f else i * (gainsDb.size - 1) / (bandCount - 1).toFloat()
        val lo = pos.toInt().coerceIn(0, gainsDb.size - 1)
        val hi = (lo + 1).coerceAtMost(gainsDb.size - 1)
        val frac = pos - lo
        val db = gainsDb[lo] * (1f - frac) + gainsDb[hi] * frac
        (db * 100f).roundToInt().coerceIn(minMb, maxMb)
    }
}

/** Level tiap band (millibel) untuk pengaturan saat ini: dari preset, atau dari nilai kustom. */
fun eqLevels(p: AudioPrefs, info: EqInfo): IntArray =
    if (p.eqPreset in EqPresets.indices) {
        presetBandLevels(EqPresets[p.eqPreset].gainsDb, info.bandCount, info.minMb, info.maxMb)
    } else {
        IntArray(info.bandCount) { p.eqCustomMb.getOrElse(it) { 0 }.coerceIn(info.minMb, info.maxMb) }
    }

/** Pengali volume 0..1: naik di awal lagu dan turun di akhir lagu. 1 jika pudar mati atau durasi tidak diketahui. */
fun fadeFactor(positionMs: Long, durationMs: Long, fadeMs: Long): Float {
    if (fadeMs <= 0L || durationMs <= 0L) return 1f
    val fade = minOf(fadeMs, durationMs / 2).coerceAtLeast(1L)
    val fadeIn = (positionMs.toFloat() / fade).coerceIn(0f, 1f)
    val fadeOut = ((durationMs - positionMs).toFloat() / fade).coerceIn(0f, 1f)
    return minOf(fadeIn, fadeOut)
}

/** Pengali volume menjelang timer tidur habis: turun linear selama [fadeMs] terakhir. */
fun sleepFadeFactor(remainingMs: Long, fadeMs: Long): Float =
    if (fadeMs <= 0L || remainingMs >= fadeMs) 1f else (remainingMs.toFloat() / fadeMs).coerceIn(0f, 1f)

fun formatHz(hz: Int): String = when {
    hz >= 1000 && hz % 1000 == 0 -> "${hz / 1000} kHz"
    hz >= 1000 -> "%.1f kHz".format(hz / 1000f)
    else -> "$hz Hz"
}
