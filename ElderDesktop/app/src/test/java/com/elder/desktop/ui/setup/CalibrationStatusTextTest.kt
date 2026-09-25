package com.elder.desktop.ui.setup

import com.elder.desktop.data.local.CalibrationPoint
import com.elder.desktop.data.local.WechatCalibration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 设置页"微信视频校准"记录摘要文本的纯函数单测。 */
class CalibrationStatusTextTest {

    @Test
    fun `uncalibrated returns 未校准`() {
        assertEquals("未校准", calibrationStatusText(WechatCalibration.empty()))
    }

    @Test
    fun `calibrated shows screen version and full steps`() {
        val cal = WechatCalibration(
            screenW = 1080, screenH = 2340, wechatVersion = "8.0.76",
            steps = List(7) { CalibrationPoint(0.5f, 0.5f) }, calibrated = true,
        )
        val t = calibrationStatusText(cal)
        assertTrue(t.contains("已校准"))
        assertTrue(t.contains("1080x2340"))
        assertTrue(t.contains("8.0.76"))
        assertTrue(t.contains("7/7"))
    }

    @Test
    fun `calibrated with missing steps reports recorded count`() {
        val steps = List(7) { CalibrationPoint(0f, 0f) }.toMutableList()
        steps[0] = CalibrationPoint(0.5f, 0.5f)
        val cal = WechatCalibration(1080, 2340, "8.0.76", steps, calibrated = true)
        assertTrue(calibrationStatusText(cal).contains("1/7"))
    }
}
