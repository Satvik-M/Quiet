package com.satvikm.quiet.util

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import com.satvikm.quiet.service.GestureAccessibilityService

fun isGestureAccessibilityServiceEnabled(context: Context): Boolean {
    val expected = ComponentName(context, GestureAccessibilityService::class.java)
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false

    val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabled) }
    while (splitter.hasNext()) {
        if (ComponentName.unflattenFromString(splitter.next()) == expected) return true
    }
    return false
}

fun accessibilitySettingsIntent(): Intent =
    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/** Returns false if the accessibility service isn't enabled (so the caller can explain why nothing happened). */
fun lockScreen(): Boolean =
    GestureAccessibilityService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) ?: false

/** Returns false if the accessibility service isn't enabled (so the caller can explain why nothing happened). */
fun expandNotifications(): Boolean =
    GestureAccessibilityService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) ?: false
