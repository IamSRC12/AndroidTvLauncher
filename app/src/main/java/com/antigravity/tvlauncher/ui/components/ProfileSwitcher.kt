package com.antigravity.tvlauncher.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import com.antigravity.tvlauncher.data.Profile
import com.antigravity.tvlauncher.ui.theme.*

/**
 * Profile switcher overlay — shows all profiles as large cards.
 * Selecting a profile switches to it.
 * Kids mode profile tints the accent to warm yellow and triggers filtering in the UI.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfileSwitcher(
    profiles: List<Profile>,
    activeProfileId: String,
    onSwitchProfile: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text       = "Switch Profile",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )

            Text(
                text  = "Select a profile to switch to",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            TvLazyRow(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding        = PaddingValues(horizontal = 48.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ProfileCard(
                        profile         = profile,
                        isActive        = profile.id == activeProfileId,
                        onSelect        = {
                            onSwitchProfile(profile.id)
                            onDismiss()
                        }
                    )
                }
            }

            Text(
                text  = "Press Back to cancel",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.5f)
            )
        }
    }
}

// ── Individual profile card ────────────────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileCard(
    profile: Profile,
    isActive: Boolean,
    onSelect: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val accentColor = LocalAccentColor.current

    val avatarColor = try {
        Color(android.graphics.Color.parseColor(profile.avatarColorHex))
    } catch (_: Exception) { accentColor }

    val borderColor = when {
        isActive -> avatarColor
        focused  -> avatarColor.copy(alpha = 0.6f)
        else     -> Color.Transparent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) Color(0x33FFFFFF) else Color(0x22FFFFFF))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if ((ev.key == Key.DirectionCenter || ev.key == Key.Enter)
                    && ev.type == KeyEventType.KeyUp
                ) { onSelect(); true } else false
            }
            .padding(24.dp)
    ) {
        // Avatar circle
        Box(contentAlignment = Alignment.Center) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(avatarColor)
            ) {
                if (profile.isKidsMode) {
                    Icon(
                        imageVector        = Icons.Default.ChildCare,
                        contentDescription = "Kids",
                        tint               = Color.Black,
                        modifier           = Modifier.size(36.dp)
                    )
                } else {
                    Text(
                        text       = profile.initials,
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.Black
                    )
                }
            }

            // Active check badge
            if (isActive) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                ) {
                    Icon(
                        imageVector        = Icons.Default.Check,
                        contentDescription = "Active",
                        tint               = Color.White,
                        modifier           = Modifier.size(14.dp)
                    )
                }
            }
        }

        Text(
            text       = profile.name,
            style      = MaterialTheme.typography.titleSmall,
            color      = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        if (profile.isKidsMode) {
            Box(
                modifier = Modifier
                    .background(Color(0x33FFD600), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text  = "Kids Mode",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFD600),
                    fontSize = 10.sp
                )
            }
        }

        if (profile.pin != null) {
            Text(
                text  = "🔒 PIN protected",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}
