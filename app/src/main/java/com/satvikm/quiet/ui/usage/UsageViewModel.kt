package com.satvikm.quiet.ui.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satvikm.quiet.data.apps.AppRepository
import com.satvikm.quiet.data.settings.SettingsRepository
import com.satvikm.quiet.data.usage.AppUsage
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

enum class UsagePeriod(val days: Int) { WEEK(7), MONTH(30) }

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

    private val _period = MutableStateFlow(UsagePeriod.WEEK)
    val period: StateFlow<UsagePeriod> = _period.asStateFlow()

    val appLabels: StateFlow<Map<String, String>> = appRepository.apps
        .map { apps -> apps.associate { it.packageName to it.displayLabel } }
        .stateIn(viewModelScope, started, emptyMap())

    val range: StateFlow<List<Pair<LocalDate, DailyUsage>>> = combine(refreshTrigger, period) { _, p -> p }
        .map { usageRepository.lastDays(it.days) }
        .stateIn(viewModelScope, started, emptyList())

    val topApps: StateFlow<List<AppUsage>> = range
        .map { UsageRepository.topApps(it) }
        .stateIn(viewModelScope, started, emptyList())

    val avgMillisPerDay: StateFlow<Long> = range
        .map { days -> if (days.isEmpty()) 0L else days.sumOf { it.second.totalMillis } / days.size }
        .stateIn(viewModelScope, started, 0L)

    val avgUnlocksPerDay: StateFlow<Int> = range
        .map { days -> if (days.isEmpty()) 0 else days.sumOf { it.second.unlockCount } / days.size }
        .stateIn(viewModelScope, started, 0)

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

    fun setPeriod(period: UsagePeriod) {
        _period.value = period
    }

    fun setDailyGoalMinutes(minutes: Int?) {
        viewModelScope.launch { settingsRepository.setDailyGoalMinutes(minutes) }
    }
}
