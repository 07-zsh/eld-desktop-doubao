package com.elder.desktop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.elder.desktop.data.di.AppContainer
import com.elder.desktop.ui.app.AppManageScreen
import com.elder.desktop.ui.calibration.CalibrationScreen
import com.elder.desktop.ui.call.CallScreen
import com.elder.desktop.ui.contact.ContactEditScreen
import com.elder.desktop.ui.contact.ContactManageScreen
import com.elder.desktop.ui.family.FamilyScreen
import com.elder.desktop.ui.home.HomeScreen
import com.elder.desktop.ui.photo.PhotoScreen
import com.elder.desktop.ui.setup.SetupScreen
import com.elder.desktop.ui.sos.SosScreen

/** 单屏导航：产品要求无二级菜单，仅做平级页面切换 + 返回首页。
 *  子女维护入口（管理家人/编辑联系人/校准微信视频）仅从设置模式进入，长辈阶段不可达。 */
sealed interface Screen {
    data object Home : Screen
    data object Call : Screen
    data object Family : Screen
    data object Photo : Screen
    data object Sos : Screen
    data object Setup : Screen
    data object ContactManage : Screen
    /** contactId == null 表示新增联系人；否则编辑指定联系人。 */
    data class ContactEdit(val contactId: Long?) : Screen
    data object AppManage : Screen
    data object Calibration : Screen
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
        Screen.Setup -> SetupScreen(
            container = container,
            onDone = goHome,
            onManageContacts = { screen = Screen.ContactManage },
            onManageApps = { screen = Screen.AppManage },
            onCalibrateWechat = { screen = Screen.Calibration },
        )
        Screen.ContactManage -> ContactManageScreen(
            container = container,
            onBack = { screen = Screen.Setup },
            onAdd = { screen = Screen.ContactEdit(null) },
            onEdit = { id -> screen = Screen.ContactEdit(id) },
        )
        is Screen.ContactEdit -> ContactEditScreen(
            container = container,
            contactId = s.contactId,
            onBack = { screen = Screen.ContactManage },
            onSaved = { screen = Screen.ContactManage },
        )
        Screen.AppManage -> AppManageScreen(
            container = container,
            onBack = { screen = Screen.Setup },
        )
        Screen.Calibration -> CalibrationScreen(
            container = container,
            onBack = { screen = Screen.Setup },
        )
    }
}
