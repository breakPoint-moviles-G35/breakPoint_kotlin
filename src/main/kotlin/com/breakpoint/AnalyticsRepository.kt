package com.breakpoint

import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap

class AnalyticsRepository @Inject constructor(
    private val api: AnalyticsApi
) {
    suspend fun getFeatureUsageQ7(): List<FeatureUsageDto> = api.getFeatureUsageQ7()

    /**
     * Juan David – Sprint 3
     * Multi-threading strategy: fetch per-feature usage in parallel (extend, share, reviews, chat)
     * to answer BQ "Least used features".
     */
    suspend fun getFeatureUsageDashboard(): Result<FeatureUsageDashboard> = withContext(Dispatchers.IO) {
        return@withContext try {
            coroutineScope {
                val extendDef = async(Dispatchers.IO) { api.getFeatureUsage("extend") }
                val shareDef = async(Dispatchers.IO) { api.getFeatureUsage("share") }
                val reviewsDef = async(Dispatchers.IO) { api.getFeatureUsage("reviews") }
                val chatDef = async(Dispatchers.IO) { api.getFeatureUsage("chat") }

                val extendCount = extendDef.await()
                val shareCount = shareDef.await()
                val reviewsCount = reviewsDef.await()
                val chatCount = chatDef.await()

                Result.success(
                    FeatureUsageDashboard(
                        extendCount = extendCount,
                        shareCount = shareCount,
                        reviewsCount = reviewsCount,
                        chatCount = chatCount
                    )
                )
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * Q20 – Demand forecast for next 7 days for a given space.
     * Used in host spaces list (per space card).
     */
    suspend fun getDemandForecastForSpace(spaceId: String): Result<List<DemandForecastDayDto>> = withContext(Dispatchers.IO) {
        // simple LRU cache to avoid repeated calls when host has many spaces
        DemandForecastCache.get(spaceId)?.let { return@withContext Result.success(it) }
        return@withContext try {
            val forecast = api.getDemandForecast(spaceId)
            DemandForecastCache.put(spaceId, forecast)
            Result.success(forecast)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    // LRU cache for per-space forecast (max 50 entries)
    private object DemandForecastCache {
        private const val MAX_ENTRIES = 50
        private val lru = object : LinkedHashMap<String, List<DemandForecastDayDto>>(MAX_ENTRIES, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<DemandForecastDayDto>>): Boolean =
                size > MAX_ENTRIES
        }

        @Synchronized
        fun get(spaceId: String): List<DemandForecastDayDto>? = lru[spaceId]

        @Synchronized
        fun put(spaceId: String, value: List<DemandForecastDayDto>) {
            lru[spaceId] = value
        }
    }
}
