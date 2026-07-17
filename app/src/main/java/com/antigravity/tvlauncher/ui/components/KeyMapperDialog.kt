package com.antigravity.tvlauncher.ui.components

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.antigravity.tvlauncher.data.AppInfo
import com.antigravity.tvlauncher.ui.theme.*

// Navigation keys that should NOT be captured as mappable keys
private val RESERVED_KEYS = setOf(
    AndroidKeyEvent.KEYCODE_DPAD_UP,
    AndroidKeyEvent.KEYCODE_DPAD_DOWN,
    AndroidKeyEvent.KEYCODE_DPAD_LEFT,
    AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
    AndroidKeyEvent.KEYCODE_DPAD_CENTER,
    AndroidKeyEvent.KEYCODE_ENTER,
    AndroidKeyEvent.KEYCODE_BACK,
    AndroidKeyEvent.KEYCODE_HOME,
    AndroidKeyEvent.KEYCODE_VOLUME_UP,
    AndroidKeyEvent.KEYCODE_VOLUME_DOWN,
    AndroidKeyEvent.KEYCODE_VOLUME_MUTE,
    AndroidKeyEvent.KEYCODE_POWER
)

sealed class KmStep {
    object Overview : KmStep()
    object WaitingKey : KmStep()
    data class PickAction(val capturedCode: Int, val keyName: String) : KmStep()
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun KeyMapperDialog(
    existingMappings: Map<Int, String>,
    apps: List<AppInfo>,
    onSave: (keyCode: Int, action: String) -> Unit,
    onDelete: (keyCode: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf<KmStep>(KmStep.Overview) }
    val addFR = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .onKeyEvent { ev ->
                when {
                    ev.key == Key.Back && ev.type == KeyEventType.KeyUp -> {
                        when (step) {
                            is KmStep.WaitingKey, is KmStep.PickAction -> { step = KmStep.Overview; true }
                            else -> { onDismiss(); true }
                        }
                    }
                    step is KmStep.WaitingKey && ev.type == KeyEventType.KeyDown -> {
                        val code = ev.nativeKeyEvent.keyCode
                        if (!RESERVED_KEYS.contains(code)) {
                            val name = AndroidKeyEvent.keyCodeToString(code)
                                .removePrefix("KEYCODE_")
                                .replace("_", " ")
                                .lowercase()
                                .replaceFirstChar { it.uppercase() }
                            step = KmStep.PickAction(capturedCode = code, keyName = name)
                            true
                        } else false
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (val s = step) {
            is KmStep.Overview -> {
                OverviewPanel(
                    mappings = existingMappings,
                    onAddKey = {
                        step = KmStep.WaitingKey
                    },
                    onDelete = onDelete,
                    onDismiss = onDismiss,
                    addFR = addFR
                )
                LaunchedEffect(Unit) { runCatching { addFR.requestFocus() } }
            }
            is KmStep.WaitingKey -> WaitingKeyPanel()
            is KmStep.PickAction -> {
                PickActionPanel(
                    keyName = s.keyName,
                    apps    = apps,
                    onPick  = { action ->
                        onSave(s.capturedCode, action)
                        step = KmStep.Overview
                    }
                )
            }
        }
    }
}

// ── Overview: list current mappings + "Add" button ───────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun OverviewPanel(
    mappings: Map<Int, String>,
    onAddKey: () -> Unit,
    onDelete: (Int) -> Unit,
    onDismiss: () -> Unit,
    addFR: FocusRequester
) {
    Column(
        modifier = Modifier
            .width(480.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111111))
            .padding(24.dp)
    ) {
        Text(
            text  = "Remote Key Mapper",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Text(
            text  = "Map unused remote buttons to launch apps or actions",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        if (mappings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "No mappings yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF555555)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(mappings.entries.toList()) { (code, action) ->
                    val keyName = AndroidKeyEvent.keyCodeToString(code)
                        .removePrefix("KEYCODE_").replace("_", " ")
                    val actionLabel = when {
                        action.startsWith("launch:") -> "Launch " + action.removePrefix("launch:")
                        action == "action:search"    -> "Open Search"
                        action == "action:wallpaper" -> "Open Wallpaper"
                        action == "action:bluetooth" -> "Open Bluetooth"
                        action == "action:settings"  -> "Open Settings"
                        else                         -> action
                    }
                    MappingRow(keyName = keyName, actionLabel = actionLabel, onDelete = { onDelete(code) })
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KmButton(
                text = "+ Add Key Mapping",
                modifier = Modifier.weight(1f),
                focusRequester = addFR,
                onClick = onAddKey
            )
            KmButton(
                text = "Close",
                modifier = Modifier.weight(1f),
                outline = true,
                onClick = onDismiss
            )
        }
    }
}

// ── Waiting for key press ─────────────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WaitingKeyPanel() {
    Column(
        modifier = Modifier
            .width(360.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111111))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text  = "Press any button on your remote",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text  = "Navigation keys (D-pad, Back, Home, Volume) are excluded",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text  = "Press Back to cancel",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF555555)
        )
    }
}

// ── Pick action for captured key ──────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PickActionPanel(
    keyName: String,
    apps: List<AppInfo>,
    onPick: (String) -> Unit
) {
    val firstFR = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .width(440.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111111))
            .padding(24.dp)
    ) {
        Text(
            text  = "Key captured: $keyName",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Text(
            text  = "Choose what this button should do",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Shortcut actions
            item {
                ActionPickRow("Open Search",    "action:search",    firstFR, onPick)
            }
            item { ActionPickRow("Open Bluetooth",  "action:bluetooth", null, onPick) }
            item { ActionPickRow("Open Wallpaper",  "action:wallpaper", null, onPick) }
            item { ActionPickRow("Open Settings",   "action:settings",  null, onPick) }

            // All apps
            item {
                Text(
                    text  = "Launch App",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(apps) { app ->
                ActionPickRow("  ${app.label}", "launch:${app.packageName}", null, onPick)
            }
        }
    }

    LaunchedEffect(Unit) { runCatching { firstFR.requestFocus() } }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActionPickRow(
    label: String,
    action: String,
    focusRequester: FocusRequester?,
    onPick: (String) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val baseMod = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(if (focused) Color.White else Color(0xFF1A1A1A))
        .onFocusChanged { focused = it.isFocused }
        .focusable()
        .onKeyEvent { ev ->
            if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter) && ev.type == KeyEventType.KeyUp) {
                onPick(action); true
            } else false
        }
        .padding(horizontal = 16.dp, vertical = 12.dp)

    Box(
        modifier = if (focusRequester != null)
            baseMod.focusRequester(focusRequester)
        else baseMod
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (focused) Color.Black else TextPrimary
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MappingRow(
    keyName: String,
    actionLabel: String,
    onDelete: () -> Unit
) {
    var delFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text  = keyName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text  = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (delFocused) Color(0xFFEF5350) else Color(0xFF2A2A2A))
                .onFocusChanged { delFocused = it.isFocused }
                .focusable()
                .onKeyEvent { ev ->
                    if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter) && ev.type == KeyEventType.KeyUp) {
                        onDelete(); true
                    } else false
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = if (delFocused) Color.White else Color(0xFF666666),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun KmButton(
    text: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    outline: Boolean = false,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    focused       -> Color.White
                    outline       -> Color(0xFF222222)
                    else          -> Color(0xFF2979FF)
                }
            )
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter) && ev.type == KeyEventType.KeyUp) {
                    onClick(); true
                } else false
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = text,
            style = MaterialTheme.typography.titleMedium,
            color = if (focused) Color.Black else Color.White
        )
    }
}
