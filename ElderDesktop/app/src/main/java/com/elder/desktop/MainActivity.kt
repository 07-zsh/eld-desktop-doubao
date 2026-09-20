package com.elder.desktop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.elder.desktop.ui.ElderDesktopRoot
import com.elder.desktop.ui.theme.ElderDesktopTheme

/**
 * 普通入口（桌面图标「老年桌面」）：用于首次安装的子女设置模式与后续维护。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as ElderApp
        setContent {
            ElderDesktopTheme {
                ElderDesktopRoot(container = app.container, startInSetup = true)
            }
        }
    }
}
