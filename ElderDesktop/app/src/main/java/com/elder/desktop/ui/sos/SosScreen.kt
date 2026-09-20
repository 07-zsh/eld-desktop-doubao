package com.elder.desktop.ui.sos

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elder.desktop.data.di.AppContainer
import com.elder.desktop.data.model.EmergencyInfo
import com.elder.desktop.sos.SosState
import com.elder.desktop.sos.SosStateMachine
import com.elder.desktop.ui.common.TopBar
import com.elder.desktop.ui.theme.EldBg
import com.elder.desktop.ui.theme.EldMuted
import com.elder.desktop.ui.theme.EldSos
import com.elder.desktop.ui.theme.EldWhite
import com.elder.desktop.util.Caller
import com.elder.desktop.util.Guard
import com.elder.desktop.util.SmsSender
import kotlinx.coroutines.delay

@Composable
fun SosScreen(container: AppContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val sm = remember { SosStateMachine() }
    var uiState by remember { mutableStateOf<SosState>(sm.state) }
    var triggered by remember { mutableStateOf(false) }

    val emergencyList = remember { mutableStateListOf<EmergencyInfo>() }
    LaunchedEffect(Unit) { emergencyList.addAll(container.sosRepository.emergencyList()) }
    val primary = emergencyList.firstOrNull()

    // 倒计时驱动
    LaunchedEffect(uiState) {
        if (uiState is SosState.Countdown) {
            while (true) {
                delay(1000)
                sm.onTick()
                uiState = sm.state
                if (uiState is SosState.Triggered) break
            }
        }
    }

    // 触发副作用：拨打 + 短信 + 冷却
    LaunchedEffect(uiState) {
        if (uiState is SosState.Triggered && !triggered) {
            triggered = true
            Guard.alert(context)
            primary?.let { Caller.call(context, it.phone) }
            emergencyList.forEach { SmsSender.send(context, it.phone, it.message) }
            container.sosRepository.markTriggered(System.currentTimeMillis())
            delay(4000)
            sm.resetAfterTrigger()
            triggered = false
            uiState = sm.state
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(EldBg).padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopBar("紧急求助", onBack)
        Spacer(Modifier.height(24.dp))

        when (val s = uiState) {
            is SosState.Idle -> SosIdle(primary?.name) {
                sm.onSosPressed(); uiState = sm.state
            }
            is SosState.Confirm1 -> SosConfirm(
                target = primary?.name,
                onConfirm = { sm.onSosPressed(); uiState = sm.state },
                onCancel = { sm.onCancel(); uiState = sm.state },
            )
            is SosState.Countdown -> SosCountdown(s.remainingSeconds) {
                sm.onCancel(); uiState = sm.state
            }
            is SosState.Triggered -> Text("已拨打紧急电话\n并已短信通知家人",
                color = EldWhite, style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable
private fun SosIdle(target: String?, onSos: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("遇到紧急情况？", color = EldWhite,
            style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text("点下方按钮，会打电话给\n${target ?: "紧急联系人"}，并通知家人",
            color = EldMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(36.dp))
        SosBigButton("SOS") { onSos() }
    }
}

@Composable
private fun SosConfirm(target: String?, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("请再点一次确认！", color = EldWhite,
            style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text("再点一次，就会拨打 ${target ?: "紧急联系人"}\n并短信通知家人，防止误触",
            color = EldMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(36.dp))
        SosBigButton("再次确认") { onConfirm() }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = EldWhite.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth().height(72.dp),
        ) {
            Text("取消", color = EldWhite, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SosCountdown(seconds: Int, onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("正在倒计时", color = EldWhite,
            style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("${seconds} 秒后将拨打电话并发送短信",
            color = EldMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(36.dp))
        SosBigButton("取消") { onCancel() }
    }
}

@Composable
private fun SosBigButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(CircleShape)
            .background(EldSos)
            .clip(CircleShape)
            .clickableNoRipple { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = EldWhite, style = MaterialTheme.typography.headlineLarge)
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(clickable(
        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
        indication = null,
    ) { onClick() })
