package com.antigravity.tvlauncher.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.tvlauncher.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val dataStore = LauncherDataStore(application)
    private val repo = AppRepository(application, dataStore)

    // ── All apps — single combined flow of REAL PackageManager data ───────────
    val allApps: StateFlow<List<AppInfo>> = repo.getAppsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val visibleApps: StateFlow<List<AppInfo>> = allApps
        .map { it.filter { a -> !a.isHidden } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val favoriteApps: StateFlow<List<AppInfo>> = allApps
        .map { it.filter { a -> a.isFavorite && !a.isHidden } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // ── Row-specific filtered / sorted app lists ──────────────────────────────

    /** Recommended Apps row — streaming apps and top apps. */
    val recommendedApps: StateFlow<List<AppInfo>> = visibleApps
        .map { apps ->
            val streaming = apps.filter { it.category == AppCategory.STREAMING }
            if (streaming.isNotEmpty()) streaming.take(8)
            else apps.take(8)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    /** Top 10 apps by launch count. */
    val mostUsedApps: StateFlow<List<AppInfo>> = visibleApps
        .map { apps ->
            apps.sortedByDescending { it.launchCount }
                .take(10)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    /** Apps installed in the last 7 days. */
    val recentlyInstalledApps: StateFlow<List<AppInfo>> = visibleApps
        .map { apps -> apps.filter { it.isNew } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    /** Streaming apps (auto-detected by category). */
    val streamingApps: StateFlow<List<AppInfo>> = visibleApps
        .map { apps -> apps.filter { it.category == AppCategory.STREAMING } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    /** Game apps (auto-detected by category). */
    val gameApps: StateFlow<List<AppInfo>> = visibleApps
        .map { apps -> apps.filter { it.category == AppCategory.GAME } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    /** Hero section — favorites first, then most-launched, capped at 5. */
    val heroApps: StateFlow<List<AppInfo>> = allApps
        .map { apps ->
            val favs     = apps.filter { it.isFavorite && !it.isHidden }
            val mostUsed = apps.filter { !it.isHidden && !it.isFavorite }
                .sortedByDescending { it.launchCount }
            (favs + mostUsed).take(5)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // ── Row configuration ─────────────────────────────────────────────────────

    val rowConfigs: StateFlow<List<RowConfig>> = dataStore.rowsConfigFlow
        .map { json ->
            if (json.isBlank()) RowConfig.defaults() else json.toRowConfigs()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), RowConfig.defaults())

    // ── Theme ─────────────────────────────────────────────────────────────────

    val themePreset: StateFlow<ThemePreset> = dataStore.themePresetFlow
        .map { ThemePreset.byName(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ThemePreset.byName("Midnight"))

    val accentColorHex: StateFlow<String> = dataStore.accentColorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "#4FC3F7")

    val clockIs24h: StateFlow<Boolean> = dataStore.clock24hFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val screensaverMins: StateFlow<Int> = dataStore.screensaverMinsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 3)

    val gridColumns: StateFlow<Int> = dataStore.columnsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 6)

    val wallpaper: StateFlow<String> = dataStore.wallpaperFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "#000000")

    val keyMappings: StateFlow<Map<Int, String>> = dataStore.allMappingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    // ── Profiles ──────────────────────────────────────────────────────────────

    val profiles: StateFlow<List<Profile>> = dataStore.profilesJsonFlow
        .map { json ->
            if (json.isBlank()) listOf(Profile.default(), Profile.kids())
            else json.toProfiles()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L),
            listOf(Profile.default(), Profile.kids()))

    val activeProfileId: StateFlow<String> = dataStore.activeProfileIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "main")

    val activeProfile: StateFlow<Profile> = combine(profiles, activeProfileId) { profs, id ->
        profs.find { it.id == id } ?: Profile.default()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), Profile.default())

    // ── UI overlay state ──────────────────────────────────────────────────────

    var showSearch      by mutableStateOf(false)
    var showBluetooth   by mutableStateOf(false)
    var showInputs      by mutableStateOf(false)
    var showFavPicker   by mutableStateOf(false)
    var showWallpaper   by mutableStateOf(false)
    var showKeyMapper   by mutableStateOf(false)
    var showSettings    by mutableStateOf(false)
    var showContextMenu by mutableStateOf(false)
    var showProfiles    by mutableStateOf(false)
    var showStats       by mutableStateOf(false)
    var screenSaverActive by mutableStateOf(false)
    var contextMenuApp  by mutableStateOf<AppInfo?>(null)

    // ── App actions ───────────────────────────────────────────────────────────

    /** Launch an app by package name and increment its usage counter. */
    fun launchApp(pkg: String) {
        repo.launchApp(pkg)
        val currentCount = allApps.value.find { it.packageName == pkg }?.launchCount ?: 0
        viewModelScope.launch { repo.trackLaunch(pkg, currentCount) }
    }

    fun uninstallIntent(pkg: String): Intent = repo.uninstallIntent(pkg)
    fun toggleFavorite(pkg: String) { viewModelScope.launch { repo.toggleFavorite(pkg) } }
    fun toggleHidden(pkg: String)   { viewModelScope.launch { repo.toggleHidden(pkg) } }
    fun setCustomName(pkg: String, name: String?) {
        viewModelScope.launch { repo.setCustomName(pkg, name) }
    }

    // ── Theme / appearance actions ────────────────────────────────────────────

    fun setThemePreset(name: String) {
        viewModelScope.launch {
            dataStore.setThemePreset(name)
            // Also update accent to match preset
            dataStore.setAccentColor(ThemePreset.byName(name).accentHex)
        }
    }

    fun setAccentColor(hex: String) {
        viewModelScope.launch { dataStore.setAccentColor(hex) }
    }

    fun toggleClockFormat() {
        viewModelScope.launch { dataStore.setClockFormat(!clockIs24h.value) }
    }

    fun setScreensaverMins(mins: Int) {
        viewModelScope.launch { dataStore.setScreensaverMins(mins) }
    }

    fun setGridColumns(count: Int) {
        viewModelScope.launch { dataStore.setColumns(count) }
    }

    fun setWallpaper(v: String) { viewModelScope.launch { dataStore.setWallpaper(v) } }
    fun resetUsageData() { viewModelScope.launch { dataStore.resetUsage() } }

    // ── Row management actions ────────────────────────────────────────────────

    private fun updateRows(transform: (MutableList<RowConfig>) -> Unit) {
        viewModelScope.launch {
            val current = rowConfigs.value.toMutableList()
            transform(current)
            dataStore.setRowsConfig(current.toJsonString())
        }
    }

    fun setRowVisible(id: String, visible: Boolean) = updateRows { rows ->
        val idx = rows.indexOfFirst { it.id == id }
        if (idx >= 0) rows[idx] = rows[idx].copy(isVisible = visible)
    }

    fun toggleRowCollapsed(id: String) = updateRows { rows ->
        val idx = rows.indexOfFirst { it.id == id }
        if (idx >= 0) rows[idx] = rows[idx].copy(isCollapsed = !rows[idx].isCollapsed)
    }

    fun reorderRow(fromIndex: Int, toIndex: Int) = updateRows { rows ->
        if (fromIndex in rows.indices && toIndex in rows.indices) {
            val item = rows.removeAt(fromIndex)
            rows.add(toIndex, item)
        }
    }

    fun addCustomRow(title: String) = updateRows { rows ->
        rows.add(RowConfig(
            id    = UUID.randomUUID().toString(),
            title = title,
            type  = RowType.CUSTOM
        ))
    }

    fun updateRowTitle(id: String, title: String) = updateRows { rows ->
        val idx = rows.indexOfFirst { it.id == id }
        if (idx >= 0) rows[idx] = rows[idx].copy(title = title)
    }

    fun deleteCustomRow(id: String) = updateRows { rows ->
        rows.removeAll { it.id == id && it.type == RowType.CUSTOM }
    }

    fun addAppToCustomRow(rowId: String, pkg: String) = updateRows { rows ->
        val idx = rows.indexOfFirst { it.id == rowId }
        if (idx >= 0) {
            val row = rows[idx]
            if (!row.customAppPackages.contains(pkg))
                rows[idx] = row.copy(customAppPackages = row.customAppPackages + pkg)
        }
    }

    fun removeAppFromCustomRow(rowId: String, pkg: String) = updateRows { rows ->
        val idx = rows.indexOfFirst { it.id == rowId }
        if (idx >= 0) {
            val row = rows[idx]
            rows[idx] = row.copy(customAppPackages = row.customAppPackages - pkg)
        }
    }

    // ── Profile actions ───────────────────────────────────────────────────────

    private fun updateProfiles(transform: (MutableList<Profile>) -> Unit) {
        viewModelScope.launch {
            val current = profiles.value.toMutableList()
            transform(current)
            dataStore.setProfilesJson(current.toJsonString())
        }
    }

    fun switchProfile(profileId: String) {
        viewModelScope.launch { dataStore.setActiveProfile(profileId) }
    }

    fun addProfile(profile: Profile) = updateProfiles { it.add(profile) }

    fun updateProfile(profile: Profile) = updateProfiles { profiles ->
        val idx = profiles.indexOfFirst { it.id == profile.id }
        if (idx >= 0) profiles[idx] = profile
    }

    fun deleteProfile(profileId: String) = updateProfiles { profiles ->
        // Cannot delete the currently active profile
        if (profileId != activeProfileId.value)
            profiles.removeAll { it.id == profileId }
    }

    // ── Key remapping (preserved from original) ───────────────────────────────

    fun saveKeyMapping(keyCode: Int, action: String) {
        viewModelScope.launch { dataStore.setKeyMapping(keyCode, action) }
    }

    fun deleteKeyMapping(keyCode: Int) {
        viewModelScope.launch { dataStore.removeKeyMapping(keyCode) }
    }

    fun handleRemappedKey(keyCode: Int): Boolean {
        val action = keyMappings.value[keyCode] ?: return false
        return when {
            action.startsWith("launch:")  -> { launchApp(action.removePrefix("launch:")); true }
            action == "action:search"     -> { showSearch    = true; true }
            action == "action:bluetooth"  -> { showBluetooth = true; true }
            action == "action:wallpaper"  -> { showWallpaper = true; true }
            action == "action:settings"   -> { showSettings  = true; true }
            else                          -> false
        }
    }

    // ── Context menu helpers ──────────────────────────────────────────────────

    fun openContextMenu(app: AppInfo) {
        contextMenuApp = app
        showContextMenu = true
    }

    fun dismissContextMenu() {
        showContextMenu = false
        contextMenuApp  = null
    }

    // ── Screensaver ───────────────────────────────────────────────────────────

    fun activateScreensaver() { screenSaverActive = true }
    fun dismissScreensaver()  { screenSaverActive = false }
}
