package com.satvikm.quiet.ui.digest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satvikm.quiet.data.apps.AppRepository
import com.satvikm.quiet.data.notifications.MutedNotificationEntity
import com.satvikm.quiet.data.notifications.NotificationMuteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DigestViewModel @Inject constructor(
    private val notificationMuteRepository: NotificationMuteRepository,
    appRepository: AppRepository,
) : ViewModel() {

    private val started = SharingStarted.WhileSubscribed(5_000)
    private val refreshTrigger = MutableStateFlow(0)

    val appLabels: StateFlow<Map<String, String>> = appRepository.apps
        .map { apps -> apps.associate { it.packageName to it.displayLabel } }
        .stateIn(viewModelScope, started, emptyMap())

    val entries: StateFlow<List<MutedNotificationEntity>> = refreshTrigger
        .map { notificationMuteRepository.digestToday() }
        .stateIn(viewModelScope, started, emptyList())

    init {
        refresh()
    }

    fun refresh() {
        refreshTrigger.value++
    }
}
