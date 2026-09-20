package com.elder.desktop.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.elder.desktop.data.model.Contact
import com.elder.desktop.ui.theme.EldCard
import com.elder.desktop.ui.theme.EldInk
import com.elder.desktop.ui.theme.EldPhone

/**
 * 联系人卡片：大头像 + 大字姓名 + 电话图标。电话/家人模块复用。
 */
@Composable
fun ContactRow(
    contact: Contact,
    avatarFile: java.io.File?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(EldCard)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (avatarFile != null && avatarFile.exists()) {
            AsyncImage(
                model = avatarFile,
                contentDescription = contact.name,
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDFE7EF)),
                contentAlignment = Alignment.Center,
            ) {
                Text(contact.name.take(1), color = EldInk,
                    style = MaterialTheme.typography.headlineMedium)
            }
        }
        Spacer(Modifier.width(18.dp))
        Text(
            contact.name,
            color = EldInk,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.Phone, contentDescription = "拨打", tint = EldPhone,
            modifier = Modifier.size(48.dp))
    }
}
