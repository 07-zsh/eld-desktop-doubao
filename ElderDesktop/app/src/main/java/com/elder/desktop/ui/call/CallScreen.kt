package com.elder.desktop.ui.call

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elder.desktop.data.di.AppContainer
import com.elder.desktop.data.model.Contact
import com.elder.desktop.ui.common.ContactRow
import com.elder.desktop.ui.common.TopBar
import com.elder.desktop.ui.theme.EldBg
import com.elder.desktop.ui.theme.EldPhone
import com.elder.desktop.ui.theme.EldSub
import com.elder.desktop.ui.theme.EldWhite
import com.elder.desktop.util.Caller
import com.elder.desktop.util.Guard
import java.io.File

@Composable
fun CallScreen(container: AppContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val contacts by produceState<List<Contact>>(initialValue = emptyList(), container) {
        container.contactRepository.observeContacts().collect { value = it }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(EldBg).padding(horizontal = 22.dp),
    ) {
        TopBar("电话", onBack)
        // 大拨号按钮：点拨号直接拨给第一个家人（技术方案 §4.2 主操作区）
        val first = contacts.firstOrNull()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(EldPhone)
                .clickable {
                    if (first != null) {
                        Guard.tap(context)
                        Caller.call(context, first.phone)
                    }
                }
                .padding(vertical = 30.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Phone, contentDescription = null, tint = EldWhite,
                modifier = Modifier.size(44.dp))
            Spacer(Modifier.size(16.dp))
            Text("拨号", color = EldWhite, style = MaterialTheme.typography.headlineLarge)
        }
        Spacer(Modifier.height(16.dp))
        Text("家人通讯录", color = EldSub, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(contacts, key = { it.id }) { c ->
                ContactRow(
                    contact = c,
                    avatarFile = avatarOf(context, c.avatarFileName),
                    onClick = {
                        Guard.tap(context)
                        Caller.call(context, c.phone)
                    },
                )
            }
        }
    }
}

private fun avatarOf(context: Context, name: String?): File? {
    if (name.isNullOrEmpty()) return null
    return File(context.filesDir, "preset/photos/$name")
}
