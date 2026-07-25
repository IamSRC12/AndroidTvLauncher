package com.antigravity.tvlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.antigravity.tvlauncher.data.AppInfo
import com.antigravity.tvlauncher.ui.theme.*

/**
 * Overlay dialog allowing the user to select/unselect apps to add to Favorites.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FavPickerOverlay(
    allApps: List<AppInfo>,
    onToggleFavorite: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current

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
                .width(440.dp)
                .heightIn(max = 520.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF161616))
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Add Apps to Favorites",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Text(
                text = "Select apps to pin to your Favorites row",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            TvLazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(allApps, key = { it.packageName }) { app ->
                    FavItemRow(
                        app = app,
                        accentColor = accentColor,
                        onToggle = { onToggleFavorite(app.packageName) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FavItemRow(
    app: AppInfo,
    accentColor: Color,
    onToggle: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Color(0x33FFFFFF) else Color(0x1AFFFFFF))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                    && ev.type == KeyEventType.KeyUp
                ) { onToggle(); true } else false
            }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(app.icon)
                    .crossfade(true)
                    .build(),
                contentDescription = app.displayLabel,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Text(
                text = app.displayLabel,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Icon(
            imageVector = if (app.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
            contentDescription = null,
            tint = if (app.isFavorite) accentColor else TextSecondary,
            modifier = Modifier.size(24.dp)
        )
    }
}
