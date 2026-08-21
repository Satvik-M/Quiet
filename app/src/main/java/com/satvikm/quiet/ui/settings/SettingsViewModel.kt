package com.satvikm.quiet.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satvikm.quiet.data.apps.AppOverridesRepository
import com.satvikm.quiet.data.apps.AppRepository
import com.satvikm.quiet.data.block.BlocklistRepository
import com.satvikm.quiet.data.focus.FocusScheduleEntity
import com.satvikm.quiet.data.focus.FocusScheduleRepository
import com.satvikm.quiet.data.notifications.NotificationMuteRepository
import com.satvikm.quiet.data.settings.AppFontFamily
import com.satvikm.quiet.data.settings.FontSize
import com.satvikm.quiet.data.settings.GestureSettingsRepository
import com.satvikm.quiet.data.settings.GestureSlot
import com.satvikm.quiet.data.settings.HomeAlignment
import com.satvikm.quiet.data.settings.SettingsRepository
import com.satvikm.quiet.data.settings.ThemeMode
import com.satvikm.quiet.domain.model.LaunchableApp
import com.satvikm.quiet.util.setSystemGrayscale
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockedAppUi(
    val packageName: String,
    val label: String,
    val delaySeconds: Int,
    val dailyOpenLimit: Int?,
    val dailyTimeBudgetMinutes: Int?,
)

data class MutedAppUi(
    val packageName: String,
    val label: String,
)

