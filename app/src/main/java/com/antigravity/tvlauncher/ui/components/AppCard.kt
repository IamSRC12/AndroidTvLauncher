package com.antigravity.tvlauncher.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.tv.material3.*
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.antigravity.tvlauncher.data.AppInfo
import com.antigravity.tvlauncher.ui.theme.*

/**
 * TV-optimised app card.
 * Hardware accelerated, smooth 60fps spring scale on focus (1.0 → 1.12)
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppCard(
    app: AppInfo,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    showLaunchCount: Boolean = false
) {
    var focused   by remember { mutableStateOf(false) }
    var holdStart by remember { mutableStateOf(0L) }

    val accentColor = LocalAccentColor.current
    val cardRadius  = LocalCardRadius.current

    val scale by animateFloatAsState(
        targetValue   = if (focused) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
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
                    (ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                            && ev.type == KeyEventType.KeyDown -> {
                        if (holdStart == 0L) holdStart = System.currentTimeMillis()
                        true
                    }
                    (ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                            && ev.type == KeyEventType.KeyUp -> {
                        val held = System.currentTimeMillis() - holdStart
                        holdStart = 0L
                        if (held >= 600L) onLongPress() else onClick()
                        true
                    }
                    ev.key == Key.Menu && ev.type == KeyEventType.KeyUp -> {
                        onLongPress(); true
                    }
                    else -> false
                }
            }
    ) {
        Box {
            // ── Icon box with hardware-accelerated focus ring ───────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(cardRadius.dp))
                    .background(if (focused) Color(0xFF2A2A2A) else Color(0xFF1A1A1A))
                    .border(
                        width = if (focused) 2.5.dp else 1.dp,
                        color = if (focused) accentColor else Color(0x22FFFFFF),
                        shape = RoundedCornerShape(cardRadius.dp)
                    )
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(app.icon)
                            .crossfade(150)
                            .allowHardware(true)
                            .build()
                    ),
                    contentDescription = app.displayLabel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                )
            }

            // ── NEW badge ───────────────────────────────────────────────────
            if (app.isNew) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .background(Color(0xFF00C853), RoundedCornerShape(6.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text  = "NEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 8.sp
                    )
                }
            }

            // ── Favourite star badge ────────────────────────────────────────
            if (app.isFavorite) {
                androidx.compose.material3.Icon(
                    imageVector        = Icons.Default.Star,
                    contentDescription = "Favourite",
                    tint               = accentColor,
                    modifier           = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(14.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // ── App label ───────────────────────────────────────────────────────
        Text(
            text      = app.displayLabel,
            style     = MaterialTheme.typography.labelMedium,
            color     = if (focused) TextPrimary else TextSecondary,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier  = Modifier.width(100.dp)
        )

        // ── Launch count label (only in Most Used row) ──────────────────────
        if (showLaunchCount && app.launchCount > 0) {
            Text(
                text     = "${app.launchCount}×",
                style    = MaterialTheme.typography.labelSmall,
                color    = TextSecondary.copy(alpha = 0.6f),
                fontSize = 9.sp,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}
