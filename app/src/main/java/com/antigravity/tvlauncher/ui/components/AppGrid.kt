package com.antigravity.tvlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.material3.*
import com.antigravity.tvlauncher.data.AppCategory
import com.antigravity.tvlauncher.data.AppInfo
import com.antigravity.tvlauncher.ui.theme.*

// Filter tab options for the grid
private enum class GridFilter(val label: String) {
    ALL("All"),
    STREAMING("Streaming"),
    GAMES("Games"),
    MUSIC("Music"),
    TOOLS("Tools")
}

/**
 * Full-screen scrollable app grid with category filter tabs.
 * Uses TvLazyVerticalGrid for TV-friendly D-pad navigation through all installed apps.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppGrid(
    apps: List<AppInfo>,
    columns: Int = 6,
    onAppClick: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf(GridFilter.ALL) }

    val filteredApps = remember(apps, activeFilter) {
        when (activeFilter) {
            GridFilter.ALL       -> apps
            GridFilter.STREAMING -> apps.filter { it.category == AppCategory.STREAMING }
            GridFilter.GAMES     -> apps.filter { it.category == AppCategory.GAME }
            GridFilter.MUSIC     -> apps.filter { it.category == AppCategory.MUSIC }
            GridFilter.TOOLS     -> apps.filter { it.category == AppCategory.TOOL }
        }.filter { !it.isHidden }
    }

    Column(modifier = modifier.fillMaxWidth()) {

        // ── Section header ─────────────────────────────────────────────────
        Text(
            text     = "All Apps",
            style    = MaterialTheme.typography.titleMedium,
            color    = LocalAccentColor.current,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        // ── Filter tabs ────────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        ) {
            GridFilter.entries.forEach { filter ->
                FilterTab(
                    label    = filter.label,
                    selected = activeFilter == filter,
                    onClick  = { activeFilter = filter }
                )
            }
        }

        // ── App grid ───────────────────────────────────────────────────────
        if (filteredApps.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Text(
                    text  = "No apps in this category",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            TvLazyVerticalGrid(
                columns            = TvGridCells.Fixed(columns),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement   = Arrangement.spacedBy(20.dp),
                contentPadding        = PaddingValues(bottom = 32.dp, top = 4.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppCard(
                        app         = app,
                        onClick     = { onAppClick(app) },
                        onLongPress = { onAppLongPress(app) }
                    )
                }
            }
        }
    }
}

// ── Filter tab chip ────────────────────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FilterTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = LocalAccentColor.current
    var focused by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(
                color = when {
                    selected -> accentColor
                    focused  -> accentColor.copy(alpha = 0.25f)
                    else     -> Color(0x22FFFFFF)
                },
                shape = RoundedCornerShape(20.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                    && ev.type == KeyEventType.KeyUp
                ) { onClick(); true } else false
            }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.Black else if (focused) accentColor else TextSecondary
        )
    }
}
