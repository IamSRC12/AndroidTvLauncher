package com.antigravity.tvlauncher.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.antigravity.tvlauncher.data.*
import com.antigravity.tvlauncher.ui.components.*
import com.antigravity.tvlauncher.ui.theme.*
import com.antigravity.tvlauncher.ui.viewmodel.HomeViewModel
import com.antigravity.tvlauncher.util.BluetoothHelper

/**
 * Root home screen composable.
 *
 * Layout (top → bottom):
 *   TopBar  (clock | status | profile | search | settings)
 *   Hero Spotlight carousel
 *   App Rows   (Continue Watching, Favorites, Most Used, Streaming, Games, New Apps, Custom)
 *   App Grid   (All Apps with filter tabs)
 *
 * All overlays (search, context menu, settings, profiles, screensaver, stats, BT, wallpaper,
 * key mapper) are stacked on top via AnimatedVisibility.
 *
 * Renaming dialog uses an inline Box overlay with a simple text field.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    bluetoothHelper: BluetoothHelper,
    onUninstallRequest: (Intent) -> Unit
) {
    val ctx = LocalContext.current

    // ── Collect all state from ViewModel ─────────────────────────────────────
    val allApps              by viewModel.allApps.collectAsState()
    val visibleApps          by viewModel.visibleApps.collectAsState()
    val favoriteApps         by viewModel.favoriteApps.collectAsState()
    val recommendedApps      by viewModel.recommendedApps.collectAsState()
    val mostUsedApps         by viewModel.mostUsedApps.collectAsState()
    val recentlyInstalled    by viewModel.recentlyInstalledApps.collectAsState()
    val streamingApps        by viewModel.streamingApps.collectAsState()
    val gameApps             by viewModel.gameApps.collectAsState()
    val heroApps             by viewModel.heroApps.collectAsState()
    val rowConfigs           by viewModel.rowConfigs.collectAsState()
    val themePreset          by viewModel.themePreset.collectAsState()
    val clockIs24h           by viewModel.clockIs24h.collectAsState()
    val screensaverMins      by viewModel.screensaverMins.collectAsState()
    val gridColumns          by viewModel.gridColumns.collectAsState()
    val wallpaper            by viewModel.wallpaper.collectAsState()
    val keyMappings          by viewModel.keyMappings.collectAsState()
    val profiles             by viewModel.profiles.collectAsState()
    val activeProfileId      by viewModel.activeProfileId.collectAsState()
    val activeProfile        by viewModel.activeProfile.collectAsState()

    // ── Kids mode filtering ───────────────────────────────────────────────────
    val displayApps = remember(visibleApps, activeProfile) {
        if (activeProfile.isKidsMode)
            visibleApps.filter { it.category != AppCategory.GAME && it.category != AppCategory.OTHER }
        else visibleApps
    }

    // ── Resolve app list for each row ─────────────────────────────────────────
    fun appsForRow(config: RowConfig): List<AppInfo> = when (config.type) {
        RowType.RECOMMENDED        -> recommendedApps
        RowType.FAVORITES          -> favoriteApps
        RowType.MOST_USED          -> mostUsedApps
        RowType.RECENTLY_INSTALLED -> recentlyInstalled
        RowType.STREAMING          -> streamingApps.filter { !activeProfile.isKidsMode || it.category != AppCategory.GAME }
        RowType.GAMES              -> if (activeProfile.isKidsMode) emptyList() else gameApps
        RowType.CUSTOM             -> {
            val pkgSet = config.customAppPackages.toSet()
            visibleApps.filter { it.packageName in pkgSet }
        }
    }

    // Inline rename state
    var renamingApp     by remember { mutableStateOf<AppInfo?>(null) }
    var renameTextValue by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Wallpaper background ──────────────────────────────────────────────
        WallpaperBackground(wallpaper = wallpaper)

        // Kids mode top banner
        if (activeProfile.isKidsMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x99FFD600))
                    .padding(4.dp)
                    .align(Alignment.TopCenter)
            ) {
                Text(
                    text  = "🧒 Kids Mode Active",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // ── Main scrollable content ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 48.dp, top = 28.dp, bottom = 16.dp)
        ) {
            var connectedBtName by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                while (true) {
                    try {
                        connectedBtName = bluetoothHelper.getConnectedDeviceName()
                    } catch (_: Exception) {}
                    kotlinx.coroutines.delay(10_000L)
                }
            }

            // Top bar
            TopBar(
                is24h                 = clockIs24h,
                activeProfile         = activeProfile,
                connectedBtDeviceName = connectedBtName,
                onSearchClick         = { viewModel.showSearch = true },
                onProfileClick        = { viewModel.showProfiles = true },
                onBluetoothClick      = {
                    try {
                        ctx.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    } catch (_: Exception) {
                        viewModel.showBluetooth = true
                    }
                },
                onInputsClick         = { viewModel.showInputs = true },
                onSettingsClick       = {
                    try {
                        ctx.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    } catch (_: Exception) {}
                }
            )

            Spacer(Modifier.height(20.dp))

            // Main scrollable area
            TvLazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding      = PaddingValues(bottom = 32.dp)
            ) {
                // Hero section (only shown when there are apps to feature)
                if (heroApps.isNotEmpty()) {
                    item {
                        HeroSection(
                            apps        = heroApps,
                            onLaunch    = { app -> viewModel.launchApp(app.packageName) },
                            onLongPress = { app -> viewModel.openContextMenu(app) }
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Rows — only visible rows that have at least one app
                items(rowConfigs.filter { it.isVisible }) { rowConfig ->
                    val apps = appsForRow(rowConfig)
                    AppRow(
                        config          = rowConfig,
                        apps            = apps,
                        onAppClick      = { app -> viewModel.launchApp(app.packageName) },
                        onAppLongPress  = { app -> viewModel.openContextMenu(app) },
                        onToggleCollapse= { viewModel.toggleRowCollapsed(rowConfig.id) },
                        onAddAppClick   = if (rowConfig.type == RowType.FAVORITES) { { viewModel.showFavPicker = true } } else null,
                        showLaunchCount = rowConfig.type == RowType.MOST_USED
                    )
                }

                // All Apps grid at the bottom
                item {
                    Spacer(Modifier.height(16.dp))
                    AppGrid(
                        apps           = displayApps,
                        columns        = gridColumns,
                        onAppClick     = { app -> viewModel.launchApp(app.packageName) },
                        onAppLongPress = { app -> viewModel.openContextMenu(app) }
                    )
                }
            }
        }

        // ── Overlays (all stacked on top of main content) ─────────────────────

        // Context menu
        AnimatedVisibility(
            visible = viewModel.showContextMenu && viewModel.contextMenuApp != null,
            enter   = fadeIn(tween(150)),
            exit    = fadeOut(tween(120))
        ) {
            viewModel.contextMenuApp?.let { app ->
                ContextMenuOverlay(
                    app             = app,
                    onDismiss       = { viewModel.dismissContextMenu() },
                    onLaunch        = { viewModel.launchApp(app.packageName) },
                    onToggleFavorite= { viewModel.toggleFavorite(app.packageName) },
                    onToggleHidden  = { viewModel.toggleHidden(app.packageName) },
                    onRename        = {
                        renamingApp    = app
                        renameTextValue = app.displayLabel
                        viewModel.dismissContextMenu()
                    },
                    onAppInfo       = {
                        try {
                            ctx.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.parse("package:${app.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        } catch (_: Exception) {}
                    },
                    onUninstall     = {
                        onUninstallRequest(viewModel.uninstallIntent(app.packageName))
                    }
                )
            }
        }

        // Search overlay
        AnimatedVisibility(
            visible = viewModel.showSearch,
            enter   = fadeIn() + scaleIn(initialScale = 0.97f),
            exit    = fadeOut() + scaleOut(targetScale = 0.97f)
        ) {
            SearchOverlay(
                apps       = displayApps,
                onLaunch   = { pkg -> viewModel.showSearch = false; viewModel.launchApp(pkg) },
                onDismiss  = { viewModel.showSearch = false }
            )
        }

        // Settings
        AnimatedVisibility(
            visible = viewModel.showSettings,
            enter   = slideInHorizontally(tween(300)) { it / 2 } + fadeIn(tween(300)),
            exit    = slideOutHorizontally(tween(250)) { it / 2 } + fadeOut(tween(250))
        ) {
            SettingsScreen(
                currentPreset      = themePreset,
                clockIs24h         = clockIs24h,
                screensaverMins    = screensaverMins,
                gridColumns        = gridColumns,
                rowConfigs         = rowConfigs,
                allApps            = allApps,
                onSetPreset        = { viewModel.setThemePreset(it) },
                onSetAccentColor   = { viewModel.setAccentColor(it) },
                onToggleClock      = { viewModel.toggleClockFormat() },
                onSetScreensaverMins = { viewModel.setScreensaverMins(it) },
                onSetGridColumns   = { viewModel.setGridColumns(it) },
                onSetRowVisible    = { id, v -> viewModel.setRowVisible(id, v) },
                onToggleRowCollapsed = { viewModel.toggleRowCollapsed(it) },
                onResetUsage       = { viewModel.resetUsageData() },
                onDismiss          = { viewModel.showSettings = false }
            )
        }

        // Profile switcher
        AnimatedVisibility(
            visible = viewModel.showProfiles,
            enter   = fadeIn() + scaleIn(initialScale = 0.95f),
            exit    = fadeOut() + scaleOut(targetScale = 0.95f)
        ) {
            ProfileSwitcher(
                profiles        = profiles,
                activeProfileId = activeProfileId,
                onSwitchProfile = { viewModel.switchProfile(it) },
                onDismiss       = { viewModel.showProfiles = false }
            )
        }

        // Usage stats
        AnimatedVisibility(
            visible = viewModel.showStats,
            enter   = fadeIn() + scaleIn(initialScale = 0.95f),
            exit    = fadeOut() + scaleOut(targetScale = 0.95f)
        ) {
            UsageStatsOverlay(
                allApps      = allApps,
                onResetUsage = { viewModel.resetUsageData() },
                onDismiss    = { viewModel.showStats = false }
            )
        }

        // Bluetooth
        AnimatedVisibility(
            visible = viewModel.showBluetooth,
            enter   = fadeIn() + scaleIn(initialScale = 0.95f),
            exit    = fadeOut() + scaleOut(targetScale = 0.95f)
        ) {
            BluetoothOverlay(
                btHelper  = bluetoothHelper,
                onDismiss = { viewModel.showBluetooth = false }
            )
        }

        // HDMI / TV Inputs Overlay
        AnimatedVisibility(
            visible = viewModel.showInputs,
            enter   = fadeIn() + scaleIn(initialScale = 0.95f),
            exit    = fadeOut() + scaleOut(targetScale = 0.95f)
        ) {
            InputsOverlay(
                onDismiss = { viewModel.showInputs = false }
            )
        }

        // Favorites Picker Overlay
        AnimatedVisibility(
            visible = viewModel.showFavPicker,
            enter   = fadeIn() + scaleIn(initialScale = 0.95f),
            exit    = fadeOut() + scaleOut(targetScale = 0.95f)
        ) {
            FavPickerOverlay(
                allApps          = allApps,
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onDismiss        = { viewModel.showFavPicker = false }
            )
        }

        // Wallpaper picker
        AnimatedVisibility(
            visible = viewModel.showWallpaper,
            enter   = fadeIn() + scaleIn(initialScale = 0.95f),
            exit    = fadeOut() + scaleOut(targetScale = 0.95f)
        ) {
            WallpaperPicker(
                onSelected = { value -> viewModel.setWallpaper(value); viewModel.showWallpaper = false },
                onDismiss  = { viewModel.showWallpaper = false }
            )
        }

        // Key mapper
        AnimatedVisibility(
            visible = viewModel.showKeyMapper,
            enter   = fadeIn() + scaleIn(initialScale = 0.95f),
            exit    = fadeOut() + scaleOut(targetScale = 0.95f)
        ) {
            KeyMapperDialog(
                existingMappings = keyMappings,
                apps             = allApps.filter { !it.isHidden },
                onSave           = { code, action -> viewModel.saveKeyMapping(code, action) },
                onDelete         = { code -> viewModel.deleteKeyMapping(code) },
                onDismiss        = { viewModel.showKeyMapper = false }
            )
        }

        // Screensaver (topmost — covers everything)
        AnimatedVisibility(
            visible = viewModel.screenSaverActive,
            enter   = fadeIn(tween(500)),
            exit    = fadeOut(tween(300))
        ) {
            ScreensaverScreen(
                onDismiss = { viewModel.dismissScreensaver() }
            )
        }

        // Inline rename dialog
        renamingApp?.let { app ->
            RenameDialog(
                app         = app,
                initialText = renameTextValue,
                onConfirm   = { newName ->
                    viewModel.setCustomName(app.packageName, newName.ifBlank { null })
                    renamingApp = null
                },
                onDismiss   = { renamingApp = null }
            )
        }
    }
}

// ── Wallpaper background ──────────────────────────────────────────────────────
@Composable
private fun WallpaperBackground(wallpaper: String) {
    val bgColor = LocalBgColor.current
    if (wallpaper.startsWith("#")) {
        val base = try {
            Color(android.graphics.Color.parseColor(wallpaper))
        } catch (_: Exception) { bgColor }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(base, Color.Black)))
        )
    } else {
        Image(
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(wallpaper)
                    .crossfade(600)
                    .build()
            ),
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            alpha              = 0.35f,
            modifier           = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xCC000000), Color(0x88000000), Color(0xCC000000))
                    )
                )
        )
    }
}

// ── Simple inline rename dialog ────────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RenameDialog(
    app: AppInfo,
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .onKeyEvent { ev ->
                when {
                    ev.key == Key.Escape && ev.type == KeyEventType.KeyUp -> { onDismiss(); true }
                    ev.key == Key.Enter  && ev.type == KeyEventType.KeyUp -> { onConfirm(text); true }
                    else -> false
                }
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .background(Color(0xFF1C1C1C), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .padding(24.dp)
                .width(360.dp)
        ) {
            Text("Rename App", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(
                text  = "Current: ${app.label}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            androidx.compose.material3.OutlinedTextField(
                value          = text,
                onValueChange  = { text = it },
                label          = { androidx.compose.material3.Text("Display name (blank to reset)") },
                singleLine     = true,
                colors         = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = LocalAccentColor.current,
                    unfocusedBorderColor = Color(0xFF555555),
                    focusedTextColor     = TextPrimary,
                    unfocusedTextColor   = TextPrimary,
                    cursorColor          = LocalAccentColor.current
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text  = "Press Enter to confirm • Escape to cancel",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}
