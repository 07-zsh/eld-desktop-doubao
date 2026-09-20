package com.elder.desktop.data.repository

import com.elder.desktop.data.local.EmergencyDao
import com.elder.desktop.data.local.SettingsStore
import com.elder.desktop.data.model.EmergencyInfo

/**
 * SOS 相关数据：紧急号码、预设短信、冷却时间戳。
 */
class SosRepository(
    private val emergencyDao: EmergencyDao,
    private val settings: SettingsStore,
) {
    suspend fun emergencyList(): List<EmergencyInfo> = emergencyDao.getAll()
    suspend fun primaryEmergency(): EmergencyInfo? = emergencyDao.getAll().firstOrNull()

    suspend fun markTriggered(atMillis: Long) = settings.setSosLastTrigger(atMillis)
    fun lastTriggerAt() = settings.sosLastTriggerAt
}
