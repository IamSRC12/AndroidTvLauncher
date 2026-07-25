package com.antigravity.tvlauncher.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import coil.compose.rememberAsyncImagePainter
import com.antigravity.tvlauncher.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WallpaperPicker(
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current

    val solidColors = listOf(
        "#000000" to "Pitch Black",
        "#0A1128" to "Navy",
        "#0B1A10" to "Forest",
        "#1C0F1A" to "Plum",
        "#10141D" to "Midnight",
        "#1F1610" to "Mocha",
        "#121212" to "Charcoal",
        "#1C1C24" to "Slate"
    )

    val sampleImages = listOf(
        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&q=80",
        "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?w=800&q=80",
        "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=800&q=80",
        "https://images.unsplash.com/photo-1541701494587-cb58502866ab?w=800&q=80",
        "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&q=80",
        "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=800&q=80",
        "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&q=80",
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80"
    )

    val imgPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                ctx.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            onSelected(uri.toString())
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .onKeyEvent { ev ->
                if (ev.key == Key.Back && ev.type == KeyEventType.KeyUp) { onDismiss(); true } else false
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF111111))
                .padding(24.dp)
        ) {
            Text(
                text  = "Choose Wallpaper",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(260.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement   = Arrangement.spacedBy(10.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text  = "Curated 4K Wallpapers",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                }
                items(sampleImages) { url ->
                    WpImageTile(url = url, onClick = { onSelected(url); onDismiss() })
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text  = "Solid Dark Colors",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(solidColors) { (hex, _) ->
                    WpColorTile(hex = hex, onClick = { onSelected(hex); onDismiss() })
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WpButton(text = "Choose from Gallery", onClick = { runCatching { imgPicker.launch("image/*") } }, modifier = Modifier.weight(1f))
                WpButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f), outline = true)
            }
        }
    }
}

@Composable
private fun WpImageTile(url: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .aspectRatio(1.78f)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E1E1E))
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) LocalAccentColor.current else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter) && ev.type == KeyEventType.KeyUp) {
                    onClick(); true
                } else false
            }
    ) {
        Image(
            painter = rememberAsyncImagePainter(url),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = if (focused) 1f else 0.75f
        )
    }
}

@Composable
private fun WpColorTile(hex: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Black }
    Box(
        modifier = Modifier
            .aspectRatio(1.78f)
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Color.White else Color(0x33FFFFFF),
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter) && ev.type == KeyEventType.KeyUp) {
                    onClick(); true
                } else false
            }
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WpButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outline: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    focused -> Color.White
                    outline -> Color(0xFF222222)
                    else -> LocalAccentColor.current
                }
            )
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
