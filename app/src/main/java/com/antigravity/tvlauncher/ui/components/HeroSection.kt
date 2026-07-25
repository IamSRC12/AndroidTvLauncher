package com.antigravity.tvlauncher.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.antigravity.tvlauncher.data.AppCategory
import com.antigravity.tvlauncher.data.AppInfo
import com.antigravity.tvlauncher.ui.theme.*
import kotlinx.coroutines.delay

// One gradient palette per position (0-4) so each hero card has a unique look
private val HERO_GRADIENTS = listOf(
    listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)),
    listOf(Color(0xFF2C0A37), Color(0xFF512B58), Color(0xFF1B0A2C)),
    listOf(Color(0xFF0A2E0A), Color(0xFF1B5E20), Color(0xFF071207)),
    listOf(Color(0xFF2E1A00), Color(0xFF5D4037), Color(0xFF1C0900)),
    listOf(Color(0xFF001529), Color(0xFF003D5B), Color(0xFF000E1A)),
)

/**
 * Hero / Spotlight section — a full-width featured app carousel.
 *
 * - Shows up to 5 apps (favorites first, then most-launched)
 * - Auto-rotates every 8 seconds; rotation pauses when focused
 * - L/R D-pad navigates between items manually
 * - Each card: large icon, bold name, category label, Launch button
 * - Background gradient tinted to the item's position palette
 * - Dot indicators at the bottom
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HeroSection(
    apps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    if (apps.isEmpty()) return

    var currentIndex by remember { mutableStateOf(0) }
    var isFocusedInHero by remember { mutableStateOf(false) }
    val accentColor = LocalAccentColor.current

    // Auto-rotate every 8 seconds, pauses when user is navigating the hero
    LaunchedEffect(isFocusedInHero) {
        if (!isFocusedInHero) {
            while (true) {
                delay(8_000L)
                currentIndex = (currentIndex + 1) % apps.size
            }
        }
    }

    val currentApp = apps[currentIndex]
    val gradient   = HERO_GRADIENTS[currentIndex % HERO_GRADIENTS.size]
    val cardRadius = LocalCardRadius.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(cardRadius.dp))
            .onFocusChanged { isFocusedInHero = it.hasFocus }
            .onKeyEvent { ev ->
                when {
                    ev.key == Key.DirectionLeft && ev.type == KeyEventType.KeyUp -> {
                        currentIndex = (currentIndex - 1 + apps.size) % apps.size; true
                    }
                    ev.key == Key.DirectionRight && ev.type == KeyEventType.KeyUp -> {
                        currentIndex = (currentIndex + 1) % apps.size; true
                    }
                    else -> false
                }
            }
    ) {
        // ── Animated background gradient ────────────────────────────────────
        AnimatedContent(
            targetState   = currentIndex,
            transitionSpec = {
                fadeIn(tween(500)) togetherWith fadeOut(tween(500))
            },
            label = "heroBackground"
        ) { idx ->
            val grad = HERO_GRADIENTS[idx % HERO_GRADIENTS.size]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(grad))
            )
        }

        // ── Subtle grid pattern overlay ────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x15000000))
        )

        // ── Content row ────────────────────────────────────────────────────
        AnimatedContent(
            targetState   = currentApp,
            transitionSpec = {
                (fadeIn(tween(400)) + slideInHorizontally { it / 8 }) togetherWith
                (fadeOut(tween(300)) + slideOutHorizontally { -it / 8 })
            },
            label = "heroContent"
        ) { app ->
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp, vertical = 28.dp)
            ) {
                // App icon
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(app.icon)
                            .crossfade(300)
                            .build()
                    ),
                    contentDescription = app.displayLabel,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(20.dp))
                )

                // Text info + launch button
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Category chip
                    CategoryChip(app.category)

                    Text(
                        text       = app.displayLabel,
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = TextPrimary,
                        maxLines   = 1
                    )

                    Text(
                        text  = "Tap to launch  •  Long press for options",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(Modifier.height(4.dp))

                    HeroLaunchButton(
                        accentColor = accentColor,
                        onClick     = { onLaunch(app) },
                        onLongPress = { onLongPress(app) }
                    )
                }

                // Right decorative gradient blob
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                ),
                                radius = size.minDimension / 2
                            )
                        }
                )
            }
        }

        // ── Dot indicators ─────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            apps.forEachIndexed { idx, _ ->
                val isActive = idx == currentIndex
                val dotWidth by animateDpAsState(
                    targetValue   = if (isActive) 20.dp else 6.dp,
                    animationSpec = tween(300),
                    label         = "dotWidth"
                )
                Box(
                    modifier = Modifier
                        .width(dotWidth)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) accentColor else Color.White.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

// ── Category chip ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryChip(category: AppCategory) {
    val label = when (category) {
        AppCategory.STREAMING -> "Streaming"
        AppCategory.GAME      -> "Game"
        AppCategory.MUSIC     -> "Music"
        AppCategory.TOOL      -> "Tool"
        AppCategory.OTHER     -> "App"
    }
    Box(
        modifier = Modifier
            .background(Color(0x33FFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary
        )
    }
}

// ── Launch button ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun HeroLaunchButton(
    accentColor: Color,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    var focused   by remember { mutableStateOf(false) }
    var holdStart by remember { mutableStateOf(0L) }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(
                if (focused) accentColor else accentColor.copy(alpha = 0.85f),
                RoundedCornerShape(24.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                when {
                    (ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                            && ev.type == KeyEventType.KeyDown -> {
                        if (holdStart == 0L) holdStart = System.currentTimeMillis(); true
                    }
                    (ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                            && ev.type == KeyEventType.KeyUp -> {
                        val held = System.currentTimeMillis() - holdStart
                        holdStart = 0L
                        if (held >= 600L) onLongPress() else onClick()
                        true
                    }
                    else -> false
                }
            }
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector        = Icons.Default.PlayArrow,
            contentDescription = "Launch",
            tint               = Color.Black,
            modifier           = Modifier.size(18.dp)
        )
        Text(
            text  = "Open",
            style = MaterialTheme.typography.labelLarge,
            color = Color.Black
        )
    }
}
