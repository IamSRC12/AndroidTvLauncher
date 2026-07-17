package com.antigravity.tvlauncher.ui.components

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.antigravity.tvlauncher.ui.theme.*
import com.antigravity.tvlauncher.util.BluetoothHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BluetoothOverlay(
    btHelper: BluetoothHelper,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var devices by remember { mutableStateOf(emptyList<BluetoothDevice>()) }
    var statusMap by remember { mutableStateOf(mapOf<String, BtStatus>()) }

    // Auto-focus the close button on first composition
    val closeFR = remember { FocusRequester() }
    val addFR   = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        devices = btHelper.getPairedDevices()
        // Request focus on the first action button
        runCatching { closeFR.requestFocus() }
    }

    // Full-screen dim overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .onKeyEvent { ev ->
                if (ev.key == Key.Back && ev.type == KeyEventType.KeyUp) { onDismiss(); true }
                else false
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(460.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF111111))
                .padding(0.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF191919))
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text  = "Bluetooth",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Add Device button → opens system BT settings
                    BtHeaderButton(
                        icon = Icons.Default.Add,
                        label = "Add Device",
                        focusRequester = addFR,
                        onClick = {
                            try { ctx.startActivity(btHelper.openBluetoothSettings()) } catch (_: Exception) {}
                        }
                    )
                    BtHeaderButton(
                        icon = Icons.Default.Close,
                        label = "Close",
                        focusRequester = closeFR,
                        onClick = onDismiss
                    )
                }
            }

            // ── Status text ───────────────────────────────────────────────────
            if (!btHelper.isEnabled()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = Color(0xFF555555),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text  = "Bluetooth is off",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text  = "Enable it in Android Settings",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF555555)
                        )
                    }
                }
            } else if (devices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.BluetoothSearching,
                            contentDescription = null,
                            tint = Color(0xFF555555),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text  = "No paired devices",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text  = "Press \"Add Device\" to pair a new one",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF555555)
                        )
                    }
                }
            } else {
                // ── Device list ───────────────────────────────────────────────
                LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(devices) { index, device ->
                        val status = statusMap[device.address] ?: BtStatus.Idle
                        DeviceRow(
                            device    = device,
                            status    = status,
                            autoFocus = index == 0,
                            onConnect = {
                                statusMap = statusMap + (device.address to BtStatus.Connecting)
                                scope.launch {
                                    val ok = btHelper.connect(device)
                                    statusMap = statusMap + (device.address to
                                            if (ok) BtStatus.Connected else BtStatus.Failed)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── System Settings shortcut ───────────────────────────────────
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                BtActionButton(
                    label = "Open Android TV Settings",
                    onClick = {
                        try { ctx.startActivity(btHelper.openSystemSettings()) } catch (_: Exception) {}
                    }
                )
            }
        }
    }
}

// ── BtStatus sealed class ─────────────────────────────────────────────────────
sealed class BtStatus {
    object Idle       : BtStatus()
    object Connecting : BtStatus()
    object Connected  : BtStatus()
    object Failed     : BtStatus()
}

// ── Device row item ───────────────────────────────────────────────────────────
@SuppressLint("MissingPermission")
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DeviceRow(
    device: BluetoothDevice,
    status: BtStatus,
    autoFocus: Boolean,
    onConnect: () -> Unit
) {
    val fr = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.03f else 1.0f,
        animationSpec = spring(),
        label = "devScale"
    )

    val deviceName = runCatching { device.name ?: "Unknown Device" }.getOrElse { "Unknown Device" }

    LaunchedEffect(autoFocus) {
        if (autoFocus) runCatching { fr.requestFocus() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Color(0xFF2A2A2A) else Color.Transparent)
            .focusRequester(fr)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter) && ev.type == KeyEventType.KeyUp) {
                    if (status !is BtStatus.Connecting) onConnect()
                    true
                } else false
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Device type icon
        val typeIcon: ImageVector = when {
            deviceName.contains("headphone", ignoreCase = true) ||
            deviceName.contains("earphone", ignoreCase = true)  -> Icons.Default.Headphones
            deviceName.contains("speaker", ignoreCase = true)   -> Icons.Default.SpeakerGroup
            status is BtStatus.Connected                         -> Icons.Default.BluetoothConnected
            else                                                 -> Icons.Default.Bluetooth
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AccentBlueDim),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = typeIcon,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = deviceName,
                style    = MaterialTheme.typography.titleMedium,
                color    = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text  = when (status) {
                    is BtStatus.Connecting -> "Connecting…"
                    is BtStatus.Connected  -> "Connected"
                    is BtStatus.Failed     -> "Failed – tap to retry"
                    else                   -> device.address
                },
                style = MaterialTheme.typography.labelMedium,
                color = when (status) {
                    is BtStatus.Connected  -> Color(0xFF4CAF50)
                    is BtStatus.Connecting -> Color(0xFFFFC107)
                    is BtStatus.Failed     -> Color(0xFFEF5350)
                    else                   -> TextSecondary
                }
            )
        }
    }
}

// ── Small icon button for header ──────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BtHeaderButton(
    icon: ImageVector,
    label: String,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (focused) Color.White else Color(0xFF2A2A2A))
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter) && ev.type == KeyEventType.KeyUp) {
                    onClick(); true
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (focused) Color.Black else Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ── Full-width text action button ─────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BtActionButton(label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) Color.White else Color(0xFF222222))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter) && ev.type == KeyEventType.KeyUp) {
                    onClick(); true
                } else false
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (focused) Color.Black else Color(0xFFAAAAAA)
        )
    }
}
