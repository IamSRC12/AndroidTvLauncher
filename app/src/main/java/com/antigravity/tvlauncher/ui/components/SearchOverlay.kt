package com.antigravity.tvlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.antigravity.tvlauncher.data.AppInfo
import com.antigravity.tvlauncher.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchOverlay(
    apps: List<AppInfo>,
    onLaunch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val tfr = remember { FocusRequester() }

    val filtered = remember(query, apps) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    LaunchedEffect(Unit) { runCatching { tfr.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .onKeyEvent { ev ->
                if (ev.key == Key.Back && ev.type == KeyEventType.KeyUp) { onDismiss(); true } else false
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 40.dp)
        ) {
            // ── Search bar ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF1A1A1A)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(24.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(tfr),
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                    cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                text  = "Search apps…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF666666)
                            )
                        }
                        inner()
                    }
                )
                Spacer(Modifier.width(24.dp))
            }

            Spacer(Modifier.height(28.dp))

            // ── Results ────────────────────────────────────────────────────
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text  = "No apps match \"$query\"",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement   = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        AppCard(
                            app         = app,
                            onClick     = { onLaunch(app.packageName) },
                            onLongPress = { onLaunch(app.packageName) }
                        )
                    }
                }
            }
        }
    }
}
