package com.antigravity.tvlauncher.ui.components

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import com.antigravity.tvlauncher.ui.theme.*

data class InputSource(
    val id: String,
    val name: String,
    val typeName: String,
    val isHdmi: Boolean
)

/**
 * Overlay showing available HDMI & TV Inputs for switching.
 * Queries system TvInputManager or provides standard HDMI 1/2/3 fallbacks.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun InputsOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val accentColor = LocalAccentColor.current

    val inputSources = remember(ctx) {
        val manager = ctx.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
        val list = try {
            manager?.tvInputList?.mapNotNull { info ->
                val label = info.loadLabel(ctx).toString()
                val isHdmi = info.type == TvInputInfo.TYPE_HDMI
                val typeName = when (info.type) {
                    TvInputInfo.TYPE_HDMI -> "HDMI Input"
                    TvInputInfo.TYPE_TUNER -> "TV Tuner"
                    TvInputInfo.TYPE_COMPONENT -> "Component"
                    TvInputInfo.TYPE_COMPOSITE -> "Composite / AV"
                    else -> "External Input"
                }
                InputSource(
                    id = info.id,
                    name = if (label.isNotBlank()) label else typeName,
                    typeName = typeName,
                    isHdmi = isHdmi
                )
            } ?: emptyList()
        } catch (_: Exception) { emptyList() }

        if (list.isNotEmpty()) list
        else listOf(
            InputSource("hdmi_1", "HDMI 1", "HDMI Input", true),
            InputSource("hdmi_2", "HDMI 2", "HDMI Input", true),
            InputSource("hdmi_3", "HDMI 3", "HDMI Input", true),
            InputSource("av_1",   "AV / Composite", "Composite Input", false)
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .onKeyEvent { ev ->
                if ((ev.key == Key.Escape || ev.key == Key.Back) && ev.type == KeyEventType.KeyUp) {
                    onDismiss(); true
                } else false
            }
    ) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF161616))
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Input,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Select Input Source",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Text(
                text = "Switch to an HDMI or AV input channel",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            TvLazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(inputSources) { source ->
                    InputSourceRow(
                        source = source,
                        accentColor = accentColor,
                        onSelect = {
                            onDismiss()
                            try {
                                val uri = TvContract.buildChannelUriForPassthroughInput(source.id)
                                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                ctx.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = android.net.Uri.parse("passthrough://${source.id}")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    ctx.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun InputSourceRow(
    source: InputSource,
    accentColor: Color,
    onSelect: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Color(0x33FFFFFF) else Color(0x1AFFFFFF))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                    && ev.type == KeyEventType.KeyUp
                ) { onSelect(); true } else false
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = if (source.isHdmi) Icons.Default.Input else Icons.Default.Tv,
            contentDescription = null,
            tint = if (focused) accentColor else TextPrimary,
            modifier = Modifier.size(22.dp)
        )

        Column {
            Text(
                text = source.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (focused) TextPrimary else TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = source.typeName,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}
