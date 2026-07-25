package com.antigravity.tvlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.antigravity.tvlauncher.data.Profile
import com.antigravity.tvlauncher.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Top header bar:
 *  Left  — live digital clock + date
 *  Right — WiFi icon | Bluetooth (RED if disconnected, GREEN + Device Name if connected) | HDMI Inputs | Profile | Search | Settings
 *
 * All items are individually TV-focusable.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopBar(
    is24h: Boolean,
    activeProfile: Profile,
    connectedBtDeviceName: String?,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onBluetoothClick: () -> Unit,
    onInputsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var timeString by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf("") }

    // Update clock every second
    LaunchedEffect(is24h) {
        while (true) {
            val now = Calendar.getInstance()
            val fmt = if (is24h) "HH:mm" else "hh:mm a"
            timeString = SimpleDateFormat(fmt, Locale.getDefault()).format(now.time)
            dateString = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(now.time)
            kotlinx.coroutines.delay(1_000L)
        }
    }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        // ── Left: Clock + Date ──────────────────────────────────────────────
        Column {
            Text(
                text       = timeString,
                fontSize   = 36.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )
            Text(
                text  = dateString,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // ── Right: Status icons + Bluetooth + Inputs + Profile + Search + Settings ───────────
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // WiFi icon (display-only status indicator)
            Icon(
                imageVector        = Icons.Default.Wifi,
                contentDescription = "Network",
                tint               = TextSecondary,
                modifier           = Modifier.size(20.dp)
            )

            // Bluetooth Status Indicator & Quick Opener
            // RED if nothing connected, GREEN + Device Name if connected
            TopBarAction(onClick = onBluetoothClick) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Bluetooth,
                        contentDescription = "Bluetooth",
                        tint               = if (connectedBtDeviceName != null) Color(0xFF4CAF50) else Color(0xFFEF5350),
                        modifier           = Modifier.size(20.dp)
                    )
                    if (connectedBtDeviceName != null) {
                        Text(
                            text       = connectedBtDeviceName,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color(0xFF4CAF50)
                        )
                    }
                }
            }

            // HDMI Inputs Switcher Button
            TopBarAction(onClick = onInputsClick) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Input,
                        contentDescription = "Inputs",
                        tint               = TextPrimary,
                        modifier           = Modifier.size(20.dp)
                    )
                    Text(
                        text       = "Inputs",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color      = TextPrimary
                    )
                }
            }

            // Profile avatar — focusable, opens profile switcher
            TopBarAction(onClick = onProfileClick) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            try { Color(android.graphics.Color.parseColor(activeProfile.avatarColorHex)) }
                            catch (_: Exception) { LocalAccentColor.current }
                        )
                ) {
                    Text(
                        text       = activeProfile.initials,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.Black
                    )
                }
            }

            // Search button
            TopBarAction(onClick = onSearchClick) {
                Icon(
                    imageVector        = Icons.Default.Search,
                    contentDescription = "Search",
                    tint               = TextPrimary,
                    modifier           = Modifier.size(22.dp)
                )
            }

            // Settings button — opens System Settings directly
            TopBarAction(onClick = onSettingsClick) {
                Icon(
                    imageVector        = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint               = TextPrimary,
                    modifier           = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ── Focusable top-bar icon button ─────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopBarAction(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .wrapContentWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (focused) Color(0x33FFFFFF) else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                    && ev.type == KeyEventType.KeyUp
                ) { onClick(); true } else false
            }
            .padding(horizontal = 8.dp)
    ) {
        content()
    }
}
