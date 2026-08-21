package com.satvikm.quiet.ui.onboarding

import androidx.lifecycle.ViewModel
import com.satvikm.quiet.data.usage.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
) : ViewModel() {

    private val _usageAccessGranted = MutableStateFlow(usageRepository.isUsageAccessGranted())
    val usageAccessGranted: StateFlow<Boolean> = _usageAccessGranted.asStateFlow()

    fun refresh() {
        _usageAccessGranted.value = usageRepository.isUsageAccessGranted()
    }
}
