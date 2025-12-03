package com.breakpoint

import retrofit2.http.GET

data class FeatureUsageDto(
    val featureName: String,
    val uses: Int
)

interface AnalyticsApi {
    @GET("analytics/q7")
    suspend fun getFeatureUsageQ7(): List<FeatureUsageDto>
}
