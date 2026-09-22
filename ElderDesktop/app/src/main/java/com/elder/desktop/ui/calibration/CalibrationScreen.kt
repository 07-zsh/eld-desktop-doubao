package com.elder.desktop.ui.calibration

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elder.desktop.data.di.AppContainer
import com.elder.desktop.data.local.CalibrationPoint
import com.elder.desktop.data.local.WechatCalibration
import com.elder.desktop.ui.common.TopBar
import com.elder.desktop.ui.theme.EldBg
import com.elder.desktop.ui.theme.EldCard
import com.elder.desktop.ui.theme.EldInk
import com.elder.desktop.ui.theme.EldSub
import com.elder.desktop.ui.theme.EldWhite
import com.elder.desktop.wxvideo.CalibrationOverlay
import com.elder.desktop.wxvideo.WechatVideoCallService
import kotlinx.coroutines.launch

/**
 * 功能2 校准向导（子女操作）。7 步校准微信视频回放坐标：
 *  A1 全手动：子女在微信界面用悬浮十字光标对准目标按钮并记录；
 *  边录边验：每步记录后由无障碍服务单步回放，子女确认命中；
 *  B2 真发验证：第 7 步确认「视频通话」后即真正发起呼叫，子女观察是否呼出。
 */
@Composable
fun CalibrationScreen(container: AppContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val stepNames = listOf(
        "微信【聊天列表】右上角放大镜（搜索）",
        "微信【搜索页】输入框（长按呼出粘贴）",
        "长按后弹出的「粘贴」按钮",
        "【搜索结果】中的家人（点进聊天）",
        "【聊天界面】右下角「+」",
        "加号面板「视频通话」",
        "确认菜单「视频通话」",
    )

    val (screenW, screenH) = remember { realMetrics(context) }
    val wechatVersion = remember { wechatVersion(context) }
    val steps: SnapshotStateList<CalibrationPoint> = remember {
        List(7) { CalibrationPoint(0f, 0f) }.toMutableStateList()
    }
    val overlay = remember { CalibrationOverlay(context) }
    var calibrating by remember { mutableStateOf(false) }
    var canDraw by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    val callback = remember(overlay, steps) {
        object : CalibrationOverlay.Callback {
            override fun onRecorded(index: Int, x: Float, y: Float) {
                steps[index] = CalibrationPoint(x, y)
            }

            override fun onVerifyRequested(index: Int) {
                val cal = WechatCalibration(
                    screenW = screenW, screenH = screenH, wechatVersion = wechatVersion,
                    steps = steps.toList(), calibrated = true,
                )
                WechatVideoCallService.instance?.replayStep(index, cal)
            }

            override fun onVerified(index: Int) {
                if (index < 6) overlay.setStep(index + 1, stepNames[index + 1])
                else overlay.dismiss()
            }

            override fun onRedo(index: Int) {
                // 重录：由 overlay 内部回到待记录阶段
            }

            override fun onStepChanged(index: Int) {
                if (index in 0..6) overlay.setStep(index, stepNames[index])
            }

            override fun onComplete() {
                overlay.dismiss()
                scope.launch {
                    container.wechatCalibration.saveCalibration(
                        screenW, screenH, wechatVersion, steps.toList(),
                    )
                }
                onBack()
            }

            override fun onCancel() {
                overlay.dismiss()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(EldBg).padding(horizontal = 22.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        TopBar("校准微信视频", onBack)
        Spacer(Modifier.height(12.dp))
        Text(
            "让「一键微信视频」可用：校准一次，长辈之后就能一键打微信视频。\n" +
                "请先确认：已安装并登录微信，且已在系统设置开启「老年桌面微信视频服务」无障碍。",
            color = EldSub, style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Text("步骤说明", color = EldInk, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        stepNames.forEachIndexed { i, n ->
            val done = steps[i].x != 0f || steps[i].y != 0f
            Text(
                "${i + 1}. $n ${if (done) "✓" else ""}",
                color = if (done) EldSub else EldInk,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "屏幕 ${screenW}x${screenH} · 微信 $wechatVersion",
            color = EldSub, style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (!canDraw) {
                    canDraw = false
                    openOverlaySettings(context)
                } else if (!calibrating) {
                    calibrating = true
                    overlay.show(callback)
                    overlay.setStep(0, stepNames[0])
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = EldWhite),
            modifier = Modifier.fillMaxWidth().height(72.dp),
        ) {
            Text(
                when {
                    !canDraw -> "先授权悬浮窗"
                    calibrating -> "校准中（请看手机屏幕悬浮窗）"
                    else -> "开始校准"
                },
                color = EldBg, style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (!canDraw) {
            Spacer(Modifier.height(10.dp))
            Text(
                "需要「悬浮窗」权限来显示校准十字光标。授权后返回本页再点「开始校准」。",
                color = EldSub, style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun realMetrics(context: Context): Pair<Int, Int> {
    return try {
        val dm = android.util.DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.getRealMetrics(dm)
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .defaultDisplay.getRealMetrics(dm)
        }
        Pair(dm.widthPixels, dm.heightPixels)
    } catch (e: Exception) {
        context.resources.displayMetrics.let { Pair(it.widthPixels, it.heightPixels) }
    }
}

private fun wechatVersion(context: Context): String = runCatching {
    context.packageManager.getPackageInfo("com.tencent.mm", 0).versionName
}.getOrNull() ?: "未知"

private fun openOverlaySettings(context: Context) {
    runCatching {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
