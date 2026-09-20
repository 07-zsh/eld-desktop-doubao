package com.elder.desktop.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * 权限检查与申请清单。运行时申请仅出现在子女设置模式（D1）。
 */
object Permissions {

    /** 需要在设置模式一次性申请的权限。 */
    val REQUIRED = listOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.SEND_SMS,
    )

    fun missing(context: Context): List<String> = REQUIRED.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }

    fun allGranted(context: Context): Boolean = missing(context).isEmpty()
}
