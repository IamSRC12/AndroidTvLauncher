package com.antigravity.tvlauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_prefs")

/**
 * Single source of truth for all persisted launcher preferences.
 * Uses Jetpack DataStore (Preferences) for async, coroutine-friendly persistence.
 */
class LauncherDataStore(private val context: Context) {

    companion object {
        // ── Legacy / existing keys (preserved) ────────────────────────────────
        val KEY_FAVORITES      = stringSetPreferencesKey("favorites")
        val KEY_HIDDEN         = stringSetPreferencesKey("hidden")
        val KEY_WALLPAPER      = stringPreferencesKey("wallpaper")
        val KEY_COLUMNS        = intPreferencesKey("columns_count")
        private const val KEY_MAPPING_PREFIX = "remote_key_"

        // ── New keys ───────────────────────────────────────────────────────────
        val KEY_ROWS_CONFIG    = stringPreferencesKey("rows_config")    // JSON array of RowConfig
        val KEY_ACCENT_COLOR   = stringPreferencesKey("accent_color")   // hex string
        val KEY_THEME_PRESET   = stringPreferencesKey("theme_preset")   // preset name
        val KEY_CLOCK_24H      = booleanPreferencesKey("clock_24h")
        val KEY_PROFILES_JSON  = stringPreferencesKey("profiles_json")  // JSON array of Profile
        val KEY_ACTIVE_PROFILE = stringPreferencesKey("active_profile") // profile id
        val KEY_HERO_APPS      = stringPreferencesKey("hero_apps")      // comma-separated pkg list
        val KEY_SCREENSAVER_MINS = intPreferencesKey("screensaver_mins")// idle timeout (default 3)

        // Per-app usage keys use dynamic names (prefixed)
        private const val USAGE_COUNT_PREFIX  = "usage_count_"
        private const val USAGE_LAUNCH_PREFIX = "usage_last_"
        private const val CUSTOM_NAME_PREFIX  = "custom_name_"
    }

    // ── Existing flows (preserved) ────────────────────────────────────────────

    val favoritesFlow: Flow<Set<String>> = context.dataStore.data
        .map { it[KEY_FAVORITES] ?: emptySet() }

    val hiddenFlow: Flow<Set<String>> = context.dataStore.data
        .map { it[KEY_HIDDEN] ?: emptySet() }

    val wallpaperFlow: Flow<String> = context.dataStore.data
        .map { it[KEY_WALLPAPER] ?: "#000000" }

    val columnsFlow: Flow<Int> = context.dataStore.data
        .map { it[KEY_COLUMNS] ?: 6 }

    val allMappingsFlow: Flow<Map<Int, String>> = context.dataStore.data
        .map { preferences ->
            preferences.asMap().entries
                .filter { it.key.name.startsWith(KEY_MAPPING_PREFIX) }
                .mapNotNull { entry ->
                    val keyCode = entry.key.name.removePrefix(KEY_MAPPING_PREFIX).toIntOrNull()
                    val action  = entry.value as? String
                    if (keyCode != null && !action.isNullOrEmpty()) keyCode to action else null
                }
                .toMap()
        }

    // ── New flows ─────────────────────────────────────────────────────────────

    val rowsConfigFlow: Flow<String> = context.dataStore.data
        .map { it[KEY_ROWS_CONFIG] ?: "" }

    val accentColorFlow: Flow<String> = context.dataStore.data
        .map { it[KEY_ACCENT_COLOR] ?: "#4FC3F7" }

    val themePresetFlow: Flow<String> = context.dataStore.data
        .map { it[KEY_THEME_PRESET] ?: "Midnight" }

