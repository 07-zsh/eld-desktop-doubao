package com.elder.desktop.data.repository

import android.content.Context
import com.elder.desktop.data.local.PresetParser
import com.elder.desktop.data.model.Weather

/**
 * 天气：MVP 仅读取 assets/preset/weather.json 占位值（技术方案 D2）。
 */
class WeatherRepository(private val context: Context) {

    fun load(): Weather? = runCatching {
        context.assets.open("preset/weather.json").bufferedReader().use {
            PresetParser.parseWeather(it.readText())
        }
    }.getOrNull()
}
