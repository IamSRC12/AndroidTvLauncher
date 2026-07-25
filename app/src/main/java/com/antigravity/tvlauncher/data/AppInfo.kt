package com.antigravity.tvlauncher.data

import android.graphics.drawable.Drawable

// ── App category enum — used for row auto-population and grid filtering ────────
enum class AppCategory { STREAMING, GAME, MUSIC, TOOL, OTHER }

/**
 * Represents a single installed app with all metadata the launcher needs.
 * Icon is kept as [Drawable] so Coil can render it directly via rememberAsyncImagePainter.
 */
data class AppInfo(
    val packageName: String,
    val label: String,               // Label from PackageManager
    val icon: Drawable,
    val isFavorite: Boolean   = false,
    val isHidden: Boolean     = false,
    val launchCount: Int      = 0,   // Incremented by in-app tracker on each launch
    val lastLaunched: Long    = 0L,  // Epoch millis of last launch
    val installTime: Long     = 0L,  // Epoch millis from PackageManager
    val category: AppCategory = AppCategory.OTHER,
    val customLabel: String?  = null, // User-set override label
    val isNew: Boolean        = false  // True if installed within last 7 days
) {
    /** Effective display label — respects user override. */
    val displayLabel: String get() = customLabel ?: label
}

// ── Known package prefixes / full names for category auto-detection ────────────

private val STREAMING_PACKAGES = setOf(
    "com.netflix.ninja", "com.netflix.mediaclient",
    "com.amazon.avod.thirdpartyclient", "com.primevideo",
    "com.disney.disneyplus",
    "com.hbo.hbonow", "com.hbo.hbomax",
    "com.hulu.plus",
    "com.peacocktv.peacockandroid",
    "com.apple.atve.androidtv.appletv",
    "com.crunchyroll.crunchyroid",
    "com.plexapp.android",
    "org.jellyfin.androidtv",
    "app.emby.androidtv",
    "org.xbmc.kodi",
    "tv.twitch.android.app",
    "com.youtube.tv",
    "com.google.android.youtube.tv",
    "com.mxtech.videoplayer.ad",
    "com.mxtech.videoplayer.pro",
    "com.vix.tv",
    "com.dazn.app",
    "com.fubo.fuboTV",
    "com.sling",
    "com.paramountplus.app",
    "tv.plex.labs.plexamp"
)

private val MUSIC_PACKAGES = setOf(
    "com.spotify.music",
    "com.apple.android.music",
    "com.google.android.apps.youtube.music",
    "com.pandora.android",
    "com.tunein.radio",
    "deezer.android.app",
    "com.amazon.mp3",
    "com.soundcloud.android",
    "com.tidal.wave"
)

/** Detect category from package name and Android [ApplicationInfo.category]. */
fun detectCategory(
    packageName: String,
    androidCategory: Int // ApplicationInfo.category
): AppCategory {
    if (packageName in STREAMING_PACKAGES) return AppCategory.STREAMING
    if (packageName in MUSIC_PACKAGES)    return AppCategory.MUSIC
    // Android API 26+ CATEGORY_GAME = 0
    if (androidCategory == 0 /* ApplicationInfo.CATEGORY_GAME */) return AppCategory.GAME
    // Heuristic for tools
    return AppCategory.OTHER
}
