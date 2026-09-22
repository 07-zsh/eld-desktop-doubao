package com.elder.desktop.util

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * 桌面第三方应用（功能3）的启动与图标加载。
 * 启动：解析 launch intent 后 startActivity；失败或未安装返回 false（桌面格不渲染/兜底）。
 * 图标：从 PackageManager 取应用图标 Drawable，转为 Compose ImageBitmap。
 */
object AppLauncher {

    /** 启动应用。成功返回 true；未安装或启动异常返回 false。 */
    fun launch(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return false
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** 应用图标。未安装或异常时返回 null。 */
    fun loadIcon(context: Context, packageName: String): ImageBitmap? {
        return try {
            val pm = context.packageManager
            pm.getApplicationIcon(packageName)
                .toBitmap(width = 144, height = 144)
                .asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}
