package com.antigravity.tvlauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

// ── Colour palette (FLauncher / Google TV inspired) ──────────────────────────
val Black          = Color(0xFF000000)
val SurfaceDark    = Color(0xFF0F0F0F)
val SurfaceCard    = Color(0xFF1A1A1A)
val Divider        = Color(0xFF2A2A2A)
val TextPrimary    = Color(0xFFFFFFFF)
val TextSecondary  = Color(0xFF9E9E9E)
val AccentBlue     = Color(0xFF2979FF)
val AccentBlueDim  = Color(0xFF1A4A8A)
val FocusGlow      = Color(0xFFFFFFFF)

private val DarkColors = darkColorScheme(
    primary            = TextPrimary,
    onPrimary          = Black,
    primaryContainer   = SurfaceCard,
    onPrimaryContainer = TextPrimary,
    secondary          = TextSecondary,
    onSecondary        = TextPrimary,
    background         = Black,
    onBackground       = TextPrimary,
    surface            = SurfaceDark,
    onSurface          = TextPrimary,
    surfaceVariant     = SurfaceCard,
    onSurfaceVariant   = TextSecondary,
    border             = Divider
)

val LauncherTypography = Typography(
    headlineLarge  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,     fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,     fontSize = 26.sp, lineHeight = 34.sp, letterSpacing = (-0.3).sp),
    titleLarge     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium    = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,   fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    bodyLarge      = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    labelLarge     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,   fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium    = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,   fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
)

@Composable
fun TvLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography  = LauncherTypography,
        content     = content
    )
}
