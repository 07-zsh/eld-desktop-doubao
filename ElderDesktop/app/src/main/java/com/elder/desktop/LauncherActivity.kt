package com.elder.desktop

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import com.elder.desktop.ui.ElderDesktopRoot
import com.elder.desktop.ui.theme.ElderDesktopTheme

/**
 * 桌面入口：声明 HOME/DEFAULT intent-filter，设为默认桌面后开机直达（技术方案 §3.2）。
 * singleTask + onNewIntent：收到 HOME 键仅回到首页，不做越权导航。
 */
class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as ElderApp
        setContent {
            ElderDesktopTheme {
                ElderDesktopRoot(container = app.container, startInSetup = false)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // HOME 键事件：Root 内部状态自然停留在首页，无需额外处理
    }
}
