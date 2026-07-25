package com.antigravity.tvlauncher.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class BluetoothHelper(private val context: Context) {

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun isSupported(): Boolean = adapter != null
    fun isEnabled(): Boolean   = adapter?.isEnabled == true

    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!isSupported() || !hasPermission()) return emptyList()
        return try { adapter?.bondedDevices?.toList() ?: emptyList() }
        catch (e: SecurityException) { emptyList() }
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext false
        var socket: BluetoothSocket? = null
        try {
            adapter?.cancelDiscovery()
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            socket.close()
            true
        } catch (e: IOException) {
            runCatching { socket?.close() }
            false
        } catch (e: SecurityException) { false }
    }

    @SuppressLint("MissingPermission")
    fun getConnectedDeviceName(): String? {
        if (!isSupported() || !isEnabled() || !hasPermission()) return null
        return try {
            val bonded = adapter?.bondedDevices ?: return null
            for (dev in bonded) {
                val isConnected = try {
                    val method = dev.javaClass.getMethod("isConnected")
                    method.invoke(dev) as? Boolean ?: false
                } catch (_: Exception) { false }
                if (isConnected) {
                    return dev.name ?: "Connected Device"
                }
            }
            null
        } catch (_: Exception) { null }
    }

    /**
     * Sends broadcast intent compatible with AndroidTVBluetooth (saihgupr/AndroidTVBluetooth).
     * Action: "com.saihgupr.btcontrol.ACTION_CONNECT" or "com.saihgupr.btcontrol.ACTION_DISCONNECT"
     */
    fun sendBtControlBroadcast(action: String, name: String? = null, address: String? = null) {
        try {
            val intent = Intent(action).apply {
                setClassName("com.saihgupr.btcontrol", "com.saihgupr.btcontrol.BluetoothControlReceiver")
                if (!name.isNullOrBlank()) putExtra("name", name)
                if (!address.isNullOrBlank()) putExtra("address", address)
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {}
    }

    fun connectViaBtControl(deviceName: String) {
        sendBtControlBroadcast("com.saihgupr.btcontrol.ACTION_CONNECT", name = deviceName)
    }

    fun disconnectViaBtControl(deviceName: String) {
        sendBtControlBroadcast("com.saihgupr.btcontrol.ACTION_DISCONNECT", name = deviceName)
    }

    fun openBluetoothSettings(): Intent =
        Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun openSystemSettings(): Intent =
        Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
