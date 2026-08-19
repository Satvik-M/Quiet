package com.satvikm.quiet.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.satvikm.quiet.data.focus.FocusAutoMuteRepository
import com.satvikm.quiet.data.notifications.NotificationMuteRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Cancels notifications from apps the user has muted, immediately and
 * silently (no heads-up, no sound) — we cannot repost another app's
 * notification faithfully, so this is strictly suppression, not a digest.
 * The muted set is the union of the user's own mute list and whatever
 * [com.satvikm.quiet.data.focus.FocusModeOrchestrator] has auto-muted for
 * the current focus window, so neither source has to know about the other.
 */
@AndroidEntryPoint
class NotificationFilterService : NotificationListenerService() {

    @Inject lateinit var muteRepository: NotificationMuteRepository
    @Inject lateinit var focusAutoMuteRepository: FocusAutoMuteRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var mutedPackages: Set<String> = emptySet()

    override fun onListenerConnected() {
        combine(muteRepository.mutedApps, focusAutoMuteRepository.autoMuted) { manual, auto ->
            manual.map { it.packageName }.toSet() + auto.map { it.packageName }.toSet()
        }.onEach { mutedPackages = it }.launchIn(serviceScope)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val packageName = sbn?.packageName ?: return
        if (packageName !in mutedPackages) return
        cancelNotification(sbn.key)
        serviceScope.launch { muteRepository.recordMuted(packageName) }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
