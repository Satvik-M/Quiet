package com.satvikm.quiet.ui.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satvikm.quiet.data.apps.AppRepository
import com.satvikm.quiet.data.usage.DailyUsage
import com.satvikm.quiet.data.usage.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UsageViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    appRepository: AppRepository,
) : ViewModel() {

    private val started = SharingStarted.WhileSubscribed(5_000)

    private val _daily = MutableStateFlow(DailyUsage(totalMillis = 0L, unlockCount = 0, perApp = emptyList()))
    val daily: StateFlow<DailyUsage> = _daily.asStateFlow()

    private val _usageAccessGranted = MutableStateFlow(usageRepository.isUsageAccessGranted())
    val usageAccessGranted: StateFlow<Boolean> = _usageAccessGranted.asStateFlow()

    val appLabels: StateFlow<Map<String, String>> = appRepository.apps
        .map { apps -> apps.associate { it.packageName to it.displayLabel } }
        .stateIn(viewModelScope, started, emptyMap())

    init {
        refresh()
    }

    fun refresh() {
        _usageAccessGranted.value = usageRepository.isUsageAccessGranted()
        viewModelScope.launch { _daily.value = usageRepository.today() }
    }
}