private val DELAY_OPTIONS = listOf(0, 5, 10, 15, 20, 30)
private val DAILY_LIMIT_OPTIONS: List<Int?> = listOf(null, 1, 3, 5, 10)
private val TIME_BUDGET_OPTIONS: List<Int?> = listOf(null, 15, 30, 60, 120)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val gestureSettingsRepository: GestureSettingsRepository,
    private val blocklistRepository: BlocklistRepository,
    private val notificationMuteRepository: NotificationMuteRepository,
    private val focusScheduleRepository: FocusScheduleRepository,
    private val appRepository: AppRepository,
    private val appOverridesRepository: AppOverridesRepository,
) : ViewModel() {

    private val started = SharingStarted.WhileSubscribed(5_000)

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode.stateIn(viewModelScope, started, ThemeMode.SYSTEM)
    val fontFamily: StateFlow<AppFontFamily> = settingsRepository.fontFamily.stateIn(viewModelScope, started, AppFontFamily.SANS)
    val fontSize: StateFlow<FontSize> = settingsRepository.fontSize.stateIn(viewModelScope, started, FontSize.MEDIUM)
    val alignment: StateFlow<HomeAlignment> = settingsRepository.alignment.stateIn(viewModelScope, started, HomeAlignment.LEFT)
    val showScreenTime: StateFlow<Boolean> = settingsRepository.showScreenTime.stateIn(viewModelScope, started, false)
    val showMutedCount: StateFlow<Boolean> = settingsRepository.showMutedCount.stateIn(viewModelScope, started, false)
    val grayscaleEnabled: StateFlow<Boolean> = settingsRepository.grayscaleEnabled.stateIn(viewModelScope, started, false)
    val focusAutomationEnabled: StateFlow<Boolean> = settingsRepository.focusAutomationEnabled.stateIn(viewModelScope, started, false)
    val notificationDigestEnabled: StateFlow<Boolean> = settingsRepository.notificationDigestEnabled.stateIn(viewModelScope, started, false)
    val onboardingCompleted: StateFlow<Boolean> = settingsRepository.onboardingCompleted.stateIn(viewModelScope, started, false)

    val focusSchedules: StateFlow<List<FocusScheduleEntity>> = focusScheduleRepository.schedules
        .stateIn(viewModelScope, started, emptyList())
    val showFocusStatus: StateFlow<Boolean> = settingsRepository.showFocusStatus.stateIn(viewModelScope, started, false)

    val swipeLeftApp: StateFlow<LaunchableApp?> = gestureApp(GestureSlot.SWIPE_LEFT)
    val swipeRightApp: StateFlow<LaunchableApp?> = gestureApp(GestureSlot.SWIPE_RIGHT)

    val blockedApps: StateFlow<List<BlockedAppUi>> = combine(
        blocklistRepository.blockedApps,
        appRepository.apps,
    ) { blocked, apps ->
        val labelByPackage = apps.associate { it.packageName to it.displayLabel }
        blocked.map { entity ->
            BlockedAppUi(
                packageName = entity.packageName,
                label = labelByPackage[entity.packageName] ?: entity.packageName,
                delaySeconds = entity.delaySeconds,
                dailyOpenLimit = entity.dailyOpenLimit,
                dailyTimeBudgetMinutes = entity.dailyTimeBudgetMinutes,
            )
        }.sortedBy { it.label.lowercase() }
    }.stateIn(viewModelScope, started, emptyList())

    val mutedApps: StateFlow<List<MutedAppUi>> = combine(
        notificationMuteRepository.mutedApps,
        appRepository.apps,
    ) { muted, apps ->
        val labelByPackage = apps.associate { it.packageName to it.displayLabel }
        muted.map { entity ->
            MutedAppUi(
                packageName = entity.packageName,
                label = labelByPackage[entity.packageName] ?: entity.packageName,
            )
        }.sortedBy { it.label.lowercase() }
    }.stateIn(viewModelScope, started, emptyList())

    val hiddenApps: StateFlow<List<LaunchableApp>> = combine(
        appOverridesRepository.overrides,
        appRepository.apps,
    ) { overrides, apps ->
        val byId = apps.associateBy { it.id }
        overrides.filter { it.isHidden }.mapNotNull { byId[it.appId] }.sortedBy { it.displayLabel.lowercase() }
    }.stateIn(viewModelScope, started, emptyList())

    private fun gestureApp(slot: GestureSlot): StateFlow<LaunchableApp?> = combine(
        appRepository.apps,
        gestureSettingsRepository.appIdFor(slot),
    ) { apps, appId -> apps.firstOrNull { it.id == appId } }.stateIn(viewModelScope, started, null)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setFontFamily(family: AppFontFamily) {
        viewModelScope.launch { settingsRepository.setFontFamily(family) }
    }

    fun setFontSize(size: FontSize) {
        viewModelScope.launch { settingsRepository.setFontSize(size) }
    }

    fun setAlignment(alignment: HomeAlignment) {
        viewModelScope.launch { settingsRepository.setAlignment(alignment) }
    }

    fun setShowScreenTime(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowScreenTime(show) }
    }

    fun setShowMutedCount(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowMutedCount(show) }
    }

    fun clearGestureApp(slot: GestureSlot) {
        viewModelScope.launch { gestureSettingsRepository.setAppFor(slot, null) }
    }

    fun cycleDelay(app: BlockedAppUi) {
        val next = DELAY_OPTIONS[(DELAY_OPTIONS.indexOf(app.delaySeconds).coerceAtLeast(0) + 1) % DELAY_OPTIONS.size]
        viewModelScope.launch { blocklistRepository.setBlocked(app.packageName, next, app.dailyOpenLimit, app.dailyTimeBudgetMinutes) }
    }

    fun cycleDailyLimit(app: BlockedAppUi) {
        val next = DAILY_LIMIT_OPTIONS[(DAILY_LIMIT_OPTIONS.indexOf(app.dailyOpenLimit).coerceAtLeast(0) + 1) % DAILY_LIMIT_OPTIONS.size]
        viewModelScope.launch { blocklistRepository.setBlocked(app.packageName, app.delaySeconds, next, app.dailyTimeBudgetMinutes) }
    }

    fun cycleTimeBudget(app: BlockedAppUi) {
        val next = TIME_BUDGET_OPTIONS[(TIME_BUDGET_OPTIONS.indexOf(app.dailyTimeBudgetMinutes).coerceAtLeast(0) + 1) % TIME_BUDGET_OPTIONS.size]
        viewModelScope.launch { blocklistRepository.setBlocked(app.packageName, app.delaySeconds, app.dailyOpenLimit, next) }
    }

    fun removeBlocked(app: BlockedAppUi) {
        viewModelScope.launch { blocklistRepository.unblock(app.packageName) }
    }

    fun removeMuted(app: MutedAppUi) {
        viewModelScope.launch { notificationMuteRepository.unmute(app.packageName) }
    }

    fun unhide(app: LaunchableApp) {
        viewModelScope.launch { appOverridesRepository.setHidden(app, false) }
    }

    fun setGrayscale(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGrayscaleEnabled(enabled) }
        setSystemGrayscale(context, enabled)
    }

    fun addFocusSchedule() {
        viewModelScope.launch { focusScheduleRepository.addDefault() }
    }

    fun setShowFocusStatus(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowFocusStatus(show) }
    }

    fun setFocusAutomationEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setFocusAutomationEnabled(enabled) }
    }

    fun setNotificationDigestEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationDigestEnabled(enabled) }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted(completed) }
    }

    fun setStartHour(schedule: FocusScheduleEntity, hour: Int) {
        viewModelScope.launch { focusScheduleRepository.update(schedule.copy(startHour = hour)) }
    }

    fun setEndHour(schedule: FocusScheduleEntity, hour: Int) {
        viewModelScope.launch { focusScheduleRepository.update(schedule.copy(endHour = hour)) }
    }

    fun toggleDay(schedule: FocusScheduleEntity, dayBitIndex: Int) {
        val bit = 1 shl dayBitIndex
        val next = schedule.daysMask xor bit
        viewModelScope.launch { focusScheduleRepository.update(schedule.copy(daysMask = next)) }
    }

    fun toggleScheduleEnabled(schedule: FocusScheduleEntity) {
        viewModelScope.launch { focusScheduleRepository.update(schedule.copy(enabled = !schedule.enabled)) }
    }

    fun removeSchedule(schedule: FocusScheduleEntity) {
        viewModelScope.launch { focusScheduleRepository.delete(schedule) }
    }
}
