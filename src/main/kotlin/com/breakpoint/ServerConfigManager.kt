package com.breakpoint

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.serverDataStore by preferencesDataStore(name = "server_config")

class ServerConfigManager(private val context: Context) {
    private val KEY_URL = stringPreferencesKey("server_url")

    val urlFlow: Flow<String?> = context.serverDataStore.data.map { it[KEY_URL] }

    suspend fun saveUrl(url: String) {
        context.serverDataStore.edit { it[KEY_URL] = url }
    }
}


