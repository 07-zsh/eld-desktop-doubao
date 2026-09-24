package com.elder.desktop.ui.calibration

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 功能2 校准向导（子女操作）。7 步校准微信视频回放坐标：
 *  A1 全手动：子女在微信界面用悬浮十字光标对准目标按钮并记录；
 *  边录边验：每步记录后由无障碍服务单步回放，子女确认命中；
 *  B2 真发验证：第 7 步确认「视频通话」后即真正发起呼叫，子女观察是否呼出。
 *
 * 反馈（子女操作的可感知反馈）：
 *  - 每步记录/验证/重录均弹 Toast（「已记录第 X/7 步」「第 X 步验证通过」等）；
 *  - 完成校准弹「校准完成！一键微信视频已可用」并自动保存、返回设置页；
 *  - 从系统「悬浮窗授权」页返回时用 ON_RESUME 刷新权限状态，避免按钮卡在「先授权悬浮窗」。
 */
@Composable
fun CalibrationScreen(container: AppContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val stepNames = listOf(
        "先到微信【聊天列表】，把光标对准右上角「放大镜」（搜索）",
        "点放大镜进【搜索页】，把光标对准顶部「输入框」（长按会呼出粘贴）",
        "长按输入框呼出「粘贴」，把光标对准「粘贴」按钮",
        "输入家人备注（测试甲），把光标对准【搜索结果】里的那一行（点进聊天）",
        "已进【聊天界面】，把光标对准右下角「+」",
        "点「+」弹出面板，把光标对准面板「视频通话」",
        "点面板视频弹出菜单，把光标对准菜单「视频通话」（验证即真呼出）",
    )

    val (screenW, screenH) = remember { realMetrics(context) }
    val wechatVersion = remember { wechatVersion(context) }
    val steps: SnapshotStateList<CalibrationPoint> = remember {
        List(7) { CalibrationPoint(0f, 0f) }.toMutableStateList()
    }
    val overlay = remember { CalibrationOverlay(context) }
    var calibrating by remember { mutableStateOf(false) }
    var canDraw by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    // 从「悬浮窗授权」页返回后刷新权限状态，否则按钮会一直停在「先授权悬浮窗」。
    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDraw = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    // 离开校准页（完成/取消/返回）时确保移除悬浮窗，避免残留窗口点击导致异常退出 App。
    DisposableEffect(overlay) {
        onDispose { overlay.dismiss() }
    }

    // 统一收尾：关闭悬浮窗、保存校准、弹完成反馈、返回设置页。
    fun completeCalibration() {
        overlay.dismiss()
        toast("校准完成！一键微信视频已可用")
        scope.launch {
            container.wechatCalibration.saveCalibration(
                screenW, screenH, wechatVersion, steps.toList(),
            )
        }
        onBack()
    }

    val callback = remember(overlay, steps) {
        object : CalibrationOverlay.Callback {
            override fun onRecorded(index: Int, x: Float, y: Float) {
                steps[index] = CalibrationPoint(x, y)
                toast("已记录第 ${index + 1}/7 步坐标，可点「验证此步」")
            }

            override fun onVerifyRequested(index: Int) {
                val srv = WechatVideoCallService.instance
                if (srv == null) {
                    toast("无障碍服务未开启：请到系统「设置→无障碍→已下载的应用」开启「老年桌面微信视频服务」后重试")
                    return
                }
                val cal = WechatCalibration(
                    screenW = screenW, screenH = screenH, wechatVersion = wechatVersion,
                    steps = steps.toList(), calibrated = true,
                )
                srv.replayStep(index, cal)
                toast("已回放第 ${index + 1} 步，请观察微信是否命中，再点「是」或「否」")
            }

            override fun onVerified(index: Int) {
                toast("第 ${index + 1}/7 步验证通过")
                if (index < 6) {
                    overlay.setStep(index + 1, stepNames[index + 1])
                } else {
                    completeCalibration()
                }
            }

            override fun onRedo(index: Int) {
                toast("第 ${index + 1} 步重录，请重新对准目标")
            }

            override fun onStepChanged(index: Int) {
                if (index in 0..6) overlay.setStep(index, stepNames[index])
            }

            override fun onComplete() {
                completeCalibration()
            }

            override fun onCancel() {
                overlay.dismiss()
                toast("已取消校准")
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
                when {
                    !canDraw -> {
                        canDraw = false
                        openOverlaySettings(context)
                    }
                    !WechatVideoCallService.isEnabled(context) -> {
                        toast("请先到系统「设置→无障碍→已下载的应用」开启「老年桌面微信视频服务」，再开始校准")
                    }
                    !calibrating -> {
                        calibrating = true
                        toast("开始校准：请看手机屏幕悬浮窗，从第 1 步做起")
                        // 预写第一位家人的微信备注到剪贴板，使第 2/3 步「长按输入框→粘贴」能呼出粘贴按钮。
                        scope.launch {
                            val remark = container.contactRepository.observeContacts().first()
                                .firstNotNullOfOrNull {
                                    it.wechatRemark?.trim()?.takeIf { r -> r.isNotEmpty() }
                                }
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("elder_cal", remark ?: "家人"))
                        }
                        overlay.show(callback)
                        overlay.setStep(0, stepNames[0])
                    }
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
