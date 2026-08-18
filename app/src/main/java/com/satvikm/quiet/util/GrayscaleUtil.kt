package com.satvikm.quiet.util

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

const val WRITE_SECURE_SETTINGS_PERMISSION = "android.permission.WRITE_SECURE_SETTINGS"

/** ADB command the user runs once to grant [WRITE_SECURE_SETTINGS_PERMISSION] — Settings has no UI for it. */
fun grayscaleGrantCommand(context: Context): String =
    "adb shell pm grant ${context.packageName} $WRITE_SECURE_SETTINGS_PERMISSION"

fun isWriteSecureSettingsGranted(context: Context): Boolean =
    context.checkSelfPermission(WRITE_SECURE_SETTINGS_PERMISSION) == PackageManager.PERMISSION_GRANTED

/** Returns false if the permission isn't actually held (e.g. revoked since the last check). */
fun setSystemGrayscale(context: Context, enabled: Boolean): Boolean {
    if (!isWriteSecureSettingsGranted(context)) return false
    return try {
        Settings.Secure.putInt(
            context.contentResolver,
            "accessibility_display_daltonizer_enabled",
            if (enabled) 1 else 0,
        )
        // 0 = simulate monochromacy (the daltonizer mode that reads as plain grayscale).
        Settings.Secure.putInt(context.contentResolver, "accessibility_display_daltonizer", 0)
        true
    } catch (e: SecurityException) {
        false
    }
}
