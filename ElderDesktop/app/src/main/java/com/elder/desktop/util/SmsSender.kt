package com.elder.desktop.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

/**
 * SOS 短信静默发送（技术方案 §4.5）。授予过 SEND_SMS 后直接 sendTextMessage，不弹窗。
 * 失败返回 false，由调用方大字提示，不阻塞拨号结果。
 */
object SmsSender {

    fun hasSmsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    /** @return true 表示已提交发送。 */
    fun send(context: Context, phone: String, message: String): Boolean {
        val number = phone.trim()
        if (number.isEmpty() || message.isEmpty()) return false
        return try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(number, null, message, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }
}
