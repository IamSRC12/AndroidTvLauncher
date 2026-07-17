package com.antigravity.tvlauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_prefs")

class LauncherDataStore(private val context: Context) {

    companion object {
        val KEY_FAVORITES    = stringSetPreferencesKey("favorites")
        val KEY_HIDDEN       = stringSetPreferencesKey("hidden")
        val KEY_WALLPAPER    = stringPreferencesKey("wallpaper")
        val KEY_COLUMNS      = intPreferencesKey("columns_count")
        private const val KEY_MAPPING_PREFIX = "remote_key_"
    }

    val favoritesFlow: Flow<Set<String>> = context.dataStore.data
        .map { it[KEY_FAVORITES] ?: emptySet() }

    val hiddenFlow: Flow<Set<String>> = context.dataStore.data
        .map { it[KEY_HIDDEN] ?: emptySet() }

    val wallpaperFlow: Flow<String> = context.dataStore.data
        .map { it[KEY_WALLPAPER] ?: "#000000" }

    val columnsFlow: Flow<Int> = context.dataStore.data
        .map { it[KEY_COLUMNS] ?: 5 }

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
}
