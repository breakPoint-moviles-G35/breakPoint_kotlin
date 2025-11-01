package com.breakpoint

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.firstOrNull

private val Context.cacheDataStore by preferencesDataStore(name = "cache")

class CacheManager(private val context: Context) {
    private val gson = Gson()
    private val KEY_SPACES = stringPreferencesKey("spaces_json")
    private val KEY_DETAILS = stringPreferencesKey("details_json")
    private val KEY_BOOKINGS = stringPreferencesKey("bookings_json")
    private val KEY_BOOKINGS_PRESENT = booleanPreferencesKey("bookings_cached")

    // Lista de espacios (para Explore)
    suspend fun saveSpaces(list: List<SpaceItem>) {
        val json = gson.toJson(list)
        context.cacheDataStore.edit { it[KEY_SPACES] = json }
    }

    suspend fun loadSpaces(): List<SpaceItem> {
        val prefs = context.cacheDataStore.data.firstOrNull()
        val json = prefs?.get(KEY_SPACES) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SpaceItem>>() {}.type
            gson.fromJson<List<SpaceItem>>(json, type) ?: emptyList()
        } catch (_: Throwable) { emptyList() }
    }

    // Detalles: mantener últimos 5
    suspend fun saveDetail(detail: DetailedSpace) {
        val existing = loadAllDetailsMutable()
        val filtered = existing.filter { it.id != detail.id }
        val updated = listOf(detail) + filtered
        val trimmed = if (updated.size > 5) updated.take(5) else updated
        val json = gson.toJson(trimmed)
        context.cacheDataStore.edit { it[KEY_DETAILS] = json }
    }

    suspend fun loadDetail(spaceId: String): DetailedSpace? {
        return loadAllDetailsMutable().firstOrNull { it.id == spaceId }
    }

    private suspend fun loadAllDetailsMutable(): List<DetailedSpace> {
        val prefs = context.cacheDataStore.data.firstOrNull()
        val json = prefs?.get(KEY_DETAILS) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<DetailedSpace>>() {}.type
            gson.fromJson<List<DetailedSpace>>(json, type) ?: emptyList()
        } catch (_: Throwable) { emptyList() }
    }

    // Reservas del usuario
    suspend fun saveBookings(list: List<BookingListItemDto>) {
        val json = gson.toJson(list)
        context.cacheDataStore.edit { prefs ->
            prefs[KEY_BOOKINGS] = json
            prefs[KEY_BOOKINGS_PRESENT] = true
        }
    }

    suspend fun loadBookings(): List<BookingListItemDto> {
        val prefs = context.cacheDataStore.data.firstOrNull()
        val json = prefs?.get(KEY_BOOKINGS) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<BookingListItemDto>>() {}.type
            gson.fromJson<List<BookingListItemDto>>(json, type) ?: emptyList()
        } catch (_: Throwable) { emptyList() }
    }

    suspend fun hasBookingsCache(): Boolean {
        val prefs = context.cacheDataStore.data.firstOrNull()
        return prefs?.get(KEY_BOOKINGS_PRESENT) == true
    }
}


