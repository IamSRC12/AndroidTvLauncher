package com.antigravity.tvlauncher.data

/**
 * A complete visual theme preset.
 * Applying a preset changes accent color, background, surface color, and card radius all at once.
 */
data class ThemePreset(
    val name: String,
    val accentHex: String,
    val backgroundHex: String,
    val surfaceHex: String,
    val cardRadiusDp: Int = 12
) {
    companion object {

        /** All 10 built-in theme presets. */
        val ALL: List<ThemePreset> = listOf(
            ThemePreset(
                name          = "Midnight",
                accentHex     = "#4FC3F7",
                backgroundHex = "#080C14",
                surfaceHex    = "#0F1520",
                cardRadiusDp  = 8
            ),
            ThemePreset(
                name          = "Ember",
                accentHex     = "#FF6B35",
                backgroundHex = "#110800",
                surfaceHex    = "#1E1000",
                cardRadiusDp  = 10
            ),
            ThemePreset(
                name          = "Forest",
                accentHex     = "#4CAF50",
                backgroundHex = "#071209",
                surfaceHex    = "#0F2011",
                cardRadiusDp  = 16
            ),
            ThemePreset(
                name          = "Ice",
                accentHex     = "#80DEEA",
                backgroundHex = "#0A1020",
                surfaceHex    = "#14203A",
                cardRadiusDp  = 20
            ),
            ThemePreset(
                name          = "Sakura",
                accentHex     = "#F48FB1",
                backgroundHex = "#1A0D12",
                surfaceHex    = "#2A1520",
                cardRadiusDp  = 24
            ),
            ThemePreset(
                name          = "Carbon",
                accentHex     = "#E0E0E0",
                backgroundHex = "#050505",
                surfaceHex    = "#111111",
                cardRadiusDp  = 4
            ),
            ThemePreset(
                name          = "Neon",
                accentHex     = "#39FF14",
                backgroundHex = "#020202",
                surfaceHex    = "#0A0A0A",
                cardRadiusDp  = 6
            ),
            ThemePreset(
                name          = "Minimal",
                accentHex     = "#FFFFFF",
                backgroundHex = "#141414",
                surfaceHex    = "#1E1E1E",
                cardRadiusDp  = 4
            ),
            ThemePreset(
                name          = "Classic TV",
                accentHex     = "#FFD740",
                backgroundHex = "#001133",
                surfaceHex    = "#002255",
                cardRadiusDp  = 0
            ),
            ThemePreset(
                name          = "Retro",
                accentHex     = "#F5C518",
                backgroundHex = "#1A1000",
                surfaceHex    = "#2E1F00",
                cardRadiusDp  = 14
            ),
        )

        /** Find a preset by name, falling back to Midnight if not found. */
        fun byName(name: String): ThemePreset = ALL.find { it.name == name } ?: ALL.first()
    }
}
