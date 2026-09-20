package com.elder.desktop.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 防误触与触觉反馈统一封装（技术方案 §8）。
 */
object Guard {

    /** 判断 [now] 相对 [lastAt] 是否在 [windowMs] 内，用于拦截连续误触。 */
    fun isWithinDoubleTap(lastAt: Long, now: Long, windowMs: Long = 600L): Boolean =
        now - lastAt in 1..windowMs

    /** 轻触反馈：普通按钮。 */
    fun tap(context: Context) = vibrate(context, 25)

    /** 强烈反馈：SOS 触发。 */
    fun alert(context: Context) = vibrate(context, 400)

    @Suppress("DEPRECATION")
    private fun vibrate(context: Context, ms: Long) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }
}
