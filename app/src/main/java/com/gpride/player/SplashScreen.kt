package com.gpride.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Logo huruf G neon: busur hampir penuh dengan palang di tengah, plus lapisan pendar. */
@Composable
fun GprideLogo(modifier: Modifier = Modifier, color: Color = Neon.Green) {
    Canvas(modifier) {
        val r = size.minDimension * 0.34f
        val stroke = size.minDimension * 0.11f
        val c = center
        val arcTopLeft = Offset(c.x - r, c.y - r)
        val arcSize = Size(r * 2f, r * 2f)
        val endAngle = 10.0 * PI / 180.0
        val endX = c.x + r * cos(endAngle).toFloat()
        val endY = c.y + r * sin(endAngle).toFloat()
        val bar = Path().apply {
            moveTo(endX, endY)
            lineTo(endX, c.y)
            lineTo(c.x + r * 0.05f, c.y)
        }
        val glow = Stroke(width = stroke * 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val core = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawArc(color.copy(alpha = 0.18f), -30f, -320f, false, arcTopLeft, arcSize, style = glow)
        drawPath(bar, color.copy(alpha = 0.18f), style = glow)
        drawArc(color, -30f, -320f, false, arcTopLeft, arcSize, style = core)
        drawPath(bar, color, style = core)
    }
}

/** Splash singkat: logo, nama, tagline, dan bar progres tipis. */
@Composable
fun SplashScreen() {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }
    Box(
        Modifier.fillMaxSize().background(Neon.Background).pointerInput(Unit) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GprideLogo(Modifier.size(132.dp))
            Spacer(Modifier.height(20.dp))
            Text(
                "GPRIDE",
                color = Neon.TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 8.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text("Music in Your Way", color = Neon.Green, fontSize = 14.sp)
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
                .width(120.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Neon.Surface),
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress.value).background(Neon.Green))
        }
    }
}
