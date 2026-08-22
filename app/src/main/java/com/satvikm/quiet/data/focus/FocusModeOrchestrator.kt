package com.satvikm.quiet.data.focus

import android.content.Context
import android.provider.Telephony
import android.telecom.TelecomManager
import com.satvikm.quiet.data.apps.AppRepository
import com.satvikm.quiet.data.favorites.FavoritesRepository
import com.satvikm.quiet.data.settings.SettingsRepository
import com.satvikm.quiet.util.isWriteSecureSettingsGranted
import com.satvikm.quiet.util.postFocusRecapNotification
import com.satvikm.quiet.util.setSystemGrayscale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies/reverts grayscale and notification auto-muting when a focus
 * schedule starts/ends. Driven by [GestureAccessibilityService], the only
 * component that's reliably alive with no UI open — see its poll loop.
 */
@Singleton
class FocusModeOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val appRepository: AppRepository,
    private val favoritesRepository: FavoritesRepository,
    private val focusAutoMuteRepository: FocusAutoMuteRepository,
) {
    /** Whether grayscale/auto-mute are currently applied — tracked separately from the schedule's own active/inactive state, since automation can be toggled off mid-window and effects must revert immediately rather than waiting for the schedule to end. */
    private var effectsApplied = false

    /** Wall-clock time the current session's effects were applied — used to report session length in the recap notification. Null when no session is running; lives only as long as the process does, which is fine for an informational recap (unlike the commitment-lock timer, nothing depends on this surviving a process death). */
    private var sessionStartMillis: Long? = null

    /** Reconciles applied effects against the schedule state, the automation toggle, and the ad-hoc manual override on every poll tick — call with the schedule's current active/inactive state. */
    suspend fun poll(scheduleActive: Boolean) {
        settingsRepository.clearExpiredManualFocus()
        val manualActive = settingsRepository.manualFocusActive.first()
        val scheduleApplies = scheduleActive && settingsRepository.focusAutomationEnabled.first()
        val shouldApply = manualActive || scheduleApplies
        if (shouldApply == effectsApplied) return
        effectsApplied = shouldApply
        if (shouldApply) enterFocus() else exitFocus()
    }

    private suspend fun enterFocus() {
        sessionStartMillis = System.currentTimeMillis()
        if (isWriteSecureSettingsGranted(context)) {
            setSystemGrayscale(context, true)
        }
        val allApps = appRepository.apps.first()
        val favoriteIds = favoritesRepository.favorites.first().map { it.appId }.toSet()
        val favoritePackages = allApps.filter { it.id in favoriteIds }.map { it.packageName }.toSet()
        val exempt = favoritePackages + defaultDialerAndSmsPackages() + context.packageName
        val target = allApps.map { it.packageName }.toSet() - exempt
        focusAutoMuteRepository.setAutoMuted(target)
    }

    private suspend fun exitFocus() {
        if (isWriteSecureSettingsGranted(context)) {
            setSystemGrayscale(context, settingsRepository.grayscaleEnabled.first())
        }
        focusAutoMuteRepository.clearAll()

        val startedAt = sessionStartMillis
        sessionStartMillis = null
        if (startedAt != null && settingsRepository.focusRecapEnabled.first()) {
            postFocusRecapNotification(context, System.currentTimeMillis() - startedAt)
        }
    }

    private fun defaultDialerAndSmsPackages(): Set<String> {
        val dialer = context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
        val sms = Telephony.Sms.getDefaultSmsPackage(context)
        return setOfNotNull(dialer, sms)
    }
}
