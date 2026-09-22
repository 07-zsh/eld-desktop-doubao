package com.elder.desktop.ui.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elder.desktop.data.di.AppContainer
import com.elder.desktop.data.local.PresetImporter
import com.elder.desktop.ui.theme.EldBg
import com.elder.desktop.ui.theme.EldSub
import com.elder.desktop.ui.theme.EldWhite
import com.elder.desktop.util.Permissions
import kotlinx.coroutines.launch

/**
 * 子女设置模式（D1）：唯一允许出现系统权限弹窗的阶段。
 * 一次性授予 CALL_PHONE/SEND_SMS 并导入预置数据，长辈阶段零弹窗。
 * 导入完成后提供「管理家人」（联系人增删改，功能1）与「管理应用」（功能3）入口。
 */
@Composable
fun SetupScreen(
    container: AppContainer,
    onDone: () -> Unit,
    onManageContacts: () -> Unit,
    onManageApps: () -> Unit,
    onCalibrateWechat: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("正在为长辈准备桌面…") }
    var done by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        scope.launch {
            val result = container.presetImporter.importIfNeeded()
            status = when (result) {
                is PresetImporter.Result.Imported ->
                    "已导入 ${result.contacts} 位家人、${result.photos} 张照片"
                PresetImporter.Result.AlreadyInitialized ->
                    "设置已完成"
            }
            done = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(EldBg).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("子女设置", color = EldWhite, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "本步骤由子女操作：授予电话/短信权限，并导入家人通讯录与照片。\n" +
                "完成后长辈使用时不会再出现任何弹窗。",
            color = EldSub, style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        Text(status, color = EldWhite, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (done) {
                    onDone()
                } else {
                    permissionLauncher.launch(Permissions.REQUIRED.toTypedArray())
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = EldWhite),
            modifier = Modifier.fillMaxWidth().height(72.dp),
        ) {
            Text(if (done) "开始使用" else "授予权限并导入",
                color = EldBg, style = MaterialTheme.typography.bodyLarge)
        }
        if (done) {
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onManageContacts,
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) {
                Text("管理家人（增删改）", color = EldWhite,
                    style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onManageApps,
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) {
                Text("管理应用（桌面第三行）", color = EldWhite,
                    style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onCalibrateWechat,
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) {
                Text("校准微信视频（一键视频）", color = EldWhite,
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
