package com.antigravity.tvlauncher.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import com.antigravity.tvlauncher.data.AppInfo
import com.antigravity.tvlauncher.data.RowConfig
import com.antigravity.tvlauncher.data.ThemePreset
import com.antigravity.tvlauncher.ui.theme.*

private enum class SettingsCategory(val label: String, val icon: ImageVector) {
    APPEARANCE("Appearance",    Icons.Default.Palette),
    LAYOUT    ("Layout",        Icons.Default.Dashboard),
    BEHAVIOR  ("Behavior",      Icons.Default.Tune),
    PROFILES  ("Profiles",      Icons.Default.AccountCircle),
    APPS      ("Apps",          Icons.Default.Apps),
    BACKUP    ("Backup",        Icons.Default.SaveAlt),
    ABOUT     ("About",         Icons.Default.Info),
}

/**
 * Full-screen settings panel that slides in from the right.
 * Split-panel layout: categories on the left, settings on the right.
 * All setting changes are applied immediately via ViewModel callbacks.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentPreset: ThemePreset,
    clockIs24h: Boolean,
    screensaverMins: Int,
    gridColumns: Int,
    rowConfigs: List<RowConfig>,
    allApps: List<AppInfo>,
    onSetPreset: (String) -> Unit,
    onSetAccentColor: (String) -> Unit,
    onToggleClock: () -> Unit,
    onSetScreensaverMins: (Int) -> Unit,
    onSetGridColumns: (Int) -> Unit,
    onSetRowVisible: (String, Boolean) -> Unit,
    onToggleRowCollapsed: (String) -> Unit,
    onResetUsage: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(SettingsCategory.APPEARANCE) }
    // Tracks which panel has focus: true = left (categories), false = right (content)
    var leftPanelFocused by remember { mutableStateOf(true) }
    val accentColor = LocalAccentColor.current
    val ctx = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xDD000000))
            .onKeyEvent { ev ->
                if ((ev.key == Key.Escape || ev.key == Key.Back) && ev.type == KeyEventType.KeyUp) {
                    onDismiss(); true
                } else false
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            // ── Left panel: Category list ─────────────────────────────────
            TvLazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF111111))
            ) {
                item {
                    Text(
                        text       = "NovaTV Settings",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = accentColor,
                        modifier   = Modifier.padding(bottom = 16.dp, start = 8.dp)
                    )
                }
                items(SettingsCategory.entries) { cat ->
                    CategoryItem(
                        category    = cat,
                        isSelected  = selectedCategory == cat,
                        accentColor = accentColor,
                        onClick     = {
                            selectedCategory   = cat
                            leftPanelFocused   = false
                        }
                    )
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    // Android TV System Settings button
                    CategoryItem(
                        icon        = Icons.Default.Settings,
                        label       = "Device Settings",
                        isSelected  = false,
                        accentColor = accentColor,
                        onClick     = {
                            try {
                                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                ctx.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    // Close button
                    CategoryItem(
                        icon        = Icons.Default.Close,
                        label       = "Close",
                        isSelected  = false,
                        accentColor = Color(0xFFEF5350),
                        onClick     = onDismiss
                    )
                }
            }

            // ── Right panel: Settings content ─────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF191919))
            ) {
                when (selectedCategory) {
                    SettingsCategory.APPEARANCE -> AppearancePanel(
                        currentPreset    = currentPreset,
                        accentColor      = accentColor,
                        onSetPreset      = onSetPreset,
                        onSetAccentColor = onSetAccentColor
                    )
                    SettingsCategory.LAYOUT -> LayoutPanel(
                        rowConfigs         = rowConfigs,
                        gridColumns        = gridColumns,
                        onSetRowVisible    = onSetRowVisible,
                        onToggleCollapse   = onToggleRowCollapsed,
                        onSetGridColumns   = onSetGridColumns
                    )
                    SettingsCategory.BEHAVIOR -> BehaviorPanel(
                        clockIs24h         = clockIs24h,
                        screensaverMins    = screensaverMins,
                        onToggleClock      = onToggleClock,
                        onSetScreensaver   = onSetScreensaverMins
                    )
                    SettingsCategory.PROFILES -> ProfilesPanel()
                    SettingsCategory.APPS -> AppsPanel(
                        allApps       = allApps,
                        onResetUsage  = onResetUsage
                    )
                    SettingsCategory.BACKUP -> BackupPanel()
                    SettingsCategory.ABOUT -> AboutPanel()
                }
            }
        }
    }
}

// ── Category nav item ──────────────────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryItem(
    category: SettingsCategory? = null,
    icon: ImageVector? = null,
    label: String? = null,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val resolvedIcon  = category?.icon  ?: icon!!
    val resolvedLabel = category?.label ?: label!!

    var focused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> accentColor.copy(alpha = 0.2f)
                    focused    -> Color(0x22FFFFFF)
                    else       -> Color.Transparent
                }
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                    && ev.type == KeyEventType.KeyUp
                ) { onClick(); true } else false
            }
            .padding(12.dp)
    ) {
        Icon(
            imageVector        = resolvedIcon,
            contentDescription = resolvedLabel,
            tint               = if (isSelected || focused) accentColor else TextSecondary,
            modifier           = Modifier.size(20.dp)
        )
        Text(
            text  = resolvedLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected || focused) accentColor else TextSecondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (isSelected) {
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(accentColor, CircleShape)
            )
        }
    }
}

// ── APPEARANCE PANEL ───────────────────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppearancePanel(
    currentPreset: ThemePreset,
    accentColor: Color,
    onSetPreset: (String) -> Unit,
    onSetAccentColor: (String) -> Unit
) {
    TvLazyColumn(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            SettingsSectionTitle("Theme Presets")
            Spacer(Modifier.height(12.dp))
            // 2-column preset grid
            ThemePresetGrid(
                currentName  = currentPreset.name,
                accentColor  = accentColor,
                onSelectPreset = onSetPreset
            )
        }
        item {
            SettingsSectionTitle("Accent Color Quick-Pick")
            Spacer(Modifier.height(12.dp))
            AccentColorPicker(
                current   = accentColor,
                onPick    = onSetAccentColor
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ThemePresetGrid(
    currentName: String,
    accentColor: Color,
    onSelectPreset: (String) -> Unit
) {
    val presets = com.antigravity.tvlauncher.data.ThemePreset.ALL
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { preset ->
                    var focused by remember { mutableStateOf(false) }
                    val isSelected = preset.name == currentName
                    val presetAccent = try {
                        Color(android.graphics.Color.parseColor(preset.accentHex))
                    } catch (_: Exception) { accentColor }
                    val presetBg = try {
                        Color(android.graphics.Color.parseColor(preset.backgroundHex))
                    } catch (_: Exception) { Color.Black }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(presetBg)
                            .border(
                                width  = if (isSelected || focused) 2.dp else 0.dp,
                                color  = if (isSelected) presetAccent else if (focused) Color.White else Color.Transparent,
                                shape  = RoundedCornerShape(8.dp)
                            )
                            .onFocusChanged { focused = it.isFocused }
                            .focusable()
                            .onKeyEvent { ev ->
                                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                                    && ev.type == KeyEventType.KeyUp
                                ) { onSelectPreset(preset.name); true } else false
                            }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(presetAccent, CircleShape)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text  = preset.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AccentColorPicker(
    current: Color,
    onPick: (String) -> Unit
) {
    val presetColors = listOf(
        "#4FC3F7", "#FF6B35", "#4CAF50", "#80DEEA", "#F48FB1",
        "#E0E0E0", "#39FF14", "#FFFFFF", "#FFD740", "#F5C518",
        "#FF4081", "#7C4DFF", "#00BCD4", "#FF6D00", "#64FFDA"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        presetColors.forEach { hex ->
            val color = try {
                Color(android.graphics.Color.parseColor(hex))
            } catch (_: Exception) { current }
            val isSelected = hex.equals(
                String.format("#%06X", (0xFFFFFF and current.toArgb())),
                ignoreCase = true
            )
            var focused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected || focused) 2.dp else 0.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
                    .onFocusChanged { focused = it.isFocused }
                    .focusable()
                    .onKeyEvent { ev ->
                        if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                            && ev.type == KeyEventType.KeyUp
                        ) { onPick(hex); true } else false
                    }
            )
        }
    }
}

// ── LAYOUT PANEL ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LayoutPanel(
    rowConfigs: List<RowConfig>,
    gridColumns: Int,
    onSetRowVisible: (String, Boolean) -> Unit,
    onToggleCollapse: (String) -> Unit,
    onSetGridColumns: (Int) -> Unit
) {
    TvLazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            SettingsSectionTitle("Row Visibility")
        }
        items(rowConfigs) { row ->
            RowVisibilityItem(
                row         = row,
                onSetVisible = { onSetRowVisible(row.id, it) }
            )
        }
        item {
            Spacer(Modifier.height(8.dp))
            SettingsSectionTitle("Grid Columns: $gridColumns")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (3..8).forEach { count ->
                    ColumnCountChip(count, count == gridColumns) { onSetGridColumns(count) }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RowVisibilityItem(
    row: RowConfig,
    onSetVisible: (Boolean) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val accentColor = LocalAccentColor.current

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) Color(0x22FFFFFF) else Color(0x11FFFFFF))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                    && ev.type == KeyEventType.KeyUp
                ) { onSetVisible(!row.isVisible); true } else false
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text  = row.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (row.isVisible) TextPrimary else TextSecondary
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (row.isVisible) accentColor else Color(0xFF555555))
        ) {
            if (row.isVisible) {
                Icon(
                    imageVector        = Icons.Default.Check,
                    contentDescription = "Visible",
                    tint               = Color.Black,
                    modifier           = Modifier.padding(4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ColumnCountChip(count: Int, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val accentColor = LocalAccentColor.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) accentColor
                else if (focused) accentColor.copy(0.25f)
                else Color(0x22FFFFFF)
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                    && ev.type == KeyEventType.KeyUp
                ) { onClick(); true } else false
            }
    ) {
        Text(
            text  = "$count",
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) Color.Black else TextPrimary
        )
    }
}

// ── BEHAVIOR PANEL ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BehaviorPanel(
    clockIs24h: Boolean,
    screensaverMins: Int,
    onToggleClock: () -> Unit,
    onSetScreensaver: (Int) -> Unit
) {
    val accentColor = LocalAccentColor.current
    TvLazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            SettingsSectionTitle("Android TV Settings")
            Spacer(Modifier.height(8.dp))
            val ctx = LocalContext.current
            var focused by remember { mutableStateOf(false) }
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (focused) Color(0x22FFFFFF) else Color(0x11FFFFFF))
                    .onFocusChanged { focused = it.isFocused }
                    .focusable()
                    .onKeyEvent { ev ->
                        if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                            && ev.type == KeyEventType.KeyUp
                        ) {
                            try {
                                ctx.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            } catch (_: Exception) {}
                            true
                        } else false
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    Text("Open Android TV Settings", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Text("Access network, sound, display, accounts & device preferences", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
        }
        item {
            SettingsSectionTitle("Clock")
            Spacer(Modifier.height(8.dp))
            ToggleSettingItem(
                label       = "24-hour clock format",
                description = if (clockIs24h) "Currently: 24h (e.g. 14:30)" else "Currently: 12h (e.g. 2:30 PM)",
                enabled     = clockIs24h,
                onClick     = onToggleClock
            )
        }
        item {
            SettingsSectionTitle("Screensaver")
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Activate after: ${screensaverMins} min${if (screensaverMins != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 5, 10, 15, 30).forEach { mins ->
                    ColumnCountChip(mins, mins == screensaverMins) { onSetScreensaver(mins) }
                }
            }
        }
    }
}

// ── PROFILES / APPS / BACKUP / ABOUT PANELS ────────────────────────────────────
// (Informational panels — full CRUD is done via ProfileSwitcher overlay)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfilesPanel() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AccountCircle, null, tint = LocalAccentColor.current, modifier = Modifier.size(40.dp))
            Text("Manage profiles via the avatar in the top bar", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppsPanel(allApps: List<AppInfo>, onResetUsage: () -> Unit) {
    val accentColor = LocalAccentColor.current
    val hiddenCount = allApps.count { it.isHidden }
    TvLazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            SettingsSectionTitle("App Statistics")
            Spacer(Modifier.height(4.dp))
            Text("Total apps: ${allApps.size}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text("Hidden apps: $hiddenCount", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        item {
            SettingsSectionTitle("Usage Data")
            Spacer(Modifier.height(8.dp))
            var focused by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (focused) Color(0x33EF5350) else Color(0x22EF5350))
                    .onFocusChanged { focused = it.isFocused }
                    .focusable()
                    .onKeyEvent { ev ->
                        if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                            && ev.type == KeyEventType.KeyUp
                        ) { onResetUsage(); true } else false
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                Text("Reset all usage data", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFEF5350))
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BackupPanel() {
    val ctx = LocalContext.current
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.SaveAlt, null, tint = LocalAccentColor.current, modifier = Modifier.size(40.dp))
            Text("All settings are stored locally on this device.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text("Your data never leaves your TV.", style = MaterialTheme.typography.bodySmall, color = TextSecondary.copy(0.6f))
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AboutPanel() {
    TvLazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("NovaTV Launcher", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Version 1.0", style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
        }
        item {
            HorizontalDivider(color = Color(0xFF333333))
        }
        item {
            AboutRow("Stack", "Kotlin + Jetpack Compose TV")
            AboutRow("License", "Free & Open Source")
            AboutRow("Ads", "None — ever")
            AboutRow("Tracking", "All data stays on device")
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

// ── Shared small helpers ──────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text       = title,
        fontSize   = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color      = LocalAccentColor.current,
        letterSpacing = 1.sp
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ToggleSettingItem(
    label: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val accentColor = LocalAccentColor.current
    var focused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) Color(0x22FFFFFF) else Color(0x11FFFFFF))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                    && ev.type == KeyEventType.KeyUp
                ) { onClick(); true } else false
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Box(
            modifier = Modifier
                .width(44.dp).height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (enabled) accentColor else Color(0xFF555555))
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(if (enabled) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}
