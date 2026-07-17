package com.antigravity.tvlauncher.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.antigravity.tvlauncher.data.AppInfo
import com.antigravity.tvlauncher.ui.theme.TextPrimary
import com.antigravity.tvlauncher.ui.theme.TextSecondary

/**
 * TV-optimised app card:
 * • NO border rings — just a rounded-square icon (exactly like Live TV reference image)
 * • Smooth spring scale animation on focus (1.0 → 1.18)
 * • Long-press (600 ms DPAD_CENTER hold) shows context menu
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    app: AppInfo,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onHide: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused   by remember { mutableStateOf(false) }
    var menuOpen  by remember { mutableStateOf(false) }
    var holdStart by remember { mutableStateOf(0L) }

    val scale by animateFloatAsState(
        targetValue  = if (focused) 1.18f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "cardScale"
    )

    val focusRequester = remember { FocusRequester() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .width(110.dp)
            .wrapContentHeight()
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                when {
                    (ev.key == Key.DirectionCenter || ev.key == Key.Enter) && ev.type == KeyEventType.KeyDown -> {
                        if (holdStart == 0L) holdStart = System.currentTimeMillis()
                        true
                    }
                    (ev.key == Key.DirectionCenter || ev.key == Key.Enter) && ev.type == KeyEventType.KeyUp -> {
                        val held = System.currentTimeMillis() - holdStart
                        holdStart = 0L
                        if (held >= 600L) menuOpen = true else onClick()
                        true
                    }
                    ev.key == Key.Menu && ev.type == KeyEventType.KeyUp -> { menuOpen = true; true }
                    else -> false
                }
            }
    ) {
        Box {
            // ── Icon box: rounded corners, NO border ─────────────────────────
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))   // Same radius style as reference image
                    .background(if (focused) Color(0xFF2A2A2A) else Color(0xFF1A1A1A))
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(app.icon)
                            .crossfade(200)
                            .allowHardware(true)
                            .build()
                    ),
                    contentDescription = app.label,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)              // slight padding so icon doesn't bleed to edge
                )
            }

            // ── Context menu ─────────────────────────────────────────────────
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                modifier = Modifier
                    .background(Color(0xFF1E1E1E))
                    .width(200.dp)
            ) {
                DropdownMenuItem(
                    text = { MenuText(if (app.isFavorite) "Remove from Favourites" else "Add to Favourites") },
                    onClick = { menuOpen = false; onFavorite() }
                )
                DropdownMenuItem(
                    text = { MenuText("Hide App") },
                    onClick = { menuOpen = false; onHide() }
                )
                HorizontalDivider(color = Color(0xFF333333))
                DropdownMenuItem(
                    text = { MenuText("Uninstall", color = Color(0xFFEF5350)) },
                    onClick = { menuOpen = false; onUninstall() }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── App label ────────────────────────────────────────────────────────
        Text(
            text      = app.label,
            style     = MaterialTheme.typography.labelLarge,
            color     = if (focused) TextPrimary else TextSecondary,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier  = Modifier.width(100.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MenuText(text: String, color: Color = Color.White) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
}
