package com.antigravity.tvlauncher.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.tv.material3.darkColorScheme
import com.antigravity.tvlauncher.data.ThemePreset

// ── Static colors (never change with theme) ───────────────────────────────────
val Black        = Color(0xFF000000)
val TextPrimary  = Color(0xFFFFFFFF)
val TextSecondary= Color(0xFFA0A0A0)

// ── CompositionLocals for dynamic theme tokens ────────────────────────────────

/** Current accent color — changes when user picks a preset or custom color. */
val LocalAccentColor = staticCompositionLocalOf { Color(0xFF4FC3F7) }

/** Current background color — changes with preset. */
val LocalBgColor = staticCompositionLocalOf { Color(0xFF080C14) }

/** Current surface/card background color — changes with preset. */
val LocalSurfaceColor = staticCompositionLocalOf { Color(0xFF0F1520) }

/** Current card corner radius in dp — changes with preset. */
val LocalCardRadius = staticCompositionLocalOf { 8 }

// ── Theme wrapper ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvLauncherTheme(
    preset: ThemePreset = ThemePreset.byName("Midnight"),
    content: @Composable () -> Unit
) {
    val accentColor  = parseHex(preset.accentHex)
    val bgColor      = parseHex(preset.backgroundHex)
    val surfaceColor = parseHex(preset.surfaceHex)

    val colorScheme = darkColorScheme(
        primary         = accentColor,
        onPrimary       = Color.Black,
        secondary       = accentColor.copy(alpha = 0.7f),
        onSecondary     = Color.Black,
        tertiary        = accentColor.copy(alpha = 0.5f),
        background      = bgColor,
        surface         = surfaceColor,
        onBackground    = TextPrimary,
        onSurface       = TextPrimary,
        surfaceVariant  = surfaceColor.copy(alpha = 0.7f),
        onSurfaceVariant= TextSecondary,
    )

    CompositionLocalProvider(
        LocalAccentColor provides accentColor,
        LocalBgColor     provides bgColor,
        LocalSurfaceColor provides surfaceColor,
        LocalCardRadius  provides preset.cardRadiusDp,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content     = content
        )
    }
}

// ── Utility ───────────────────────────────────────────────────────────────────

/** Safely parse a hex color string, returning a fallback if invalid. */
fun parseHex(hex: String, fallback: Color = Color(0xFF080C14)): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) { fallback }
