package com.antigravity.tvlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import com.antigravity.tvlauncher.data.AppInfo
import com.antigravity.tvlauncher.ui.theme.*

/**
 * Usage statistics overlay — shows aggregate usage data tracked in-app.
 * All data is purely local; nothing is sent to any server.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun UsageStatsOverlay(
    allApps: List<AppInfo>,
    onResetUsage: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    val cardRadius  = LocalCardRadius.current

    // Top 10 apps by launch count
    val topApps = remember(allApps) {
        allApps.filter { it.launchCount > 0 }
            .sortedByDescending { it.launchCount }
            .take(10)
    }

    val totalLaunches = remember(allApps) { allApps.sumOf { it.launchCount } }
    val maxCount      = topApps.firstOrNull()?.launchCount ?: 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE6000000))
            .onKeyEvent { ev ->
                if ((ev.key == Key.Escape || ev.key == Key.Back) && ev.type == KeyEventType.KeyUp) {
                    onDismiss(); true
                } else false
            }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(600.dp)
                .clip(RoundedCornerShape((cardRadius + 8).dp))
                .background(Color(0xFF1A1A1A))
                .padding(32.dp)
        ) {
            // Header
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector        = Icons.Default.QueryStats,
                    contentDescription = "Stats",
                    tint               = accentColor,
                    modifier           = Modifier.size(28.dp)
                )
                Text(
                    text       = "Usage Statistics",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text   = "$totalLaunches total launches",
                    style  = MaterialTheme.typography.labelMedium,
                    color  = TextSecondary
                )
            }

            Spacer(Modifier.height(24.dp))

            if (topApps.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                ) {
                    Text(
                        text  = "No usage data yet. Launch some apps!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                Text(
                    text  = "Most Launched Apps",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Top apps with horizontal bar chart
                TvLazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier            = Modifier.height(300.dp)
                ) {
                    items(topApps) { app ->
                        UsageBar(
                            app         = app,
                            maxCount    = maxCount,
                            accentColor = accentColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Reset button
            var resetFocused by remember { mutableStateOf(false) }
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (resetFocused) Color(0x33EF5350) else Color(0x22EF5350))
                    .onFocusChanged { resetFocused = it.isFocused }
                    .focusable()
                    .onKeyEvent { ev ->
                        if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                            && ev.type == KeyEventType.KeyUp
                        ) { onResetUsage(); onDismiss(); true } else false
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Delete,
                    contentDescription = "Reset",
                    tint               = Color(0xFFEF5350),
                    modifier           = Modifier.size(16.dp)
                )
                Text(
                    text  = "Reset All Usage Data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFEF5350)
                )
            }
        }
    }
}

// ── Single usage bar row ───────────────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun UsageBar(
    app: AppInfo,
    maxCount: Int,
    accentColor: Color
) {
    val fraction = (app.launchCount.toFloat() / maxCount).coerceIn(0f, 1f)

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier              = Modifier.fillMaxWidth()
    ) {
        Text(
            text     = app.displayLabel,
            style    = MaterialTheme.typography.bodySmall,
            color    = TextPrimary,
            maxLines = 1,
            modifier = Modifier.width(140.dp)
        )

        // Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF333333))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accentColor)
            )
        }

        Text(
            text     = "${app.launchCount}×",
            style    = MaterialTheme.typography.labelSmall,
            color    = accentColor,
            modifier = Modifier.width(36.dp)
        )
    }
}
