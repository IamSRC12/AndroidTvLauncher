package com.antigravity.tvlauncher

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.antigravity.tvlauncher.ui.screens.HomeScreen
import com.antigravity.tvlauncher.ui.theme.TvLauncherTheme
import com.antigravity.tvlauncher.ui.viewmodel.HomeViewModel
import com.antigravity.tvlauncher.util.BluetoothHelper

class MainActivity : ComponentActivity() {

    private val vm: HomeViewModel by viewModels()
    private lateinit var btHelper: BluetoothHelper

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions handled gracefully per-feature */ }

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        btHelper = BluetoothHelper(this)
        requestRuntimePermissions()

        setContent {
            TvLauncherTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        viewModel         = vm,
                        bluetoothHelper   = btHelper,
                        onUninstallRequest = { intent ->
                            runCatching { startActivity(intent) }
                        }
                    )
                }
            }
        }
    }

    /**
     * Remote key remapping: intercept key events BEFORE views consume them.
     * IMPORTANT: Home / Volume keys are reserved by the OS and cannot be intercepted here.
     * Other keys (coloured buttons, channel, numeric, etc.) work correctly.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Only process key-down to avoid double-firing
        if (event.action == KeyEvent.ACTION_DOWN) {
            // Don't remap if a dialog is waiting for a key capture
            if (!vm.showKeyMapper && vm.handleRemappedKey(event.keyCode)) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun requestRuntimePermissions() {
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(android.Manifest.permission.BLUETOOTH_CONNECT)
                add(android.Manifest.permission.BLUETOOTH_SCAN)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(android.Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (perms.isNotEmpty()) permLauncher.launch(perms.toTypedArray())
    }
}
