package com.antigravity.tvlauncher.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.antigravity.tvlauncher.MainActivity

/**
 * Receives the BOOT_COMPLETED broadcast and relaunches the launcher
 * so it is ready at the home screen after device restart.
 *
 * Declared in AndroidManifest.xml with RECEIVE_BOOT_COMPLETED permission.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { context.startActivity(launchIntent) } catch (_: Exception) { }
        }
    }
}
