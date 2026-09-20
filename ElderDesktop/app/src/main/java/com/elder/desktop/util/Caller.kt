package com.elder.desktop.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 拨号封装：优先 ACTION_CALL 直接拨打；权限被撤或失败时回退 ACTION_DIAL（系统拨号界面），
 * 绝不静默崩溃（技术方案 §4.2、§8）。
 */
object Caller {

    fun hasCallPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * 发起呼叫。
     * @return true 表示已直接 ACTION_CALL；false 表示已回退到 ACTION_DIAL 或号码为空。
     */
    fun call(context: Context, phone: String): Boolean {
        val number = phone.trim()
        if (number.isEmpty()) return false
        return try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (se: SecurityException) {
            dial(context, number); false
        } catch (e: Exception) {
            dial(context, number); false
        }
    }

    /** 兜底：打开系统拨号界面（不直接拨出）。 */
    fun dial(context: Context, phone: String) {
        val number = phone.trim()
        if (number.isEmpty()) return
        runCatching {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun buildCallIntent(phone: String): Intent =
        Intent(Intent.ACTION_CALL, Uri.parse("tel:${phone.trim()}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
