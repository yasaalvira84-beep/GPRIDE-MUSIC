package com.gpride.player

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private val historyKey = booleanPreferencesKey("history_enabled")

    val historyEnabled: Flow<Boolean> = context.dataStore.data.map { it[historyKey] ?: true }

    suspend fun setHistoryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[historyKey] = enabled }
    }
}
