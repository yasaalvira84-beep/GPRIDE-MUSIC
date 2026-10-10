package com.gpride.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Kanvas visualizer; level dibaca di fase gambar sehingga tidak memicu recomposition tiap frame. */
@Composable
fun VisualizerCanvas(
    style: VisualizerStyle,
    bands: State<FloatArray>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "visualizer")
    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "rotation",
    )
    Canvas(modifier) {
        val b = bands.value
        when (style) {
            VisualizerStyle.Spectrum -> drawSpectrum(b, color)
            VisualizerStyle.Circular -> drawCircularBars(b, color, innerFraction = 0.45f, maxLenFraction = 0.5f)
            VisualizerStyle.Waveform -> drawWaveform(b, color, rotation.value)
            VisualizerStyle.Particle -> drawParticles(b, color, rotation.value)
        }
    }
}

internal fun DrawScope.drawSpectrum(b: FloatArray, color: Color) {
    val n = b.size
    if (n == 0) return
    val gap = 2.dp.toPx()
    val barWidth = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
    for (i in 0 until n) {
        val level = b[i].coerceIn(0f, 1f)
        val h = size.height * (0.04f + 0.96f * level)
        drawRoundRect(
            color = color.copy(alpha = 0.55f + 0.45f * level),
            topLeft = Offset(i * (barWidth + gap), size.height - h),
            size = Size(barWidth, h),
            cornerRadius = CornerRadius(barWidth / 2f),
        )
    }
}

internal fun DrawScope.drawCircularBars(
    b: FloatArray,
    color: Color,
    innerFraction: Float,
    maxLenFraction: Float,
) {
    val n = b.size
    if (n == 0) return
    val radius = size.minDimension / 2f
    val total = n * 2
    val inner = radius * innerFraction
    val maxLen = radius * maxLenFraction
    for (i in 0 until total) {
        val idx = if (i < n) i else total - 1 - i
        val level = b[idx].coerceIn(0f, 1f)
        val length = 2.dp.toPx() + maxLen * level
        val angle = (2.0 * PI * i / total - PI / 2.0).toFloat()
        drawLine(
            color = color.copy(alpha = 0.45f + 0.55f * level),
            start = Offset(center.x + inner * cos(angle), center.y + inner * sin(angle)),
            end = Offset(center.x + (inner + length) * cos(angle), center.y + (inner + length) * sin(angle)),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

internal fun DrawScope.drawWaveform(b: FloatArray, color: Color, rotationDeg: Float) {
    val n = b.size
    if (n < 2) return
    val mid = size.height / 2f
    val phase = (rotationDeg * PI / 180.0).toFloat() * 6f
    val upper = Path()
    val lower = Path()
    for (i in 0 until n) {
        val x = size.width * i / (n - 1)
        val level = b[i].coerceIn(0f, 1f)
        val wave = sin(i * 0.55f + phase) * level * mid * 0.9f
        if (i == 0) {
            upper.moveTo(x, mid - wave)
            lower.moveTo(x, mid + wave)
        } else {
            upper.lineTo(x, mid - wave)
            lower.lineTo(x, mid + wave)
        }
    }
    drawPath(lower, color.copy(alpha = 0.35f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(upper, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

internal fun DrawScope.drawParticles(b: FloatArray, color: Color, rotationDeg: Float) {
    if (b.isEmpty()) return
    val count = 90
    val maxR = size.minDimension / 2f
    val golden = 2.3999632f
    val rot = (rotationDeg * PI / 180.0).toFloat()
    for (i in 0 until count) {
        val level = b[i % b.size].coerceIn(0f, 1f)
        val frac = sqrt((i + 0.5f) / count)
        val angle = i * golden + rot * (1 + i % 3)
        val radius = maxR * frac * (0.8f + 0.35f * level)
        drawCircle(
            color = color.copy(alpha = 0.25f + 0.75f * level),
            radius = 1.5.dp.toPx() + 4.dp.toPx() * level,
            center = Offset(center.x + radius * cos(angle), center.y + radius * sin(angle)),
        )
    }
}
