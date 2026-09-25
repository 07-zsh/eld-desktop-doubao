package com.elder.desktop.ui.family

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.elder.desktop.data.di.AppContainer
import com.elder.desktop.data.local.WechatCalibration
import com.elder.desktop.data.model.Contact
import com.elder.desktop.ui.common.TopBar
import com.elder.desktop.ui.theme.EldBg
import com.elder.desktop.ui.theme.EldCard
import com.elder.desktop.ui.theme.EldInk
import com.elder.desktop.ui.theme.EldMuted
import com.elder.desktop.ui.theme.EldPhone
import com.elder.desktop.ui.theme.EldSub
import com.elder.desktop.util.Guard
import com.elder.desktop.wxvideo.WechatVideoCallService
import java.io.File

/**
 * 家人页（功能2 改版）：六宫格（3 行 × 2 列），可滚动。
 * 只显示已配「微信备注（wechatRemark）」的家人（备注为空则该家人不出现在视频入口）；
 * 空格隐藏、整体居中；仅点「视频」按钮发起微信视频。
 * 头像方形圆角、字体紧凑；视频按钮仍沿用现有已验证的 7 步机制（需备注+校准+无障碍），
 * 路线二深链（weixin://dl/chat?username=wxid）已真机判死，不再使用。
 */

/**
 * 六宫格家人过滤（纯函数，可单测）：只有填了非空微信备注的家人进入视频入口。
 * 备注用于搜索路径剪贴板定位联系人；wxid 不再作为显示条件。
 */
fun videoEligibleContacts(contacts: List<Contact>): List<Contact> =
    contacts.filter { !it.wechatRemark.isNullOrBlank() }
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FamilyScreen(container: AppContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val contacts by produceState<List<Contact>>(initialValue = emptyList(), container) {
        container.contactRepository.observeContacts().collect { value = it }
    }
    val calibration by produceState(
        initialValue = WechatCalibration.empty(), container,
    ) {
        container.wechatCalibration.flow.collect { c -> value = c }
    }
    val a11yEnabled = produceState(initialValue = false) {
        value = WechatVideoCallService.isEnabled(context)
    }

    // 只显示已填微信备注的家人（空格隐藏；备注用于搜索定位，wxid 不再是显示条件）
    val videoContacts = videoEligibleContacts(contacts)

    Column(
        modifier = Modifier.fillMaxSize().background(EldBg).padding(horizontal = 22.dp),
    ) {
        TopBar("家人", onBack)
        Text(
            "点「视频」按钮，给家人打微信视频",
            color = EldSub, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 10.dp),
        )
        if (videoContacts.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "还没有可视频的家人\n请在设置里为家人填写微信备注",
                    color = EldSub, style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp),
            ) {
                items(videoContacts, key = { it.id }) { c ->
                    val canVideo = !c.wechatRemark.isNullOrBlank() &&
                        calibration.calibrated && a11yEnabled.value
                    FamilyTile(
                        contact = c,
                        avatarFile = avatarOf(context, c.avatarFileName),
                        onVideoClick = if (canVideo) {
                            {
                                Guard.tap(context)
                                WechatVideoCallService.instance?.startCall(
                                    c.wechatRemark.orEmpty(), calibration,
                                )
                            }
                        } else null,
                    )
                }
            }
        }
    }
}

/**
 * 六宫格单元：方形大头像 + 姓名 + 微信备注小字 + 视频按钮。
 * 只有视频按钮可点；其余区域不响应点击（防误触）。
 */
@Composable
private fun FamilyTile(
    contact: Contact,
    avatarFile: File?,
    onVideoClick: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(EldCard)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (avatarFile != null && avatarFile.exists()) {
            AsyncImage(
                model = avatarFile,
                contentDescription = contact.name,
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFDFE7EF)),
                contentAlignment = Alignment.Center,
            ) {
                Text(contact.name.take(1), color = EldInk,
                    style = MaterialTheme.typography.headlineMedium)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            contact.name, color = EldInk,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        if (!contact.wechatRemark.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text("微信：${contact.wechatRemark}", color = EldMuted,
                style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        Spacer(Modifier.height(12.dp))
        if (onVideoClick != null) {
            Text(
                "视频",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(EldPhone)
                    .clickable { onVideoClick() }
                    .padding(vertical = 14.dp),
            )
        }
    }
}

private fun avatarOf(context: Context, name: String?): File? {
    if (name.isNullOrEmpty()) return null
    return File(context.filesDir, "preset/photos/$name")
}
