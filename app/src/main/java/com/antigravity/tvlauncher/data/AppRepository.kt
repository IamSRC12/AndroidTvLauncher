package com.antigravity.tvlauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false
)

class AppRepository(
    private val context: Context,
    private val dataStore: LauncherDataStore
) {
    private val pm: PackageManager = context.packageManager

    private fun queryApps(): List<AppInfo> {
        val seen   = mutableSetOf<String>()
        val result = mutableListOf<AppInfo>()

        val intents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        )

        for (intent in intents) {
            for (ri in pm.queryIntentActivities(intent, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg == context.packageName || seen.contains(pkg)) continue
                seen.add(pkg)
                result.add(
                    AppInfo(
                        packageName = pkg,
                        label       = ri.loadLabel(pm).toString(),
                        icon        = ri.loadIcon(pm)
                    )
                )
            }
        }
        return result.sortedBy { it.label.lowercase() }
    }

    fun getAppsFlow(): Flow<List<AppInfo>> =
        combine(dataStore.favoritesFlow, dataStore.hiddenFlow) { favs, hidden ->
            queryApps().map { app ->
                app.copy(
                    isFavorite = favs.contains(app.packageName),
                    isHidden   = hidden.contains(app.packageName)
                )
            }
        }

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = pm.getLaunchIntentForPackage(packageName)
                ?: pm.queryIntentActivities(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0
                ).firstOrNull { it.activityInfo.packageName == packageName }
                    ?.let {
                        Intent(Intent.ACTION_MAIN).apply {
                            setClassName(it.activityInfo.packageName, it.activityInfo.name)
                            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                        }
                    }
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
                true
            } ?: false
        } catch (e: Exception) { false }
    }

    fun uninstallIntent(packageName: String): Intent =
        Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    suspend fun toggleFavorite(packageName: String) = dataStore.toggleFavorite(packageName)
    suspend fun toggleHidden(packageName: String)   = dataStore.toggleHidden(packageName)
}
