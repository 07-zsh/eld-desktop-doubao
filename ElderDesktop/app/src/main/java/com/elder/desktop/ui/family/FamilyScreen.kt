package com.elder.desktop.ui.family

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elder.desktop.data.di.AppContainer
import com.elder.desktop.data.local.WechatCalibration
import com.elder.desktop.data.model.Contact
import com.elder.desktop.ui.common.ContactRow
import com.elder.desktop.ui.common.TopBar
import com.elder.desktop.ui.theme.EldBg
import com.elder.desktop.ui.theme.EldSub
import com.elder.desktop.util.Caller
import com.elder.desktop.util.Guard
import com.elder.desktop.wxvideo.WechatVideoCallService
import java.io.File

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

    Column(
        modifier = Modifier.fillMaxSize().background(EldBg).padding(horizontal = 22.dp),
    ) {
        TopBar("家人", onBack)
        Text("点一下家人，即可打电话", color = EldSub,
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 10.dp))
        LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)) {
            items(contacts, key = { it.id }) { c ->
                val canVideo = !c.wechatRemark.isNullOrBlank() &&
                    calibration.calibrated && a11yEnabled.value
                ContactRow(
                    contact = c,
                    avatarFile = avatarOf(context, c.avatarFileName),
                    onClick = {
                        Guard.tap(context)
                        Caller.call(context, c.phone)
                    },
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

private fun avatarOf(context: Context, name: String?): File? {
    if (name.isNullOrEmpty()) return null
    return File(context.filesDir, "preset/photos/$name")
}
