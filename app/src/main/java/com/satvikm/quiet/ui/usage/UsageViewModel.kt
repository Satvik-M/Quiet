package com.satvikm.quiet.ui.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satvikm.quiet.data.apps.AppRepository
import com.satvikm.quiet.data.settings.SettingsRepository
import com.satvikm.quiet.data.usage.DailyUsage
import com.satvikm.quiet.data.usage.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class UsageViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val settingsRepository: SettingsRepository,
    appRepository: AppRepository,
) : ViewModel() {

    private val started = SharingStarted.WhileSubscribed(5_000)

    private val _daily = MutableStateFlow(DailyUsage(totalMillis = 0L, unlockCount = 0, perApp = emptyList()))
    val daily: StateFlow<DailyUsage> = _daily.asStateFlow()

    private val _usageAccessGranted = MutableStateFlow(usageRepository.isUsageAccessGranted())
    val usageAccessGranted: StateFlow<Boolean> = _usageAccessGranted.asStateFlow()

    private val refreshTrigger = MutableStateFlow(0)

    val appLabels: StateFlow<Map<String, String>> = appRepository.apps
        .map { apps -> apps.associate { it.packageName to it.displayLabel } }
        .stateIn(viewModelScope, started, emptyMap())

    val weekly: StateFlow<List<Pair<LocalDate, DailyUsage>>> = refreshTrigger
        .map { usageRepository.lastDays(7) }
        .stateIn(viewModelScope, started, emptyList())

    val dailyGoalMinutes: StateFlow<Int?> = settingsRepository.dailyGoalMinutes
        .stateIn(viewModelScope, started, null)

    val streak: StateFlow<Int> = combine(refreshTrigger, dailyGoalMinutes) { _, goal -> goal }
        .map { goal -> goal?.let { usageRepository.streak(it) } ?: 0 }
        .stateIn(viewModelScope, started, 0)

    init {
        refresh()
    }

    fun refresh() {
        _usageAccessGranted.value = usageRepository.isUsageAccessGranted()
        viewModelScope.launch { _daily.value = usageRepository.today() }
        refreshTrigger.value++
    }

    fun setDailyGoalMinutes(minutes: Int?) {
        viewModelScope.launch { settingsRepository.setDailyGoalMinutes(minutes) }
    }
}
