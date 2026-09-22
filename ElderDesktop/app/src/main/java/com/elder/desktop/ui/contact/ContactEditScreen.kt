package com.elder.desktop.ui.contact

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.elder.desktop.data.di.AppContainer
import com.elder.desktop.data.local.AvatarStore
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

private val fieldStyle = TextStyle(
    color = EldInk,
    fontSize = 26.sp,
    fontWeight = FontWeight.Bold,
)

/**
 * 联系人新增/编辑表单（功能1）。contactId == null 表示新增。
 * 头像经 GetContent 免权限选择并拷贝进私有目录；紧急联系人开关保证 SOS 拨打对象可控（最多一人紧急）。
 */
@Composable
fun ContactEditScreen(
    container: AppContainer,
    contactId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var avatarFileName by remember { mutableStateOf<String?>(null) }
    var isEmergency by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(contactId == null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 编辑模式加载现有联系人
    LaunchedEffect(contactId) {
        if (contactId != null) {
            val c = container.contactRepository.getById(contactId)
            if (c != null) {
                name = c.name
                phone = c.phone
                avatarFileName = c.avatarFileName
                isEmergency = c.isEmergency
            }
        }
        loaded = true
    }

    val avatarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            avatarFileName = runCatching { AvatarStore.copyFromUri(context, it) }
                .getOrElse { avatarFileName }
        }
    }

    val avatarFile = remember(avatarFileName) { AvatarStore.fileFor(context, avatarFileName) }
    val title = if (contactId == null) "新增联系人" else "编辑联系人"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EldBg)
            .padding(horizontal = 22.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        TopBar(title, onBack)
        Spacer(Modifier.height(16.dp))

        // 头像选择
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(EldCard)
                .clickable { avatarLauncher.launch("image/*") }
                .padding(vertical = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (avatarFile != null && avatarFile.exists()) {
                    AsyncImage(
                        model = avatarFile,
                        contentDescription = "头像",
                        modifier = Modifier.size(120.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.size(120.dp).clip(CircleShape).background(Color(0xFFDFE7EF)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (name.isNotBlank()) name.take(1) else "?", color = EldInk,
                            style = MaterialTheme.typography.headlineLarge)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("点击更换头像", color = EldPhone, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(16.dp))

        Text("姓名", color = EldSub, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            textStyle = fieldStyle,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = EldCard,
                unfocusedContainerColor = EldCard,
                focusedTextColor = EldInk,
                unfocusedTextColor = EldInk,
                cursorColor = EldPhone,
            ),
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("请输入家人姓名", color = EldMuted,
                style = MaterialTheme.typography.bodyMedium) },
        )
        Spacer(Modifier.height(16.dp))

        Text("手机号", color = EldSub, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            singleLine = true,
            textStyle = fieldStyle,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = EldCard,
                unfocusedContainerColor = EldCard,
                focusedTextColor = EldInk,
                unfocusedTextColor = EldInk,
                cursorColor = EldPhone,
            ),
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("请输入手机号", color = EldMuted,
                style = MaterialTheme.typography.bodyMedium) },
        )
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(EldCard)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("紧急联系人", color = EldInk, style = MaterialTheme.typography.bodyLarge)
                Text("SOS 触发时拨打此号码", color = EldMuted,
                    style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = isEmergency, onCheckedChange = { isEmergency = it })
        }
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (name.isBlank() || phone.isBlank()) return@Button
                scope.launch {
                    if (contactId == null) {
                        container.contactRepository.addContact(name, phone, avatarFileName, isEmergency)
                    } else {
                        container.contactRepository.updateContact(
                            contactId, name, phone, avatarFileName, isEmergency,
                        )
                    }
                    onSaved()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = EldWhite),
            modifier = Modifier.fillMaxWidth().height(72.dp),
        ) {
            Text("保存", color = EldBg, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(12.dp))

        if (contactId != null) {
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) {
                Text("删除这位家人", color = EldSos, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除联系人", color = EldInk) },
            text = { Text("确定删除「$name」吗？", color = EldMuted) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        contactId?.let { container.contactRepository.deleteContact(it) }
                        onSaved()
                    }
                    showDeleteConfirm = false
                }) { Text("删除", color = EldSos) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消", color = EldMuted) }
            },
        )
    }
}
