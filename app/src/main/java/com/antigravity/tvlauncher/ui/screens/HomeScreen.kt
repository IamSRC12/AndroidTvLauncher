package com.antigravity.tvlauncher.ui.screens

import android.content.Intent
import android.content.res.Resources
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.antigravity.tvlauncher.ui.components.*
import com.antigravity.tvlauncher.ui.theme.*
import com.antigravity.tvlauncher.ui.viewmodel.HomeViewModel
import com.antigravity.tvlauncher.util.BluetoothHelper

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    bluetoothHelper: BluetoothHelper,
    onUninstallRequest: (Intent) -> Unit
) {
    val ctx          = LocalContext.current
    val visibleApps  by viewModel.visibleApps.collectAsState()
    val favoriteApps by viewModel.favoriteApps.collectAsState()
    val allApps      by viewModel.allApps.collectAsState()
    val wallpaper    by viewModel.wallpaper.collectAsState()
    val mappings     by viewModel.keyMappings.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Wallpaper background ──────────────────────────────────────────────
        WallpaperBackground(wallpaper = wallpaper)

        // ── Main content ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 48.dp, top = 36.dp, bottom = 24.dp)
        ) {
            // Top status bar
            TopBar(bluetoothHelper = bluetoothHelper)

            Spacer(Modifier.height(20.dp))

            // ── Quick Actions bar (FLauncher style) ─────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.padding(start = 4.dp)
            ) {
                QuickActionChip(
                    icon    = Icons.Default.Search,
                    label   = "Search",
                    onClick = { viewModel.showSearch = true }
                )
                QuickActionChip(
                    icon    = Icons.Default.Bluetooth,
                    label   = "Bluetooth",
                    onClick = { viewModel.showBluetooth = true }
                )
                QuickActionChip(
                    icon    = Icons.Default.Wallpaper,
                    label   = "Wallpaper",
                    onClick = { viewModel.showWallpaper = true }
                )
                QuickActionChip(
                    icon    = Icons.Default.Settings,
                    label   = "Settings",
                    onClick = {
                        try {
                            ctx.startActivity(
                                android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {}
                    }
                )
                QuickActionChip(
                    icon    = Icons.Default.Key,
                    label   = "Key Mapper",
                    onClick = { viewModel.showKeyMapper = true }
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Favourites row ────────────────────────────────────────────────
            if (favoriteApps.isNotEmpty()) {
                SectionHeader(title = "Favourites")
                Spacer(Modifier.height(10.dp))
                TvLazyRow(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding        = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    items(favoriteApps, key = { it.packageName }) { app ->
                        AppCard(
                            app        = app,
                            onClick    = { viewModel.launchApp(app.packageName) },
                            onFavorite = { viewModel.toggleFavorite(app.packageName) },
                            onHide     = { viewModel.toggleHidden(app.packageName) },
                            onUninstall = { onUninstallRequest(viewModel.uninstallIntent(app.packageName)) }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── All Apps row ──────────────────────────────────────────────────
            SectionHeader(
                title    = "All Apps",
                subtitle = if (visibleApps.isNotEmpty()) "${visibleApps.size} apps" else null
            )
            Spacer(Modifier.height(10.dp))

            if (visibleApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text  = "No apps found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                TvLazyRow(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding        = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    items(visibleApps, key = { it.packageName }) { app ->
                        AppCard(
                            app        = app,
                            onClick    = { viewModel.launchApp(app.packageName) },
                            onFavorite = { viewModel.toggleFavorite(app.packageName) },
                            onHide     = { viewModel.toggleHidden(app.packageName) },
                            onUninstall = { onUninstallRequest(viewModel.uninstallIntent(app.packageName)) }
                        )
                    }
                }
            }
        }

        // ── Overlays ──────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = viewModel.showSearch,
            enter   = fadeIn() + scaleIn(initialScale = 0.97f),
            exit    = fadeOut() + scaleOut(targetScale = 0.97f)
        ) {
            SearchOverlay(
                apps       = visibleApps,
                onLaunch   = { pkg ->
                    viewModel.showSearch = false
                    viewModel.launchApp(pkg)
                },
                onFavorite = { viewModel.toggleFavorite(it) },
                onHide     = { viewModel.toggleHidden(it) },
                onUninstall = { pkg ->
                    viewModel.showSearch = false
                    onUninstallRequest(viewModel.uninstallIntent(pkg))
                },
                onDismiss  = { viewModel.showSearch = false }
            )
        }

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

        AnimatedVisibility(
            visible = viewModel.showWallpaper,
            enter   = fadeIn() + scaleIn(initialScale = 0.95f),
            exit    = fadeOut() + scaleOut(targetScale = 0.95f)
        ) {
            WallpaperPicker(
                onSelected = { value ->
                    viewModel.setWallpaper(value)
                    viewModel.showWallpaper = false
                },
                onDismiss = { viewModel.showWallpaper = false }
            )
        }

        AnimatedVisibility(
            visible = viewModel.showKeyMapper,
            enter   = fadeIn() + scaleIn(initialScale = 0.95f),
            exit    = fadeOut() + scaleOut(targetScale = 0.95f)
        ) {
            KeyMapperDialog(
                existingMappings = mappings,
                apps             = allApps.filter { !it.isHidden },
                onSave           = { code, action -> viewModel.saveKeyMapping(code, action) },
                onDelete         = { code -> viewModel.deleteKeyMapping(code) },
                onDismiss        = { viewModel.showKeyMapper = false }
            )
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Text(
            text  = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        if (subtitle != null) {
            Text(
                text     = subtitle,
                style    = MaterialTheme.typography.labelMedium,
                color    = TextSecondary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

// ── Quick-action pill chip ────────────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (focused) Color.White
                else Color(0x33FFFFFF)
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter) && ev.type == KeyEventType.KeyUp) {
                    onClick(); true
                } else false
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (focused) Color.Black else Color.White,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (focused) Color.Black else Color.White
        )
    }
}

// ── Wallpaper background ──────────────────────────────────────────────────────
@Composable
private fun WallpaperBackground(wallpaper: String) {
    if (wallpaper.startsWith("#")) {
        val base = try { Color(android.graphics.Color.parseColor(wallpaper)) } catch (_: Exception) { Black }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(base, Black)))
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
            contentScale = ContentScale.Crop,
            alpha = 0.35f,
            modifier = Modifier.fillMaxSize()
        )
        // Dark overlay gradient so text stays readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xCC000000), Color(0x99000000), Color(0xCC000000))
                    )
                )
        )
    }
}
