package com.satvikm.quiet.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.satvikm.quiet.data.block.BlockedAppEntity
import com.satvikm.quiet.data.block.BlocklistRepository
import com.satvikm.quiet.ui.friction.FrictionActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Two unrelated jobs share this one service so the user only has to grant
 * Accessibility once:
 *  1. [performGlobalAction] for lock screen / expand notifications (no
 *     public API for either).
 *  2. Watching window-state-changed events to intercept launches of
 *     blocklisted apps with [FrictionActivity].
 */
@AndroidEntryPoint
class GestureAccessibilityService : AccessibilityService() {

    @Inject lateinit var blocklistRepository: BlocklistRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var blockedApps: Map<String, BlockedAppEntity> = emptyMap()
    private val graceUntilByPackage = mutableMapOf<String, Long>()

    private var lastPackageName: String? = null
    private var lastEventUptimeMillis = 0L

    companion object {
        var instance: GestureAccessibilityService? = null
            private set

        private const val DEBOUNCE_MS = 300L
        const val GRACE_DURATION_MS = 2 * 60_000L
    }

    /** Called by FrictionActivity when the user taps Continue, so the just-revealed app isn't immediately re-blocked. */
    fun grantGrace(packageName: String) {
        graceUntilByPackage[packageName] = SystemClock.uptimeMillis() + GRACE_DURATION_MS
    }

    override fun onServiceConnected() {
        instance = this
        blocklistRepository.blockedApps
            .onEach { blockedApps = it.associateBy { entity -> entity.packageName } }
            .launchIn(serviceScope)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName || packageName == "com.android.systemui") return

        val now = SystemClock.uptimeMillis()
        if (packageName == lastPackageName && now - lastEventUptimeMillis < DEBOUNCE_MS) return
        lastPackageName = packageName
        lastEventUptimeMillis = now

        if (blockedApps[packageName] == null) return
        val graceUntil = graceUntilByPackage[packageName]
        if (graceUntil != null && now < graceUntil) return

        startActivity(
            Intent(this, FrictionActivity::class.java)
                .putExtra(FrictionActivity.EXTRA_PACKAGE_NAME, packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        serviceScope.cancel()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        serviceScope.cancel()
        super.onDestroy()
    }
}
