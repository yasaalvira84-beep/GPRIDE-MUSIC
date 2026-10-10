package com.gpride.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Kanvas visualizer. Level dibaca di fase gambar sehingga tidak memicu recomposition tiap frame.
 * Semua gaya ikut berdenyut mengikuti bass dan bergradasi dari [color] ke [accent].
 */
@Composable
fun VisualizerCanvas(
    style: VisualizerStyle,
    feed: VisualizerFeed,
    color: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    frame: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "visualizer")
    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "rotation",
    )
    Canvas(modifier) {
        val b = feed.bands.value
        val p = feed.peaks.value
        val bass = bassOf(b)
        when (style) {
            VisualizerStyle.Spectrum -> drawSpectrum(b, p, color, accent, bass)
            VisualizerStyle.Circular -> drawCircularBars(b, p, color, accent, bass, rotation.value, 0.45f, 0.5f)
            VisualizerStyle.Waveform -> drawWaveform(b, color, accent, bass, rotation.value)
            VisualizerStyle.Particle -> drawParticles(b, color, accent, bass, rotation.value)
        }
        if (frame) {
            drawRoundRect(
                color = color.copy(alpha = 0.15f + 0.6f * bass),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
    }
}

/** Energi bass: rata-rata beberapa band terendah. */
private fun bassOf(b: FloatArray): Float {
    if (b.size < 4) return 0f
    return ((b[0] + b[1] + b[2] + b[3]) / 4f).coerceIn(0f, 1f)
}

/** Batang bergradasi dengan pendar, pantulan di bawah garis dasar, dan penanda puncak. */
internal fun DrawScope.drawSpectrum(b: FloatArray, peaks: FloatArray, color: Color, accent: Color, bass: Float) {
    val n = b.size
    if (n == 0) return
    val margin = 16.dp.toPx()
    val base = size.height * 0.64f
    val maxH = base * 0.92f
    val gap = 2.dp.toPx()
    val barWidth = ((size.width - 2 * margin - gap * (n - 1)) / n).coerceAtLeast(1f)

    // cahaya latar yang berdenyut mengikuti bass
    val glowCenter = Offset(size.width / 2f, base)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.10f + 0.30f * bass), Color.Transparent),
            center = glowCenter,
            radius = size.width * 0.55f,
        ),
        radius = size.width * 0.55f,
        center = glowCenter,
    )

    for (i in 0 until n) {
        val t = i / (n - 1).coerceAtLeast(1).toFloat()
        val barColor = lerp(color, accent, t)
        val level = b[i].coerceIn(0f, 1f)
        val h = maxH * (0.03f + 0.97f * level)
        val x = margin + i * (barWidth + gap)
        val corner = CornerRadius(barWidth / 2f)

        // pendar
        drawRoundRect(
            color = barColor.copy(alpha = 0.10f + 0.22f * level),
            topLeft = Offset(x - gap, base - h - gap),
            size = Size(barWidth + 2 * gap, h + 2 * gap),
            cornerRadius = CornerRadius(barWidth),
        )
        // batang
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(lerp(barColor, Color.White, 0.55f), barColor),
                startY = base - h,
                endY = base,
            ),
            topLeft = Offset(x, base - h),
            size = Size(barWidth, h),
            cornerRadius = corner,
        )
        // pantulan
        val reflectionH = h * 0.4f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(barColor.copy(alpha = 0.35f), Color.Transparent),
                startY = base + gap,
                endY = base + gap + reflectionH,
            ),
            topLeft = Offset(x, base + gap),
            size = Size(barWidth, reflectionH),
            cornerRadius = corner,
        )
        // puncak
        val peakH = maxH * (0.03f + 0.97f * peaks.getOrElse(i) { 0f }.coerceIn(0f, 1f))
        drawRoundRect(
            color = Color.White.copy(alpha = 0.9f),
            topLeft = Offset(x, base - peakH - 4.dp.toPx()),
            size = Size(barWidth, 2.dp.toPx()),
            cornerRadius = CornerRadius(1.dp.toPx()),
        )
    }
}

