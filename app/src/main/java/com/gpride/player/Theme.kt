package com.gpride.player

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
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

/** Pilihan warna aksen; indeks 0 adalah hijau neon bawaan. */
class AccentOption(val name: String, val color: Color)

val AccentOptions: List<AccentOption> = listOf(
    AccentOption("Hijau Neon", Neon.Green),
    AccentOption("Cyan", Color(0xFF00E5FF)),
    AccentOption("Biru", Color(0xFF4D9BFF)),
    AccentOption("Ungu", Color(0xFFB66DFF)),
    AccentOption("Pink", Color(0xFFFF4FA3)),
    AccentOption("Oranye", Color(0xFFFF9F1C)),
    AccentOption("Kuning", Color(0xFFFFE600)),
    AccentOption("Merah", Color(0xFFFF4D4D)),
)

/** Skema warna gelap dengan aksen pilihan; [amoled] membuat latar hitam pekat (hemat daya di layar AMOLED). */
private fun accentScheme(accent: Color, amoled: Boolean): ColorScheme {
    fun tint(gray: Long, amount: Float) = lerp(Color(gray), accent, amount)
    val base = if (amoled) Color.Black else Neon.Background
    val onAccent = if (accent.luminance() > 0.5f) Color(0xFF0A0A0A) else Color.White
    val container = lerp(Color.Black, accent, 0.22f)
    val low = if (amoled) tint(0xFF0A0A0A, 0.04f) else tint(0xFF111111, 0.05f)
    val mid = if (amoled) tint(0xFF121212, 0.06f) else tint(0xFF1A1A1A, 0.07f)
    val high = if (amoled) tint(0xFF1B1B1B, 0.06f) else tint(0xFF222222, 0.07f)
    val highest = if (amoled) tint(0xFF242424, 0.06f) else tint(0xFF2A2A2A, 0.07f)
    return darkColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = container,
        onPrimaryContainer = accent,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = container,
        onSecondaryContainer = accent,
        tertiary = lerp(accent, Color.White, 0.35f),
        background = base,
        onBackground = Neon.TextPrimary,
        surface = base,
        onSurface = Neon.TextPrimary,
        surfaceVariant = mid,
        onSurfaceVariant = Neon.TextSecondary,
        surfaceContainerLowest = if (amoled) Color.Black else Color(0xFF050505),
        surfaceContainerLow = low,
        surfaceContainer = mid,
        surfaceContainerHigh = high,
        surfaceContainerHighest = highest,
        outline = tint(0xFF3A3A3A, 0.12f),
        outlineVariant = tint(0xFF262626, 0.10f),
    )
}

/** Tema Neon Pulse: gelap dengan aksen pilihan (bawaan hijau neon) dan opsi AMOLED hitam pekat. */
@Composable
fun GprideTheme(
    darkTheme: Boolean = true,
    accentIndex: Int = 0,
    amoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val index = accentIndex.coerceIn(0, AccentOptions.lastIndex)
    val dark = if (index == 0 && !amoled) NeonDark else accentScheme(AccentOptions[index].color, amoled)
    MaterialTheme(
        colorScheme = if (darkTheme) dark else NeonLight,
        typography = GprideTypography,
        shapes = GprideShapes,
        content = content,
    )
}
