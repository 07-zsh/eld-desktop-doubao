package com.elder.desktop.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.wechatCalibrationDataStore by preferencesDataStore(name = "wechat_calibration")

/**
 * 功能2「一键微信视频」校准数据模型。
 * 全部为相对屏幕坐标（0~1），由子女在校准向导一次性采集，老人一键回放。
 */
data class CalibrationPoint(val x: Float, val y: Float)

data class WechatCalibration(
    val screenW: Int,
    val screenH: Int,
    val wechatVersion: String,
    /** 依次：1放大镜 2搜索框 3粘贴 4联系人结果 5加号 6加号面板视频 7确认视频 */
    val steps: List<CalibrationPoint>,
    val calibrated: Boolean,
) {
    companion object {
        fun empty(): WechatCalibration = WechatCalibration(
            screenW = 0, screenH = 0, wechatVersion = "",
            steps = List(7) { CalibrationPoint(0f, 0f) }, calibrated = false,
        )
    }
}

/**
 * 微信视频校准数据存储（DataStore）。7 步坐标 + 屏幕尺寸 + 微信版本 + 是否已校准。
 * 屏幕尺寸与微信版本用于判断设备/版本变化时提示重新校准。
 */
class WechatCalibrationStore(private val context: Context) {

    private object Keys {
        val CALIBRATED = booleanPreferencesKey("calibrated")
        val SCREEN_W = intPreferencesKey("screen_w")
        val SCREEN_H = intPreferencesKey("screen_h")
        val WECHAT_VERSION = stringPreferencesKey("wechat_version")

        fun stepX(i: Int) = floatPreferencesKey("step_${i + 1}_x")
        fun stepY(i: Int) = floatPreferencesKey("step_${i + 1}_y")
    }

    val flow: Flow<WechatCalibration> = context.wechatCalibrationDataStore.data.map { p ->
        WechatCalibration(
            screenW = p[Keys.SCREEN_W] ?: 0,
            screenH = p[Keys.SCREEN_H] ?: 0,
            wechatVersion = p[Keys.WECHAT_VERSION] ?: "",
            steps = (0 until 7).map { i ->
                CalibrationPoint(p[Keys.stepX(i)] ?: 0f, p[Keys.stepY(i)] ?: 0f)
            },
            calibrated = p[Keys.CALIBRATED] ?: false,
        )
    }

    /** 保存一次完整的 7 步校准结果。 */
    suspend fun saveCalibration(screenW: Int, screenH: Int, wechatVersion: String, steps: List<CalibrationPoint>) {
        require(steps.size == 7) { "需要恰好 7 个校准点" }
        context.wechatCalibrationDataStore.edit { p ->
            p[Keys.CALIBRATED] = true
            p[Keys.SCREEN_W] = screenW
            p[Keys.SCREEN_H] = screenH
            p[Keys.WECHAT_VERSION] = wechatVersion
            steps.forEachIndexed { i, pt ->
                p[Keys.stepX(i)] = pt.x
                p[Keys.stepY(i)] = pt.y
            }
        }
    }
}
