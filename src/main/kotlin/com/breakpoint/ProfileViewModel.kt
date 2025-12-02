package com.breakpoint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _featureUsage = MutableStateFlow<List<FeatureUsageDto>>(emptyList())
    val featureUsage: StateFlow<List<FeatureUsageDto>> = _featureUsage

    private val _loadingUsage = MutableStateFlow(false)
    val loadingUsage: StateFlow<Boolean> = _loadingUsage

    private val _usageError = MutableStateFlow<String?>(null)
    val usageError: StateFlow<String?> = _usageError

    fun loadFeatureUsage() {
        viewModelScope.launch {
            _loadingUsage.value = true
            _usageError.value = null
            try {
                _featureUsage.value = analyticsRepository.getFeatureUsageQ7()
            } catch (t: Throwable) {
                _usageError.value = t.message ?: "Error loading analytics"
            } finally {
                _loadingUsage.value = false
            }
        }
    }
}