    val clock24hFlow: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_CLOCK_24H] ?: false }

    val profilesJsonFlow: Flow<String> = context.dataStore.data
        .map { it[KEY_PROFILES_JSON] ?: "" }

    val activeProfileIdFlow: Flow<String> = context.dataStore.data
        .map { it[KEY_ACTIVE_PROFILE] ?: "main" }

    val heroAppsFlow: Flow<String> = context.dataStore.data
        .map { it[KEY_HERO_APPS] ?: "" }

    val screensaverMinsFlow: Flow<Int> = context.dataStore.data
        .map { it[KEY_SCREENSAVER_MINS] ?: 3 }

    /** Reads ALL usage counts as a map of packageName → count. */
    val usageCountsFlow: Flow<Map<String, Int>> = context.dataStore.data
        .map { prefs ->
            prefs.asMap().entries
                .filter { it.key.name.startsWith(USAGE_COUNT_PREFIX) }
                .mapNotNull { entry ->
                    val pkg   = entry.key.name.removePrefix(USAGE_COUNT_PREFIX)
                    val count = entry.value as? Int
                    if (pkg.isNotEmpty() && count != null) pkg to count else null
                }
                .toMap()
        }

    /** Reads ALL last-launched timestamps as a map of packageName → epoch millis. */
    val lastLaunchedFlow: Flow<Map<String, Long>> = context.dataStore.data
        .map { prefs ->
            prefs.asMap().entries
                .filter { it.key.name.startsWith(USAGE_LAUNCH_PREFIX) }
                .mapNotNull { entry ->
                    val pkg  = entry.key.name.removePrefix(USAGE_LAUNCH_PREFIX)
                    val time = entry.value as? Long
                    if (pkg.isNotEmpty() && time != null) pkg to time else null
                }
                .toMap()
        }

    /** Reads ALL custom app name overrides as a map of packageName → label. */
    val customNamesFlow: Flow<Map<String, String>> = context.dataStore.data
        .map { prefs ->
            prefs.asMap().entries
                .filter { it.key.name.startsWith(CUSTOM_NAME_PREFIX) }
                .mapNotNull { entry ->
                    val pkg  = entry.key.name.removePrefix(CUSTOM_NAME_PREFIX)
                    val name = entry.value as? String
                    if (pkg.isNotEmpty() && !name.isNullOrEmpty()) pkg to name else null
                }
                .toMap()
        }

    // ── Existing write methods (preserved) ────────────────────────────────────

    suspend fun toggleFavorite(packageName: String) {
        context.dataStore.edit { prefs ->
            val cur = prefs[KEY_FAVORITES] ?: emptySet()
            prefs[KEY_FAVORITES] = if (cur.contains(packageName)) cur - packageName else cur + packageName
        }
    }

    suspend fun toggleHidden(packageName: String) {
        context.dataStore.edit { prefs ->
            val cur = prefs[KEY_HIDDEN] ?: emptySet()
            prefs[KEY_HIDDEN] = if (cur.contains(packageName)) cur - packageName else cur + packageName
        }
    }

    suspend fun setWallpaper(value: String) {
        context.dataStore.edit { it[KEY_WALLPAPER] = value }
    }

    suspend fun setColumns(count: Int) {
        context.dataStore.edit { it[KEY_COLUMNS] = count }
    }

    suspend fun setKeyMapping(keyCode: Int, action: String) {
        context.dataStore.edit {
            it[stringPreferencesKey("$KEY_MAPPING_PREFIX$keyCode")] = action
        }
    }

    suspend fun removeKeyMapping(keyCode: Int) {
        context.dataStore.edit {
            it.remove(stringPreferencesKey("$KEY_MAPPING_PREFIX$keyCode"))
        }
    }

    // ── New write methods ─────────────────────────────────────────────────────

    suspend fun setRowsConfig(json: String) {
        context.dataStore.edit { it[KEY_ROWS_CONFIG] = json }
    }

    suspend fun setAccentColor(hex: String) {
        context.dataStore.edit { it[KEY_ACCENT_COLOR] = hex }
    }

    suspend fun setThemePreset(name: String) {
        context.dataStore.edit { it[KEY_THEME_PRESET] = name }
    }

    suspend fun setClockFormat(is24h: Boolean) {
        context.dataStore.edit { it[KEY_CLOCK_24H] = is24h }
    }

    suspend fun setProfilesJson(json: String) {
        context.dataStore.edit { it[KEY_PROFILES_JSON] = json }
    }

    suspend fun setActiveProfile(profileId: String) {
        context.dataStore.edit { it[KEY_ACTIVE_PROFILE] = profileId }
    }

    suspend fun setHeroApps(packages: List<String>) {
        context.dataStore.edit { it[KEY_HERO_APPS] = packages.joinToString(",") }
    }

    suspend fun setScreensaverMins(mins: Int) {
        context.dataStore.edit { it[KEY_SCREENSAVER_MINS] = mins }
    }

    /** Increment launch count and record last-launched timestamp for one app. */
    suspend fun trackLaunch(packageName: String, currentCount: Int) {
        context.dataStore.edit { prefs ->
            prefs[intPreferencesKey("$USAGE_COUNT_PREFIX$packageName")] = currentCount + 1
            prefs[longPreferencesKey("$USAGE_LAUNCH_PREFIX$packageName")] = System.currentTimeMillis()
        }
    }

    /** Clear all usage counts and timestamps. */
    suspend fun resetUsage() {
        context.dataStore.edit { prefs ->
            val keysToRemove = prefs.asMap().keys.filter {
                it.name.startsWith(USAGE_COUNT_PREFIX) || it.name.startsWith(USAGE_LAUNCH_PREFIX)
            }
            keysToRemove.forEach { prefs.remove(it) }
        }
    }

    /** Set or clear a custom display name override for one app. */
    suspend fun setCustomName(packageName: String, name: String?) {
        context.dataStore.edit { prefs ->
            val key = stringPreferencesKey("$CUSTOM_NAME_PREFIX$packageName")
            if (name.isNullOrEmpty()) prefs.remove(key) else prefs[key] = name
        }
    }
}
