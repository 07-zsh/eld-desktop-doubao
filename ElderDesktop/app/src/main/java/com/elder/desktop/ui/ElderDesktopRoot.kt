package com.elder.desktop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.elder.desktop.data.di.AppContainer
import com.elder.desktop.ui.call.CallScreen
import com.elder.desktop.ui.family.FamilyScreen
import com.elder.desktop.ui.home.HomeScreen
import com.elder.desktop.ui.photo.PhotoScreen
import com.elder.desktop.ui.setup.SetupScreen
import com.elder.desktop.ui.sos.SosScreen

/** 单屏导航：产品要求无二级菜单，仅做平级页面切换 + 返回首页。 */
sealed interface Screen {
    data object Home : Screen
    data object Call : Screen
    data object Family : Screen
    data object Photo : Screen
    data object Sos : Screen
    data object Setup : Screen
}

@Composable
fun ElderDesktopRoot(container: AppContainer, startInSetup: Boolean) {
    var screen by remember {
        mutableStateOf<Screen>(if (startInSetup) Screen.Setup else Screen.Home)
    }
    val goHome = { screen = Screen.Home }

    when (val s = screen) {
        Screen.Home -> HomeScreen(
            container = container,
            onOpenCall = { screen = Screen.Call },
            onOpenFamily = { screen = Screen.Family },
            onOpenPhoto = { screen = Screen.Photo },
            onOpenSos = { screen = Screen.Sos },
        )
        Screen.Call -> CallScreen(container = container, onBack = goHome)
        Screen.Family -> FamilyScreen(container = container, onBack = goHome)
        Screen.Photo -> PhotoScreen(container = container, onBack = goHome)
        Screen.Sos -> SosScreen(container = container, onBack = goHome)
        Screen.Setup -> SetupScreen(container = container, onDone = goHome)
    }
}
