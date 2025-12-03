package com.breakpoint

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

data class FeatureUsageDto(
    val featureName: String,
    val uses: Int
)

interface AnalyticsApi {
    @GET("analytics/q7")
    suspend fun getFeatureUsageQ7(): List<FeatureUsageDto>

    // Para Juan David (Sprint 3): obtener uso de una funcionalidad específica
    @GET("analytics/feature-usage")
    suspend fun getFeatureUsage(@Query("feature") feature: String): Int

    // Q20 - Forecast de demanda por espacio (siguientes 7 días)
    @GET("analytics/q20/{spaceId}")
    suspend fun getDemandForecast(
        @Path("spaceId") spaceId: String
    ): List<DemandForecastDayDto>
}
