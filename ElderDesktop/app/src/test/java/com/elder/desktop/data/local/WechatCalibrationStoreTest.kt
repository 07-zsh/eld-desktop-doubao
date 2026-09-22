package com.elder.desktop.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 功能2 校准数据存储（DataStore）读写测试。 */
@RunWith(RobolectricTestRunner::class)
class WechatCalibrationStoreTest {

    @Test
    fun saveAndReadBack() = runBlocking {
        val store = WechatCalibrationStore(ApplicationProvider.getApplicationContext<Context>())
        val steps = List(7) { CalibrationPoint(it * 0.1f, 0.5f) }

        store.saveCalibration(1080, 2340, "8.0.76", steps)

        val c = store.flow.first()
        assertTrue(c.calibrated)
        assertEquals(1080, c.screenW)
        assertEquals(2340, c.screenH)
        assertEquals("8.0.76", c.wechatVersion)
        assertEquals(7, c.steps.size)
        assertEquals(0.3f, c.steps[3].x, 1e-6f)
        assertEquals(0.5f, c.steps[3].y, 1e-6f)
        assertEquals(0.6f, c.steps[6].x, 1e-6f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun saveRequiresExactlySevenPoints() = runBlocking {
        val store = WechatCalibrationStore(ApplicationProvider.getApplicationContext<Context>())
        store.saveCalibration(1080, 2340, "8.0.76", List(3) { CalibrationPoint(0.5f, 0.5f) })
    }
}
