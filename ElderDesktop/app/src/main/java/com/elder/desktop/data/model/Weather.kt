package com.elder.desktop.data.model

/**
 * 天气占位数据（技术方案 D2：MVP 不联网、不申请定位，仅展示子女预置值）。
 */
data class Weather(
    val city: String,
    val condition: String,
    val temp: String,
)
