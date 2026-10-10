package com.gpride.player

import android.annotation.SuppressLint
import android.media.audiofx.Visualizer
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.isActive
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow

const val VISUALIZER_BANDS = 48

/** Diisi PlaybackService; layanan dan UI berada di satu proses sehingga cukup variabel bersama. */
object AudioSessionHolder {
    @Volatile
    var id: Int = 0
}

enum class VisualizerStyle(val label: String) {
    Spectrum("Spectrum"),
    Circular("Circular"),
    Waveform("Waveform"),
    Particle("Particle"),
}

val VisualizerPalettes: List<Color> = listOf(
    Neon.Green,
    Color(0xFF00E5FF),
    Color(0xFFB36BFF),
    Color(0xFF4D7CFF),
)

/** Warna kedua tiap palet: batang bergradasi dari warna utama (bass) ke warna ini (treble). */
val VisualizerAccents: List<Color> = listOf(
    Color(0xFF00E5FF),
    Color(0xFFB36BFF),
    Color(0xFFFF4DDB),
    Color(0xFF00E5FF),
)

/**
 * Ubah data FFT Android Visualizer (pasangan real/imajiner bertanda) menjadi [bandCount] level 0..1
 * pada skala logaritmik. [sensitivity] 0..1 menaikkan level (1 = paling peka).
 */
fun computeBands(fft: ByteArray, bandCount: Int, sensitivity: Float = 0.7f): FloatArray {
    val out = FloatArray(bandCount.coerceAtLeast(0))
    val bins = fft.size / 2 - 1
    if (bandCount <= 0 || bins < bandCount) return out
    val gain = 0.5f + sensitivity.coerceIn(0f, 1f)
    var start = 1
    for (band in 0 until bandCount) {
        val edge = bins.toDouble().pow((band + 1).toDouble() / bandCount).toInt()
        val end = maxOf(start, minOf(edge, bins))
        var sum = 0f
        for (k in start..end) {
            val re = fft[2 * k].toInt()
            val im = fft[2 * k + 1].toInt()
            sum += hypot(re.toFloat(), im.toFloat())
        }
        val magnitude = sum / (end - start + 1)
        val db = 20f * log10(magnitude + 1f)
        val tilt = 0.85f + 0.4f * band / bandCount
        out[band] = (db / 40f * gain * tilt).coerceIn(0f, 1f)
        start = minOf(end + 1, bins)
    }
    return out
}

/** Pembungkus android.media.audiofx.Visualizer yang menempel pada sesi audio ExoPlayer. */
class AudioVisualizer(private val onBands: (FloatArray) -> Unit) {
    private var visualizer: Visualizer? = null
    private var lastEmit = 0L

    @Volatile
    var sensitivity: Float = 0.7f

    /** Butuh izin RECORD_AUDIO; pemanggil wajib memastikan izin sudah diberikan. */
    @SuppressLint("MissingPermission")
    fun start(sessionId: Int): Boolean {
        stop()
        if (sessionId == 0) return false
        return runCatching {
            val v = Visualizer(sessionId)
            v.setEnabled(false)
            v.setCaptureSize(Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024))
            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(vis: Visualizer?, waveform: ByteArray?, samplingRate: Int) = Unit

                    override fun onFftDataCapture(vis: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft == null) return
                        val now = SystemClock.uptimeMillis()
                        if (now - lastEmit < 15) return
                        lastEmit = now
                        onBands(computeBands(fft, VISUALIZER_BANDS, sensitivity))
                    }
                },
                Visualizer.getMaxCaptureRate(),
                false,
                true,
            )
            v.setEnabled(true)
            visualizer = v
            true
        }.getOrDefault(false)
    }

    fun stop() {
        visualizer?.let { v ->
            runCatching {
                v.setEnabled(false)
                v.release()
            }
        }
        visualizer = null
    }
}

class VisualizerFeed(
    val bands: State<FloatArray>,
    val peaks: State<FloatArray>,
    val failed: State<Boolean>,
)

/**
 * Menyalakan visualizer hanya selama [active] (layar terlihat, izin ada, lagu diputar).
 * Level dihaluskan per frame di sisi UI; puncak (peak) turun perlahan di atas tiap batang.
 */
@Composable
fun rememberVisualizerFeed(active: Boolean, sensitivity: Float, fps: Int): VisualizerFeed {
    val target = remember { FloatArray(VISUALIZER_BANDS) }
    val shown = remember { mutableStateOf(FloatArray(VISUALIZER_BANDS)) }
    val peaks = remember { mutableStateOf(FloatArray(VISUALIZER_BANDS)) }
    val failed = remember { mutableStateOf(false) }
    val engine = remember { AudioVisualizer { raw -> raw.copyInto(target) } }
    engine.sensitivity = sensitivity

    DisposableEffect(active) {
        failed.value = false
        if (active) failed.value = !engine.start(AudioSessionHolder.id)
        onDispose {
            engine.stop()
            target.fill(0f)
        }
    }

    LaunchedEffect(active, fps) {
        if (!active) {
            shown.value = FloatArray(VISUALIZER_BANDS)
            peaks.value = FloatArray(VISUALIZER_BANDS)
            return@LaunchedEffect
        }
        val frameNs = 1_000_000_000L / fps.coerceIn(10, 120)
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (now - last >= frameNs) {
                    val dt = if (last == 0L) 1f / 60f else ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                    last = now
                    val current = shown.value
                    val next = FloatArray(VISUALIZER_BANDS) { i ->
                        val t = target[i]
                        val c = current[i]
                        if (t > c) c + (t - c) * 0.6f else c + (t - c) * 0.15f
                    }
                    val previousPeaks = peaks.value
                    peaks.value = FloatArray(VISUALIZER_BANDS) { i -> maxOf(next[i], previousPeaks[i] - 0.9f * dt) }
                    shown.value = next
                }
            }
        }
    }
    return remember { VisualizerFeed(shown, peaks, failed) }
}
