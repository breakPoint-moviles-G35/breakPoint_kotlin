package com.breakpoint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class FeatureUsageViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {
    private val _dashboard = MutableStateFlow<FeatureUsageDashboard?>(null)
    val dashboard: StateFlow<FeatureUsageDashboard?> = _dashboard

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadDashboard() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val res = analyticsRepository.getFeatureUsageDashboard()
            res.onSuccess { _dashboard.value = it }
                .onFailure { _error.value = it.message ?: "Error loading feature usage" }
            _loading.value = false
        }
    }
}
