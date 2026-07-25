package com.antigravity.tvlauncher.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import com.antigravity.tvlauncher.data.AppInfo
import com.antigravity.tvlauncher.data.RowConfig
import com.antigravity.tvlauncher.ui.theme.*

/**
 * A single horizontally-scrollable app row on the home screen.
 *
 * Features:
 * - Row label with optional per-row accent color
 * - Collapse/expand toggle (▼ arrow rotates 180° when collapsed, height animates)
 * - "+ Add App" card at the end of the row (e.g. for Favorites) with matching focus scale & styling
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppRow(
    config: RowConfig,
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit,
    onToggleCollapse: () -> Unit,
    onAddAppClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showLaunchCount: Boolean = false
) {
    val accentColor = LocalAccentColor.current
    val rowAccent = remember(config.accentHex, accentColor) {
        config.accentHex?.let {
            try { Color(android.graphics.Color.parseColor(it)) }
            catch (_: Exception) { accentColor }
        } ?: accentColor
    }

    val arrowRotation by animateFloatAsState(
        targetValue   = if (config.isCollapsed) -90f else 0f,
        animationSpec = tween(200),
        label         = "collapseArrow"
    )

    var headerFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {

        // ── Row header ──────────────────────────────────────────────────────
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Text(
                text  = config.title,
                style = MaterialTheme.typography.titleMedium,
                color = rowAccent
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .onFocusChanged { headerFocused = it.isFocused }
                    .focusable()
                    .onKeyEvent { ev ->
                        if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                            && ev.type == KeyEventType.KeyUp
                        ) { onToggleCollapse(); true } else false
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text  = if (config.isCollapsed) "Show" else "Hide",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (headerFocused) rowAccent else TextSecondary
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector        = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle row",
                    tint               = if (headerFocused) rowAccent else TextSecondary,
                    modifier           = Modifier
                        .size(16.dp)
                        .rotate(arrowRotation)
                )
            }
        }

        // ── App list (animated height for collapse) ─────────────────────────
        AnimatedVisibility(
            visible = !config.isCollapsed,
            enter   = expandVertically(animationSpec = tween(220)) + fadeIn(tween(220)),
            exit    = shrinkVertically(animationSpec = tween(220)) + fadeOut(tween(220))
        ) {
            if (apps.isEmpty() && onAddAppClick == null) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .padding(start = 4.dp)
                ) {
                    Text(
                        text  = "No apps in this row",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                TvLazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding        = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppCard(
                            app             = app,
                            onClick         = { onAppClick(app) },
                            onLongPress     = { onAppLongPress(app) },
                            showLaunchCount = showLaunchCount
                        )
                    }

                    if (onAddAppClick != null) {
                        item {
                            AddAppCard(
                                accentColor = rowAccent,
                                onClick     = onAddAppClick
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Add App Card — matches standard AppCard dimensions, scale, spring animation, and focus border.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddAppCard(
    accentColor: Color,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val cardRadius = LocalCardRadius.current

    val scale by animateFloatAsState(
        targetValue   = if (focused) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "addCardScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .width(110.dp)
            .wrapContentHeight()
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                    && ev.type == KeyEventType.KeyUp
                ) { onClick(); true } else false
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(cardRadius.dp))
                .background(if (focused) Color(0xFF2A2A2A) else Color(0xFF1A1A1A))
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = if (focused) accentColor else Color(0x33FFFFFF),
                    shape = RoundedCornerShape(cardRadius.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add App",
                tint = if (focused) accentColor else TextSecondary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text      = "Add App",
            style     = MaterialTheme.typography.labelMedium,
            color     = if (focused) TextPrimary else TextSecondary,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier  = Modifier.width(100.dp)
        )
    }
}
