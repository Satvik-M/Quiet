package com.satvikm.quiet.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

fun appInfoIntent(packageName: String): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private const val UNINSTALL_RESULT_ACTION = "com.satvikm.quiet.action.UNINSTALL_RESULT"

/**
 * Requests uninstall via PackageInstaller (ACTION_UNINSTALL_PACKAGE is
 * deprecated). The system still shows its own confirmation dialog; we don't
 * need the result since the drawer already updates live via
 * LauncherApps.Callback once the uninstall actually completes.
 */
fun requestUninstall(context: Context, packageName: String) {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context, intent: Intent) {
            receiverContext.unregisterReceiver(this)
        }
    }
    ContextCompat.registerReceiver(
        context,
        receiver,
        IntentFilter(UNINSTALL_RESULT_ACTION),
        ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        packageName.hashCode(),
        Intent(UNINSTALL_RESULT_ACTION).setPackage(context.packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )
    context.packageManager.packageInstaller.uninstall(packageName, pendingIntent.intentSender)
}
