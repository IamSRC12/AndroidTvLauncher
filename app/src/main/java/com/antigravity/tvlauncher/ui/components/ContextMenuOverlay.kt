package com.antigravity.tvlauncher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.*
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.antigravity.tvlauncher.data.AppInfo
import com.antigravity.tvlauncher.ui.theme.*

private enum class MenuAction(val label: String, val destructive: Boolean = false) {
    OPEN("Open"),
    ADD_FAVORITE("Add to Favorites"),
    REMOVE_FAVORITE("Remove from Favorites"),
    HIDE("Hide App"),
    UNHIDE("Unhide App"),
    RENAME("Rename"),
    APP_INFO("App Info"),
    UNINSTALL("Uninstall", destructive = true)
}

/**
 * Full-screen semi-transparent overlay containing a floating context menu panel.
 * Appears when user long-presses an app card (600ms hold).
 *
 * Focus is fully trapped inside this overlay and auto-requests focus on first item.
 * Remote Back button / Escape key cleanly dismisses the menu.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ContextMenuOverlay(
    app: AppInfo,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleHidden: () -> Unit,
    onRename: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardRadius = LocalCardRadius.current
    val accentColor = LocalAccentColor.current

    // Android TV Back Button handler
    BackHandler { onDismiss() }

    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(50L)
        runCatching { firstItemFocusRequester.requestFocus() }
    }

    val actions = remember(app.isFavorite, app.isHidden) {
        buildList {
            add(MenuAction.OPEN)
            if (app.isFavorite) add(MenuAction.REMOVE_FAVORITE) else add(MenuAction.ADD_FAVORITE)
            if (app.isHidden) add(MenuAction.UNHIDE) else add(MenuAction.HIDE)
            add(MenuAction.RENAME)
            add(MenuAction.APP_INFO)
            add(MenuAction.UNINSTALL)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .onKeyEvent { ev ->
                if ((ev.key == Key.Escape || ev.key == Key.Back) && ev.type == KeyEventType.KeyUp) {
                    onDismiss()
                    true
                } else false
            }
    ) {
        AnimatedVisibility(
            visible       = true,
            enter         = scaleIn(initialScale = 0.85f, animationSpec = tween(180)) + fadeIn(tween(180)),
            exit          = scaleOut(targetScale = 0.85f, animationSpec = tween(120)) + fadeOut(tween(120)),
            modifier      = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .clip(RoundedCornerShape((cardRadius + 4).dp))
                    .background(Color(0xFF1C1C1C))
            ) {
                Column {
                    // ── App header ──────────────────────────────────────────
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF242424))
                            .padding(16.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(app.icon)
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = app.displayLabel,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column {
                            Text(
                                text       = app.displayLabel,
                                style      = MaterialTheme.typography.titleSmall,
                                color      = TextPrimary,
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text     = app.packageName,
                                style    = MaterialTheme.typography.labelSmall,
                                color    = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 10.sp
                            )
                            if (app.launchCount > 0) {
                                Text(
                                    text     = "Opened ${app.launchCount}×",
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = accentColor.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // ── Menu items ──────────────────────────────────────────
                    TvLazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(actions) { index, action ->
                            ContextMenuItem(
                                action         = action,
                                accentColor    = accentColor,
                                focusRequester = if (index == 0) firstItemFocusRequester else remember { FocusRequester() },
                                onClick        = {
                                    when (action) {
                                        MenuAction.OPEN             -> { onDismiss(); onLaunch() }
                                        MenuAction.ADD_FAVORITE,
                                        MenuAction.REMOVE_FAVORITE  -> { onToggleFavorite(); onDismiss() }
                                        MenuAction.HIDE,
                                        MenuAction.UNHIDE           -> { onToggleHidden(); onDismiss() }
                                        MenuAction.RENAME           -> { onRename() }
                                        MenuAction.APP_INFO         -> { onDismiss(); onAppInfo() }
                                        MenuAction.UNINSTALL        -> { onDismiss(); onUninstall() }
                                    }
                                }
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
private fun ContextMenuItem(
    action: MenuAction,
    accentColor: Color,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    val icon = when (action) {
        MenuAction.OPEN             -> Icons.Default.PlayArrow
        MenuAction.ADD_FAVORITE     -> Icons.Default.StarBorder
        MenuAction.REMOVE_FAVORITE  -> Icons.Default.Star
        MenuAction.HIDE             -> Icons.Default.VisibilityOff
        MenuAction.UNHIDE           -> Icons.Default.Visibility
        MenuAction.RENAME           -> Icons.Default.Edit
        MenuAction.APP_INFO         -> Icons.Default.Info
        MenuAction.UNINSTALL        -> Icons.Default.DeleteForever
    }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(if (focused) Color(0x22FFFFFF) else Color.Transparent)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                    && ev.type == KeyEventType.KeyUp
                ) { onClick(); true }
                else if ((ev.key == Key.Back || ev.key == Key.Escape) && ev.type == KeyEventType.KeyUp) {
                    onClick(); true
                } else false
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = action.label,
            tint               = if (action.destructive) Color(0xFFEF5350)
                                 else if (focused) accentColor else TextSecondary,
            modifier           = Modifier.size(18.dp)
        )
        Text(
            text  = action.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (action.destructive) Color(0xFFEF5350)
                    else if (focused) TextPrimary else TextSecondary
        )
    }
}
