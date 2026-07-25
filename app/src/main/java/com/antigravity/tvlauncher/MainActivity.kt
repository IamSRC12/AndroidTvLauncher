package com.antigravity.tvlauncher

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.antigravity.tvlauncher.ui.screens.HomeScreen
import com.antigravity.tvlauncher.ui.theme.TvLauncherTheme
import com.antigravity.tvlauncher.ui.viewmodel.HomeViewModel
import com.antigravity.tvlauncher.util.BluetoothHelper
import com.antigravity.tvlauncher.util.IdleDetector
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: HomeViewModel by viewModels()
    private lateinit var btHelper: BluetoothHelper
    private lateinit var idleDetector: IdleDetector

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions handled gracefully per-feature */ }

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        btHelper = BluetoothHelper(this)
        requestRuntimePermissions()

        idleDetector = IdleDetector(
            scope       = lifecycleScope,
            idleMinutes = 3,
            onIdle      = { vm.activateScreensaver() }
        )
        idleDetector.start()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.screensaverMins.collect { mins ->
                    idleDetector.setMinutes(mins)
                }
            }
        }

        setContent {
            val themePreset by vm.themePreset.collectAsState()

            TvLauncherTheme(preset = themePreset) {
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

    override fun onResume() {
        super.onResume()
        vm.dismissScreensaver()
        idleDetector.start()
    }

    override fun onStop() {
        super.onStop()
        idleDetector.stop()
    }

    /**
     * Remote key remapping: intercept key events BEFORE views consume them.
     * Every key press updates idle detector timestamp with zero coroutine churn.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            idleDetector.resetTimer()
            if (vm.screenSaverActive) {
                vm.dismissScreensaver()
            }

            if (!vm.showKeyMapper && vm.handleRemappedKey(event.keyCode)) {
                return true
            }
        }
        return try {
            super.dispatchKeyEvent(event)
        } catch (_: Exception) {
            true
        }
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
        if (perms.isNotEmpty()) {
            try { permLauncher.launch(perms.toTypedArray()) } catch (_: Exception) {}
        }
    }
}
