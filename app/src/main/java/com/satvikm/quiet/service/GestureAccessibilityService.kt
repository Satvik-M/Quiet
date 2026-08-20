package com.satvikm.quiet.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.satvikm.quiet.data.block.BlockedAppEntity
import com.satvikm.quiet.data.block.BlocklistRepository
import com.satvikm.quiet.data.focus.FocusModeOrchestrator
import com.satvikm.quiet.data.focus.FocusScheduleRepository
import com.satvikm.quiet.data.settings.SettingsRepository
import com.satvikm.quiet.ui.friction.FrictionActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    @Inject lateinit var focusScheduleRepository: FocusScheduleRepository
    @Inject lateinit var focusModeOrchestrator: FocusModeOrchestrator
    @Inject lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var blockedApps: Map<String, BlockedAppEntity> = emptyMap()
    private val graceUntilByPackage = mutableMapOf<String, Long>()

    private var lastPackageName: String? = null
    private var lastEventUptimeMillis = 0L

    companion object {
        var instance: GestureAccessibilityService? = null
            private set

        private const val TAG = "GestureAccessibilityService"
        private const val DEBOUNCE_MS = 300L
        private const val FOCUS_POLL_INTERVAL_MS = 60_000L
    }

    /** Called by FrictionActivity when the user taps Continue, so the just-revealed app isn't immediately re-blocked. */
    fun grantGrace(packageName: String) {
        graceUntilByPackage[packageName] = System.currentTimeMillis() + BlocklistRepository.GRACE_DURATION_MS
        serviceScope.launch { blocklistRepository.grantGrace(packageName) }
    }

    override fun onServiceConnected() {
        instance = this
        blocklistRepository.blockedApps
            .onEach { blockedApps = it.associateBy { entity -> entity.packageName } }
            .launchIn(serviceScope)

        serviceScope.launch { graceUntilByPackage.putAll(blocklistRepository.activeGrace()) }

        settingsRepository.focusAutomationEnabled
            .onEach { pollFocusNow() }
            .launchIn(serviceScope)

        serviceScope.launch {
            while (isActive) {
                pollFocusNow()
                delay(FOCUS_POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun pollFocusNow() {
        try {
            focusModeOrchestrator.poll(focusScheduleRepository.isFocusActiveNow())
        } catch (e: Exception) {
            Log.w(TAG, "Focus mode poll failed", e)
        }
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
        if (graceUntil != null && System.currentTimeMillis() < graceUntil) return

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