/** Cincin batang simetris yang berputar pelan, berdenyut mengikuti bass, dengan titik puncak. */
internal fun DrawScope.drawCircularBars(
    b: FloatArray,
    peaks: FloatArray,
    color: Color,
    accent: Color,
    bass: Float,
    rotationDeg: Float,
    innerFraction: Float,
    maxLenFraction: Float,
) {
    val n = b.size
    if (n == 0) return
    val radius = size.minDimension / 2f
    val total = n * 2
    val inner = radius * innerFraction * (1f + 0.06f * bass)
    val maxLen = radius * maxLenFraction
    val rot = (rotationDeg * PI / 180.0)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.10f + 0.35f * bass), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
    drawCircle(
        color = color.copy(alpha = 0.4f + 0.5f * bass),
        radius = inner * 0.96f,
        style = Stroke(width = 2.dp.toPx() + 2.dp.toPx() * bass),
    )

    for (i in 0 until total) {
        val idx = if (i < n) i else total - 1 - i
        val level = b[idx].coerceIn(0f, 1f)
        val t = idx / (n - 1).coerceAtLeast(1).toFloat()
        val tickColor = lerp(color, accent, t)
        val length = 2.dp.toPx() + maxLen * level
        val angle = (2.0 * PI * i / total - PI / 2.0 + rot).toFloat()
        val dirX = cos(angle)
        val dirY = sin(angle)
        val start = Offset(center.x + inner * dirX, center.y + inner * dirY)
        val end = Offset(center.x + (inner + length) * dirX, center.y + (inner + length) * dirY)
        drawLine(
            color = tickColor.copy(alpha = 0.14f + 0.20f * level),
            start = start,
            end = end,
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tickColor.copy(alpha = 0.55f + 0.45f * level),
            start = start,
            end = end,
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        val peak = peaks.getOrElse(idx) { 0f }.coerceIn(0f, 1f)
        if (peak > 0.05f) {
            val dist = inner + 4.dp.toPx() + maxLen * peak
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 1.5.dp.toPx(),
                center = Offset(center.x + dist * dirX, center.y + dist * dirY),
            )
        }
    }
}

/** Tiga gelombang berlapis yang mengecil di kedua ujung, dengan pendar. */
internal fun DrawScope.drawWaveform(b: FloatArray, color: Color, accent: Color, bass: Float, rotationDeg: Float) {
    val n = b.size
    if (n < 2) return
    val margin = 16.dp.toPx()
    val mid = size.height / 2f
    val phase = (rotationDeg * PI / 180.0).toFloat() * 6f
    val samples = 160
    for (layer in 0 until 3) {
        val path = Path()
        val amp = mid * (0.95f - 0.25f * layer) * (1f + 0.15f * bass)
        for (s in 0 until samples) {
            val pos = s * (n - 1) / (samples - 1f)
            val i0 = pos.toInt().coerceIn(0, n - 2)
            val frac = pos - i0
            val level = (b[i0] * (1f - frac) + b[i0 + 1] * frac).coerceIn(0f, 1f)
            val x = margin + (size.width - 2 * margin) * s / (samples - 1f)
            val taper = sin(PI.toFloat() * s / (samples - 1f))
            val wave = sin(s * (0.11f + 0.04f * layer) + phase * (layer + 1)) * level * amp * taper
            if (s == 0) path.moveTo(x, mid - wave) else path.lineTo(x, mid - wave)
        }
        val layerColor = lerp(color, accent, layer / 2f)
        drawPath(
            path = path,
            color = layerColor.copy(alpha = 0.16f),
            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            path = path,
            color = layerColor.copy(alpha = 1f - 0.3f * layer),
            style = Stroke(width = (3.5f - layer).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/** Partikel spiral yang mengembang dan berpendar mengikuti band serta bass. */
internal fun DrawScope.drawParticles(b: FloatArray, color: Color, accent: Color, bass: Float, rotationDeg: Float) {
    if (b.isEmpty()) return
    val count = 110
    val maxR = size.minDimension / 2f
    val golden = 2.3999632f
    val rot = (rotationDeg * PI / 180.0).toFloat()

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.08f + 0.30f * bass), Color.Transparent),
            center = center,
            radius = maxR,
        ),
        radius = maxR,
        center = center,
    )
    for (i in 0 until count) {
        val level = b[i % b.size].coerceIn(0f, 1f)
        val frac = sqrt((i + 0.5f) / count)
        val angle = i * golden + rot * (1 + i % 3)
        val radius = maxR * frac * (0.75f + 0.30f * level + 0.25f * bass)
        val dotColor = lerp(color, accent, frac)
        val pos = Offset(center.x + radius * cos(angle), center.y + radius * sin(angle))
        drawCircle(
            color = dotColor.copy(alpha = 0.12f + 0.20f * level),
            radius = 3.dp.toPx() + 8.dp.toPx() * level,
            center = pos,
        )
        drawCircle(
            color = dotColor.copy(alpha = 0.35f + 0.65f * level),
            radius = 1.5.dp.toPx() + 3.5.dp.toPx() * level,
            center = pos,
        )
    }
}
