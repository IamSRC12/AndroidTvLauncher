package com.antigravity.tvlauncher.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.antigravity.tvlauncher.ui.theme.TextPrimary
import com.antigravity.tvlauncher.ui.theme.TextSecondary
import com.antigravity.tvlauncher.util.BluetoothHelper
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopBar(
    bluetoothHelper: BluetoothHelper,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    var time by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var netConnected  by remember { mutableStateOf(false) }
    val btEnabled = bluetoothHelper.isEnabled()

    LaunchedEffect(Unit) {
        val tf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val df = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        while (true) {
            val now = Date()
            time = tf.format(now)
            date = df.format(now)
            netConnected = checkNetwork(ctx)
            delay(2000L)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Time + Date
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text  = time,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text  = date,
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }

        // Right: status icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            WifiStatusIcon(connected = netConnected)
            BluetoothStatusIcon(enabled = btEnabled)
        }
    }
}

// ── Network check (Wi-Fi OR Ethernet) ────────────────────────────────────────
private fun checkNetwork(ctx: Context): Boolean {
    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val net = cm.activeNetwork ?: return false
    val cap = cm.getNetworkCapabilities(net) ?: return false
    return cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
           cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
           cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
}

// ── Wi-Fi icon drawn with Canvas ─────────────────────────────────────────────
@Composable
fun WifiStatusIcon(connected: Boolean, modifier: Modifier = Modifier) {
    val color = if (connected) Color.White else Color(0xFF555555)
    Canvas(modifier = modifier.size(22.dp)) {
        val cx = size.width / 2f
        val cy = size.height * 0.68f
        val dotR = size.width * 0.1f

        // Dot
        drawCircle(color = color, radius = dotR, center = Offset(cx, cy))

        // Three arcs
        listOf(0.22f, 0.40f, 0.60f).forEach { factor ->
            val r = size.width * factor
            drawArc(
                color = color,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = size.width * 0.075f, cap = StrokeCap.Round)
            )
        }
    }
}

// ── Bluetooth icon ────────────────────────────────────────────────────────────
@Composable
fun BluetoothStatusIcon(enabled: Boolean, modifier: Modifier = Modifier) {
    val color = if (enabled) Color(0xFF448AFF) else Color(0xFF555555)
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.12f, cap = StrokeCap.Round)
        // Classic Bluetooth B-shape path
        val path = Path().apply {
            moveTo(w * 0.3f, h * 0.3f)
            lineTo(w * 0.7f, h * 0.7f)
            lineTo(w * 0.5f, h * 0.9f)
            lineTo(w * 0.5f, h * 0.1f)
            lineTo(w * 0.7f, h * 0.3f)
            lineTo(w * 0.3f, h * 0.7f)
        }
        drawPath(path = path, color = color, style = stroke)
    }
}
