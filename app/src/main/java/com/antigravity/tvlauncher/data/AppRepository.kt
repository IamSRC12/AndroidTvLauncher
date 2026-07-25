package com.antigravity.tvlauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/**
 * Queries the system PackageManager for installed apps and merges in persisted
 * metadata (favorites, hidden, usage counts, custom names) from DataStore.
 *
 * Performance-optimized:
 * - Caches base PackageManager query results to avoid blocking IPC calls on flow emits.
 * - Hardware-friendly memory management to prevent OOM and lag.
 */
class AppRepository(
    private val context: Context,
    private val dataStore: LauncherDataStore
) {
    private val pm: PackageManager = context.packageManager
    @Volatile
    private var cachedBaseApps: List<AppInfo>? = null

    // ── App querying ──────────────────────────────────────────────────────────

    /**
     * Queries PackageManager for all launchable apps.
     * Caches the list in memory. Call [refreshApps] when package changes occur.
     */
    private suspend fun queryAppsCached(): List<AppInfo> = withContext(Dispatchers.IO) {
        cachedBaseApps?.let { return@withContext it }

        val seen   = mutableSetOf<String>()
        val result = mutableListOf<AppInfo>()
        val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000

        val intents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        )

        for (intent in intents) {
            @Suppress("DEPRECATION")
            val activities = try { pm.queryIntentActivities(intent, 0) } catch (_: Exception) { emptyList() }
            for (ri in activities) {
                val pkg = ri.activityInfo.packageName
                if (pkg == context.packageName || seen.contains(pkg)) continue
                seen.add(pkg)

                val appInfo = try { pm.getApplicationInfo(pkg, 0) } catch (_: Exception) { null }
                val installTime = try {
                    pm.getPackageInfo(pkg, 0).firstInstallTime
                } catch (_: Exception) { 0L }

                val category = detectCategory(
                    packageName     = pkg,
                    androidCategory = appInfo?.category ?: ApplicationInfo.CATEGORY_UNDEFINED
                )

                result.add(
                    AppInfo(
                        packageName = pkg,
                        label       = try { ri.loadLabel(pm).toString() } catch (_: Exception) { pkg },
                        icon        = try { ri.loadIcon(pm) } catch (_: Exception) { pm.defaultActivityIcon },
                        installTime = installTime,
                        isNew       = installTime > sevenDaysAgo,
                        category    = category
                    )
                )
            }
        }
        val sorted = result.sortedBy { it.label.lowercase() }
        cachedBaseApps = sorted
        sorted
    }

    fun refreshApps() {
        cachedBaseApps = null
    }

    // ── Reactive flows ────────────────────────────────────────────────────────

    /**
     * Emits a fully-annotated list of all apps whenever favorites, hidden status,
     * usage counts, or custom names change in DataStore.
     */
    fun getAppsFlow(): Flow<List<AppInfo>> =
        combine(
            dataStore.favoritesFlow,
            dataStore.hiddenFlow,
            dataStore.usageCountsFlow,
            dataStore.lastLaunchedFlow,
            dataStore.customNamesFlow
        ) { favs, hidden, counts, lastLaunched, customNames ->
            val all = queryAppsCached()
            val effectiveFavs = if (favs.isNotEmpty()) favs else {
                val popularPkgs = setOf(
                    "com.google.android.youtube.tv",
                    "com.google.android.youtube",
                    "com.netflix.ninja",
                    "com.netflix.mediaclient",
                    "com.amazon.amazonvideo.livingroom",
                    "com.disney.disneyplus",
                    "com.plexapp.android",
                    "com.spotify.tv.android"
                )
                val matchedPopular = all.filter { popularPkgs.contains(it.packageName) }.map { it.packageName }.toSet()
                if (matchedPopular.isNotEmpty()) matchedPopular
                else all.take(5).map { it.packageName }.toSet()
            }

            try {
                all.map { app ->
                    app.copy(
                        isFavorite   = effectiveFavs.contains(app.packageName),
                        isHidden     = hidden.contains(app.packageName),
                        launchCount  = counts[app.packageName] ?: 0,
                        lastLaunched = lastLaunched[app.packageName] ?: 0L,
                        customLabel  = customNames[app.packageName]
                    )
                }
            } catch (_: Exception) {
                all
            }
        }.catch { emit(emptyList()) }

    // ── App actions ───────────────────────────────────────────────────────────

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
        } catch (_: Exception) { false }
    }

    fun uninstallIntent(packageName: String): Intent =
        Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    // ── DataStore delegation ──────────────────────────────────────────────────

    suspend fun toggleFavorite(packageName: String) = dataStore.toggleFavorite(packageName)
    suspend fun toggleHidden(packageName: String)   = dataStore.toggleHidden(packageName)

    suspend fun trackLaunch(packageName: String, currentCount: Int) =
        dataStore.trackLaunch(packageName, currentCount)

    suspend fun setCustomName(packageName: String, name: String?) =
        dataStore.setCustomName(packageName, name)
}
