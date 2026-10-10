package com.gpride.player

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Palet "Neon Pulse". */
object Neon {
    val Green = Color(0xFF39FF14)
    val Background = Color(0xFF0A0A0A)
    val Surface = Color(0xFF1A1F1B)
    val TextPrimary = Color(0xFFF5F7F5)
    val TextSecondary = Color(0xFF8B8F8A)
    val GreenDeep = Color(0xFF14341A)
}

private val NeonDark = darkColorScheme(
    primary = Neon.Green,
    onPrimary = Neon.Background,
    primaryContainer = Neon.GreenDeep,
    onPrimaryContainer = Neon.Green,
    secondary = Neon.Green,
    onSecondary = Neon.Background,
    secondaryContainer = Neon.GreenDeep,
    onSecondaryContainer = Neon.Green,
    tertiary = Color(0xFF7CFF63),
    background = Neon.Background,
    onBackground = Neon.TextPrimary,
    surface = Neon.Background,
    onSurface = Neon.TextPrimary,
    surfaceVariant = Neon.Surface,
    onSurfaceVariant = Neon.TextSecondary,
    surfaceContainerLowest = Color(0xFF050505),
    surfaceContainerLow = Color(0xFF111411),
    surfaceContainer = Neon.Surface,
    surfaceContainerHigh = Color(0xFF222822),
    surfaceContainerHighest = Color(0xFF2A312B),
    outline = Color(0xFF3A423B),
    outlineVariant = Color(0xFF262C27),
)

private val NeonLight = lightColorScheme(
    primary = Color(0xFF1B8A00),
    onPrimary = Color.White,
    secondaryContainer = Color(0xFFD7F5CF),
    onSecondaryContainer = Color(0xFF0F5200),
)

private val GprideTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
)

private val GprideShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
)

/** Tema Neon Pulse: gelap AMOLED dengan aksen hijau neon. Mode terang disiapkan untuk pengaturan nanti. */
@Composable
fun GprideTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) NeonDark else NeonLight,
        typography = GprideTypography,
        shapes = GprideShapes,
        content = content,
    )
}
