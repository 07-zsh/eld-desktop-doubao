package com.elder.desktop

import android.app.Application
import com.elder.desktop.data.di.AppContainer

class ElderApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
