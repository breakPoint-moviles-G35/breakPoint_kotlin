package com.breakpoint

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth")

class TokenManager(private val context: Context) {
    private val KEY_TOKEN = stringPreferencesKey("jwt")
    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_USER_ROLE = stringPreferencesKey("user_role")

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val roleFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ROLE] }

    suspend fun saveSession(token: String, userId: String, role: String?) {
        context.dataStore.edit {
            it[KEY_TOKEN] = token
            it[KEY_USER_ID] = userId
            role?.let { r -> it[KEY_USER_ROLE] = r } ?: it.remove(KEY_USER_ROLE)
        }
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[KEY_TOKEN] = token }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(KEY_TOKEN)
            it.remove(KEY_USER_ID)
            it.remove(KEY_USER_ROLE)
        }
    }
}




