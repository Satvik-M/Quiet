package com.satvikm.quiet.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Provides no accessibility features of its own; it exists solely so
 * [performGlobalAction] is available to trigger system actions
 * (lock screen, expand notifications) that have no public API.
 */
class GestureAccessibilityService : AccessibilityService() {

    companion object {
        var instance: GestureAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
