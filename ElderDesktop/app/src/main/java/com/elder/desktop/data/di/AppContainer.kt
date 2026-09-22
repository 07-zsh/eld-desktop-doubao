package com.elder.desktop.data.di

import android.content.Context
import com.elder.desktop.data.local.AppDatabase
import com.elder.desktop.data.local.PhotoStore
import com.elder.desktop.data.local.PresetImporter
import com.elder.desktop.data.local.SettingsStore
import com.elder.desktop.data.repository.AppRepository
import com.elder.desktop.data.repository.ContactRepository
import com.elder.desktop.data.repository.PhotoRepository
import com.elder.desktop.data.repository.SosRepository
import com.elder.desktop.data.repository.WeatherRepository

/**
 * 手工 DI 容器（技术方案 §2.2 备选方案：MVP 精简，不引入 Hilt，降低编译体积与复杂度）。
 * 在 Application 中持有单例，ViewModel 经工厂取用。
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)
    private val settings = SettingsStore(appContext)

    val contactRepository = ContactRepository(db.contactDao())
    val photoRepository = PhotoRepository(PhotoStore(appContext))
    val sosRepository = SosRepository(db.emergencyDao(), settings)
    val weatherRepository = WeatherRepository(appContext)
    val presetImporter = PresetImporter(appContext, db.contactDao(), db.emergencyDao(), settings)
    val appRepository = AppRepository(db.appDao(), appContext.packageManager)

    val isInitialized = settings.isInitialized
}
