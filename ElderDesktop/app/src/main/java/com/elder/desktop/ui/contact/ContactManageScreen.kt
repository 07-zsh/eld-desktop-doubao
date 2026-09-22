package com.elder.desktop.ui.contact

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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.elder.desktop.data.di.AppContainer
import com.elder.desktop.data.local.AvatarStore
import com.elder.desktop.data.model.Contact
import com.elder.desktop.ui.common.TopBar
import com.elder.desktop.ui.theme.EldBg
import com.elder.desktop.ui.theme.EldCard
import com.elder.desktop.ui.theme.EldInk
import com.elder.desktop.ui.theme.EldMuted
import com.elder.desktop.ui.theme.EldPhone
import com.elder.desktop.ui.theme.EldSos
import com.elder.desktop.ui.theme.EldSub
import com.elder.desktop.ui.theme.EldWhite
import kotlinx.coroutines.launch

/**
 * 子女联系人管理页（功能1）：列表展示 + 新增 / 编辑 / 删除入口。
 * 仅从子女设置模式进入；增删改不删除头像文件（与相册共用目录）。
 */
@Composable
fun ContactManageScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<Contact?>(null) }

    val contacts by produceState<List<Contact>>(initialValue = emptyList(), container) {
        container.contactRepository.observeContacts().collect { value = it }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(EldBg).padding(horizontal = 22.dp),
    ) {
        TopBar("管理家人", onBack)
        Text(
            "新增 / 修改家人姓名、手机号与头像。删除后长辈端自动更新。",
            color = EldSub, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 10.dp),
        )
        if (contacts.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("还没有家人，点下方「新增」添加", color = EldSub,
                    style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(contacts, key = { it.id }) { c ->
                    ManageRow(
                        contact = c,
                        avatarFile = AvatarStore.fileFor(context, c.avatarFileName),
                        onEdit = { onEdit(c.id) },
                        onDelete = { pendingDelete = c },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = EldWhite),
            modifier = Modifier.fillMaxWidth().height(72.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = EldBg)
            Spacer(Modifier.width(8.dp))
            Text("新增联系人", color = EldBg, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(16.dp))
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除联系人", color = EldInk) },
            text = { Text("确定删除「${target.name}」吗？\n头像文件会保留（可能与相册共用）。", color = EldMuted) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { container.contactRepository.deleteContact(target.id) }
                    pendingDelete = null
                }) { Text("删除", color = EldSos) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消", color = EldMuted) }
            },
        )
    }
}

@Composable
private fun ManageRow(
    contact: Contact,
    avatarFile: java.io.File?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(EldCard)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (avatarFile != null && avatarFile.exists()) {
            AsyncImage(
                model = avatarFile,
                contentDescription = contact.name,
                modifier = Modifier.size(72.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFFDFE7EF)),
                contentAlignment = Alignment.Center,
            ) {
                Text(contact.name.take(1), color = EldInk,
                    style = MaterialTheme.typography.headlineMedium)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(contact.name, color = EldInk, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (contact.isEmergency) "${contact.phone}（紧急）" else contact.phone,
                color = EldMuted, style = MaterialTheme.typography.bodyMedium,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = EldPhone,
                modifier = Modifier.size(34.dp))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "删除", tint = EldSos,
                modifier = Modifier.size(34.dp))
        }
    }
}
