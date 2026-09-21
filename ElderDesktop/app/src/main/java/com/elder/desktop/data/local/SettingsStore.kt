package com.elder.desktop.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * 极简设置：仅记录「已初始化」与「SOS 冷却时间戳」，无账户、无云同步。
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val INITIALIZED = booleanPreferencesKey("initialized")
        val SOS_LAST_TRIGGER_AT = longPreferencesKey("sos_last_trigger_at")
    }

    val isInitialized: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.INITIALIZED] ?: false }

    val sosLastTriggerAt: Flow<Long> =
        context.settingsDataStore.data.map { it[Keys.SOS_LAST_TRIGGER_AT] ?: 0L }

    suspend fun markInitialized() {
        context.settingsDataStore.edit { it[Keys.INITIALIZED] = true }
    }

    suspend fun setSosLastTrigger(atMillis: Long) {
        context.settingsDataStore.edit { it[Keys.SOS_LAST_TRIGGER_AT] = atMillis }
    }
}
