package com.satvikm.quiet.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.satvikm.quiet.MainActivity
import com.satvikm.quiet.R

private const val FOCUS_RECAP_CHANNEL_ID = "focus_recap"
private const val FOCUS_RECAP_NOTIFICATION_ID = 1001

/** Below API 33 posting a notification needs no runtime grant; POST_NOTIFICATIONS only exists as a checkable permission from 33 on. */
fun isPostNotificationsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/** Falls back to system notification settings for this app — needed once the user has permanently denied the runtime prompt, since re-requesting it then does nothing. */
fun appNotificationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/** Registering a channel twice is a no-op, so this can be called unconditionally on every app start. */
fun createFocusRecapNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        FOCUS_RECAP_CHANNEL_ID,
        context.getString(R.string.focus_recap_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = context.getString(R.string.focus_recap_channel_description)
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

/** Posts a "focus ended" summary. No-ops silently if the user hasn't granted POST_NOTIFICATIONS — callers aren't expected to check first. */
fun postFocusRecapNotification(context: Context, durationMillis: Long) {
    if (!isPostNotificationsGranted(context)) return

    val totalMinutes = durationMillis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val durationText = if (hours > 0) {
        context.getString(R.string.duration_hours_minutes, hours, minutes)
    } else {
        context.getString(R.string.duration_minutes, minutes)
    }

    val contentIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, FOCUS_RECAP_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_focus_tile)
        .setContentTitle(context.getString(R.string.focus_recap_title))
        .setContentText(context.getString(R.string.focus_recap_text, durationText))
        .setContentIntent(contentIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    NotificationManagerCompat.from(context).notify(FOCUS_RECAP_NOTIFICATION_ID, notification)
}
