package com.antigravity.tvlauncher.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.antigravity.tvlauncher.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.*

/**
 * Full-screen screensaver activated after [idleMinutes] of inactivity.
 *
 * Features:
 * - Near-black background (#050505) to save OLED pixels
 * - Large clock (24h format) that slowly drifts to a new random position every 30 seconds
 *   to prevent burn-in
 * - Date shown below the clock
 * - Smooth fade in/out transitions
 * - Any key press dismisses the screensaver
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ScreensaverScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var timeString by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf("") }

    // Clock position — expressed as fraction of screen (0.0 to 1.0 with safe margins)
    var xFraction by remember { mutableStateOf(0.5f) }
    var yFraction by remember { mutableStateOf(0.5f) }

    val accentColor = LocalAccentColor.current

    // Update time every second
    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
            dateString = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(now.time)
            delay(1_000L)
        }
    }

    // Move clock position every 30 seconds to prevent burn-in
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            xFraction = Random.nextFloat() * 0.5f + 0.15f // 0.15 to 0.65
            yFraction = Random.nextFloat() * 0.5f + 0.15f // 0.15 to 0.65
        }
    }

    // Animated clock position
    val animX by animateFloatAsState(
        targetValue   = xFraction,
        animationSpec = tween(2_000, easing = EaseInOutCubic),
        label         = "clockX"
    )
    val animY by animateFloatAsState(
        targetValue   = yFraction,
        animationSpec = tween(2_000, easing = EaseInOutCubic),
        label         = "clockY"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            // Any key press dismisses the screensaver
            .onKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyUp) { onDismiss(); true } else false
            }
            .focusable()
    ) {
        // Clock rendered at animated fraction position
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val clockW = 280.dp
            val clockH = 80.dp

            val xDp = (constraints.maxWidth * animX).dp - (clockW / 2)
            val yDp = (constraints.maxHeight * animY).dp - (clockH / 2)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(x = xDp, y = yDp)
            ) {
                // Time
                Text(
                    text       = timeString,
                    fontSize   = 64.sp,
                    fontWeight = FontWeight.Light,
                    color      = TextPrimary.copy(alpha = 0.9f),
                    letterSpacing = 4.sp
                )

                // Date
                Text(
                    text     = dateString,
                    fontSize = 16.sp,
                    color    = TextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Small accent dot
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(accentColor.copy(alpha = 0.6f), CircleShape)
                )
            }
        }

        // "Press any key to wake" hint (very dim)
        Text(
            text     = "Press any key to wake",
            style    = MaterialTheme.typography.labelSmall,
            color    = TextSecondary.copy(alpha = 0.2f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}
