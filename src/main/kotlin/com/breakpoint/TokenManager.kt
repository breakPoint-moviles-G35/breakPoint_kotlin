package com.breakpoint

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth")

class TokenManager(private val context: Context) {
    private val KEY = stringPreferencesKey("jwt")

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY] }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[KEY] = token }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(KEY) }
    }
}


