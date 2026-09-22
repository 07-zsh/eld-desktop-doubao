package com.elder.desktop.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elder.desktop.data.AppWhitelist
import com.elder.desktop.data.di.AppContainer
import com.elder.desktop.data.model.AppEntry
import com.elder.desktop.ui.common.TopBar
import com.elder.desktop.ui.theme.EldBg
import com.elder.desktop.ui.theme.EldCard
import com.elder.desktop.ui.theme.EldInk
import com.elder.desktop.ui.theme.EldMuted
import com.elder.desktop.ui.theme.EldPhone
import com.elder.desktop.ui.theme.EldSos
import com.elder.desktop.ui.theme.EldSub
import com.elder.desktop.ui.theme.EldWhite
import com.elder.desktop.util.AppLauncher
import kotlinx.coroutines.launch

/**
 * 子女「添加应用」管理页（功能3）：从白名单勾选桌面第三方应用，最多 [com.elder.desktop.data.repository.AppRepository.MAX_APPS] 个。
 * 仅从子女设置模式进入；桌面第三行按此处勾选结果展示。
 */
@Composable
fun AppManageScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hint by remember { mutableStateOf<String?>(null) }

    val apps by produceState<List<AppEntry>>(initialValue = emptyList(), container) {
        container.appRepository.observeApps().collect { value = it }
    }
    val addedPackages = apps.map { it.packageName }.toSet()
    val full = apps.size >= com.elder.desktop.data.repository.AppRepository.MAX_APPS

    Column(
        modifier = Modifier.fillMaxSize().background(EldBg).padding(horizontal = 22.dp),
    ) {
        TopBar("管理应用", onBack)
        Text(
            "选择放入桌面第三行的应用（最多 ${com.elder.desktop.data.repository.AppRepository.MAX_APPS} 个）。\n已选中的会显示在桌面，点一下直接打开。",
            color = EldSub, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 10.dp),
        )
        if (hint != null) {
            Text(hint!!, color = EldSos, style = MaterialTheme.typography.bodyMedium)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            items(AppWhitelist.ALL, key = { it.packageName }) { entry ->
                val added = entry.packageName in addedPackages
                val installed = container.appRepository.isInstalled(entry.packageName)
                AppRow(
                    label = entry.label,
                    icon = remember(entry.packageName) {
                        if (installed) AppLauncher.loadIcon(context, entry.packageName) else null
                    },
                    installed = installed,
                    added = added,
                    full = full,
                    onAdd = {
                        scope.launch {
                            val ok = container.appRepository.add(entry.packageName, entry.label)
                            hint = if (ok != null) null
                            else "最多只能添加 ${com.elder.desktop.data.repository.AppRepository.MAX_APPS} 个应用，先移除一个再添加"
                        }
                    },
                    onRemove = {
                        val current = apps.firstOrNull { it.packageName == entry.packageName }
                        if (current != null) scope.launch { container.appRepository.remove(current.id) }
                    },
                )
            }
        }
    }
}

@Composable
private fun AppRow(
    label: String,
    icon: ImageBitmap?,
    installed: Boolean,
    added: Boolean,
    full: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(EldCard)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFEFEFEF)),
                contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Image(bitmap = icon, contentDescription = label,
                    modifier = Modifier.size(46.dp))
            }
        } else {
            Box(Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFDFE7EF)),
                contentAlignment = Alignment.Center) {
                Text(label.take(1), color = EldInk, style = MaterialTheme.typography.headlineMedium)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = EldInk, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (!installed) "未安装" else if (added) "已放入桌面" else "点击添加到桌面",
                color = if (!installed) EldSos else EldMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (added) {
            TextButton(onClick = onRemove) {
                Text("移除", color = EldSos, style = MaterialTheme.typography.bodyMedium)
            }
        } else if (installed) {
            Button(
                onClick = onAdd,
                enabled = !full,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EldPhone,
                    disabledContainerColor = EldCard.copy(alpha = 0.4f),
                ),
                modifier = Modifier.height(52.dp),
            ) {
                Text(if (full) "已满" else "添加", color = EldWhite,
                    style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Text("未安装", color = EldMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
