package com.antigravity.tvlauncher.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.tvlauncher.data.AppInfo
import com.antigravity.tvlauncher.data.AppRepository
import com.antigravity.tvlauncher.data.LauncherDataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val dataStore  = LauncherDataStore(application)
    private val repo = AppRepository(application, dataStore)

    // All apps including hidden/fav flags
    val allApps: StateFlow<List<AppInfo>> = repo.getAppsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val visibleApps: StateFlow<List<AppInfo>> = allApps
        .map { it.filter { a -> !a.isHidden } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val favoriteApps: StateFlow<List<AppInfo>> = allApps
        .map { it.filter { a -> a.isFavorite && !a.isHidden } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val wallpaper: StateFlow<String> = dataStore.wallpaperFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "#000000")

    val keyMappings: StateFlow<Map<Int, String>> = dataStore.allMappingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    // ── UI overlay state ──────────────────────────────────────────────────────
    var showSearch    by mutableStateOf(false)
    var showBluetooth by mutableStateOf(false)
    var showWallpaper by mutableStateOf(false)
    var showKeyMapper by mutableStateOf(false)

    fun launchApp(pkg: String) = repo.launchApp(pkg)
    fun uninstallIntent(pkg: String) = repo.uninstallIntent(pkg)

    fun toggleFavorite(pkg: String) { viewModelScope.launch { repo.toggleFavorite(pkg) } }
    fun toggleHidden(pkg: String)   { viewModelScope.launch { repo.toggleHidden(pkg) } }
    fun setWallpaper(v: String)     { viewModelScope.launch { dataStore.setWallpaper(v) } }

    fun saveKeyMapping(keyCode: Int, action: String) {
        viewModelScope.launch { dataStore.setKeyMapping(keyCode, action) }
    }
    fun deleteKeyMapping(keyCode: Int) {
        viewModelScope.launch { dataStore.removeKeyMapping(keyCode) }
    }

    fun handleRemappedKey(keyCode: Int): Boolean {
        val action = keyMappings.value[keyCode] ?: return false
        return when {
            action.startsWith("launch:")    -> { launchApp(action.removePrefix("launch:")); true }
            action == "action:search"       -> { showSearch    = true;  true }
            action == "action:bluetooth"    -> { showBluetooth = true;  true }
            action == "action:wallpaper"    -> { showWallpaper = true;  true }
            action == "action:settings"     -> { openSystemSettings(); true }
            else                            -> false
        }
    }

    private fun openSystemSettings() {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(intent)
        } catch (_: Exception) {}
    }
}
