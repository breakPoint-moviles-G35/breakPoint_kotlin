package com.breakpoint

import javax.inject.Inject

class AnalyticsRepository @Inject constructor(
    private val api: AnalyticsApi
) {
    suspend fun getFeatureUsageQ7(): List<FeatureUsageDto> = api.getFeatureUsageQ7()
}
