package com.elder.desktop.wxvideo

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import com.elder.desktop.data.local.CalibrationPoint
import com.elder.desktop.data.local.WechatCalibration

/**
 * 功能2「一键微信视频」无障碍服务。
 *
 * 机制（真机 Redmi Note 8 / 微信 8.0.76 实测验证）：
 *  真实微信屏蔽无障碍节点树，无法按备注定位；故不读节点，仅用 dispatchGesture 坐标手势 +
 *  剪贴板粘贴联系人备注 + 微信顶部搜索路径，逐级回放子女校准好的 7 步坐标。
 *
 * 时序控制（事件驱动 + 延时兜底，取代纯固定延时）：
 *  微信会向本服务投递 TYPE_WINDOW_STATE_CHANGED（带 className + text），据实测定为两类信号：
 *    - ChattingUI（com.tencent.mm.ui.chatting.ChattingUI）＝ 已进入目标聊天
 *    - dialog.a4 + 文本含「视频通话」＝ 通话类型菜单已弹出
 *  在这两处最脆弱的过渡用「等信号再点」，超时则回退按原延时执行；搜索段（放大镜/长按/粘贴/结果）
 *  无稳定类名信号，保留固定延时兜底。链路（自拉起微信起）：
 *    拉起微信(CLEAR_TASK 强制回干净聊天列表, 等 LauncherUI 主界面) → 点放大镜 → 长按搜索框(粘贴备注) → 点粘贴 → 点搜索结果联系人 →
 *    【等 ChattingUI】→ 点右下角加号 → 点面板"视频通话" →
 *    【等 dialog.a4】→ 点菜单"视频通话" → 发起呼叫。
 */
class WechatVideoCallService : AccessibilityService() {

    companion object {
        private const val TAG = "WechatVideoCall"

        /** 供家人页触发使用；服务未连接时为 null。 */
        @Volatile
        var instance: WechatVideoCallService? = null
            private set

        /** 无障碍服务是否已在系统设置开启（长辈可点视频按钮的硬前提之一）。 */
        fun isEnabled(context: Context): Boolean {
            val expected = context.packageName + "/" + WechatVideoCallService::class.java.name
            val enabled = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )
            return enabled?.split(':')?.any { it.equals(expected, ignoreCase = true) } ?: false
        }

        private const val TAP_DURATION = 80
        private const val LONG_PRESS_DURATION = 600

        // 各步骤之间的间隔（毫秒）。长按后粘贴气泡会快速消失，须短延迟点击。
        private const val AFTER_ICON_MS = 2000L
        // 长按搜索框(600ms)完成 + 气泡稳定(300ms)后再点粘贴；之前 400ms 在长按未结束时就去点，
        // 气泡还没弹出导致粘贴失败（搜索框为空）。
        private const val AFTER_LONG_PRESS_MS = 900L
        private const val AFTER_PASTE_MS = 2500L
        private const val AFTER_CONTACT_MS = 2500L
        private const val AFTER_PLUS_MS = 2000L
        private const val AFTER_PANEL_VIDEO_MS = 3000L
        /** ChattingUI 信号命中后，聊天页仍需时间渲染加号按钮，否则 step5 手势被忽略。 */
        private const val CHAT_UI_SETTLE_MS = 1500L
        /** dialog.a4 菜单信号命中后，等菜单动画完成再点 step7，否则点空。 */
        private const val CALL_MENU_SETTLE_MS = 500L
        private const val RESTORE_CLIPBOARD_MS = 2500L
        private const val TOTAL_TIMEOUT_MS = 45000L

        // 硬遮窗（防老人无意识误触）：手势前临时切穿透，等 WMS 处理完再下发。
        private const val PENETRATE_SETTLE_MS = 120L

        // 事件驱动信号（真机实测 className）。
        private const val CLASS_CHATTING_UI = "com.tencent.mm.ui.chatting.ChattingUI"
        private const val CLASS_CALL_MENU = "com.tencent.mm.ui.widget.dialog.a4"
        /** 微信聊天列表主界面（7 步起点的界面）；拉起微信后等它出现再开始第 1 步。 */
        private const val CLASS_WECHAT_MAIN = "com.tencent.mm.ui.LauncherUI"
        private const val WECHAT_PACKAGE = "com.tencent.mm"
        /** 拉起微信后等主界面信号的超时兜底（冷启动偏慢）；超时则直接开始第 1 步。 */
        private const val WECHAT_LAUNCH_FALLBACK_MS = 8000L
        /** LauncherUI 信号命中后、列表可能仍在加载，等其渲染稳定再点放大镜。 */
        private const val WECHAT_LIST_SETTLE_MS = 1200L
        internal const val SIGNAL_CHAT = 1
        internal const val SIGNAL_CALL_MENU = 2
        internal const val SIGNAL_WECHAT_MAIN = 3

        /**
         * 信号匹配（纯函数，可单测）：判断收到的窗口事件是否命中当前等待的信号。
         * @param waitingSignal 当前等待的信号（SIGNAL_CHAT / SIGNAL_CALL_MENU / 0）
         * @param cls 事件 className
         * @param text 事件携带文本（call menu 需含「视频通话」以与其它对话框区分）
         */
        fun matchSignal(waitingSignal: Int, cls: String, text: List<CharSequence>?): Boolean =
            when (waitingSignal) {
                SIGNAL_CHAT -> cls == CLASS_CHATTING_UI
                SIGNAL_CALL_MENU -> cls == CLASS_CALL_MENU &&
                    text?.any { it.toString().contains("视频通话") } == true
                SIGNAL_WECHAT_MAIN -> cls == CLASS_WECHAT_MAIN
                else -> false
            }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var clipboardBackup: String? = null
    /** 当前正在等待的信号（0=不在等待）；命中后执行 pendingStep。 */
    private var waitingSignal = 0
    /** 信号命中后待执行的下一步。 */
    private var pendingStep: Runnable? = null

    // 硬遮窗（防老人无意识误触）：全屏可触摸拦截 + 大字提示。
    // 自动拨打期间显示；每次手势前临时切 FLAG_NOT_TOUCHABLE 让手势穿透到微信，完成后恢复拦截。
    private var shieldView: View? = null
    private var shieldParams: WindowManager.LayoutParams? = null
    private var shieldPid = 0

    private val timeoutRunnable = Runnable {
        Log.w(TAG, "call timeout, reset")
        reset()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "service connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        hideShield()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!running || waitingSignal == 0) return
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val cls = event.className?.toString().orEmpty()
        if (matchSignal(waitingSignal, cls, event.text)) {
            Log.i(TAG, "signal received waiting=$waitingSignal cls=$cls")
            waitingSignal = 0
            val r = pendingStep
            pendingStep = null
            r?.run()
        }
    }

    override fun onInterrupt() {
    }

    /** 是否正在执行一轮拨号。 */
    fun isRunning(): Boolean = running

    /**
     * 校准用的单步回放：仅对第 [index] 步（0~6）执行手势，供「边录边验」确认坐标是否命中。
     * 校准阶段目标界面由子女手动切换（例如已停在搜索页时验证第 2 步长按）。
     */
    fun replayStep(index: Int, cal: WechatCalibration): Boolean {
        if (index !in 0..6) {
            Log.w(TAG, "replayStep invalid index=$index")
            return false
        }
        val pt = cal.steps[index]
        if (index == 1) {
            // 第 2 步（长按搜索框呼出「粘贴」）：直接长按 0.6s。
            longPressSearchBox(pt, cal)
            Log.i(
                TAG,
                "replayStep index=1 longPress at ${pt.x},${pt.y} screen=${cal.screenW}x${cal.screenH}",
            )
            return true
        }
        val ok = dispatchGestureAt(pt, cal, TAP_DURATION)
        Log.i(
            TAG,
            "replayStep index=$index at ${pt.x},${pt.y} screen=${cal.screenW}x${cal.screenH} dur=$TAP_DURATION ok=$ok",
        )
        return ok
    }

    /**
     * 对指定微信备注发起一轮视频通话回放。
     * @return 是否已成功启动（服务已连接且不在运行中）
     */
    fun startCall(remark: String, cal: WechatCalibration): Boolean {
        if (running || !cal.calibrated || remark.isBlank()) return false
        if (cal.steps.size != 7) return false
        running = true
        Log.i(TAG, "startCall remark=$remark screen=${cal.screenW}x${cal.screenH}")

        clipboardBackup = readClipboard()
        writeClipboard(remark.trim())

        // 防老人无意识误触：显示全屏硬遮窗（拦截触摸 + 大字提示），reset 时移除。
        showShield()

        // 关键前提修复：7 步坐标从「微信聊天列表」起算，但点「视频」时手机并不在微信。
        // 故先拉起微信到前台（聊天列表），等微信主界面（LauncherUI）信号命中后再开始第 1 步；
        // 冷启动慢/类名未命中时超时兜底直接开始。
        launchWechat()
        gateStartAfterWechat(cal)
        handler.postDelayed({ finishWithTimeout() }, TOTAL_TIMEOUT_MS)
        return true
    }

    /**
     * 拉起微信到前台并强制回到干净聊天列表。
     * 必须用 packageManager.getLaunchIntentForPackage 解析出的 intent（直接拼 ACTION_MAIN +
     * CATEGORY_LAUNCHER + setPackage 的隐式 intent 在 Android 11+ 因包可见性会 resolve 到 null，
     * 导致微信无法打开）；在其上叠加 FLAG_ACTIVITY_CLEAR_TASK，避免微信恢复旧聊天/搜索界面
     * 而让校准坐标对不上。
     */
    private fun launchWechat() {
        val ok = runCatching {
            val launcher = packageManager.getLaunchIntentForPackage(WECHAT_PACKAGE)
                ?: throw IllegalStateException("wechat launch intent not resolvable")
            launcher.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(launcher)
        }.isSuccess
        Log.i(TAG, "launch wechat (clear task) ok=$ok")
    }

    /** 等微信主界面信号命中后，先等列表渲染稳定，再开始第 1 步（点放大镜）。 */
    private fun gateStartAfterWechat(cal: WechatCalibration) {
        waitingSignal = SIGNAL_WECHAT_MAIN
        pendingStep = Runnable {
            waitingSignal = 0
            handler.postDelayed({ runStep(0, cal) }, WECHAT_LIST_SETTLE_MS)
        }
        handler.postDelayed({
            if (waitingSignal == SIGNAL_WECHAT_MAIN) {
                Log.w(TAG, "wechat main signal timeout, fallback start")
                waitingSignal = 0
                val r = pendingStep
                pendingStep = null
                r?.run()
            }
        }, WECHAT_LAUNCH_FALLBACK_MS)
    }

    /** 执行指定步骤并按策略推进下一步（延时 or 等信号）。 */
    private fun runStep(index: Int, cal: WechatCalibration) {
        if (!running) return
        val pt = cal.steps[index]
        if (index == 1) {
            // 长按搜索框：先轻点聚焦，再长按，确保呼出「粘贴」（与校准 replayStep 一致）。
            longPressSearchBox(pt, cal)
            Log.i(TAG, "step2 (longPressSearchBox) longPress at ${pt.x},${pt.y}")
        } else {
            val ok = dispatchGestureAt(pt, cal, TAP_DURATION)
            Log.i(TAG, "step${index + 1} (${stepName(index)}) at ${pt.x},${pt.y} ok=$ok")
        }
        when (index) {
            // 点完联系人结果：等 ChattingUI（已进聊天）再点「＋」，避免点空。
            // 信号命中后再等 CHAT_UI_SETTLE_MS 让聊天页渲染完加号按钮。
            3 -> gateNext(4, cal, SIGNAL_CHAT, AFTER_CONTACT_MS, CHAT_UI_SETTLE_MS)
            // 点完面板"视频通话"：等 dialog.a4 通话类型菜单弹出再点菜单"视频通话"确认。
            // 新版微信点面板"视频通话"直接拨号、不弹菜单，此时超时后【不要】执行 step7——
            // step7 在拨号界面上会误点挂断/取消，把刚拨出的电话挂掉（聊天记录满屏"已取消"的根因）。
            5 -> gatePanelVideoOrDirectDial(cal)
            // 全部完成：延时恢复剪贴板并复位。
            6 -> handler.postDelayed({ reset() }, RESTORE_CLIPBOARD_MS)
            else -> schedule(index + 1, cal, afterDelayMs(index))
        }
    }

    private fun schedule(index: Int, cal: WechatCalibration, delay: Long) {
        if (!running) return
        handler.postDelayed({ runStep(index, cal) }, delay)
    }

    /** 非信号门控步骤之间沿用固定延时（搜索段无稳定类名信号）。 */
    private fun afterDelayMs(index: Int): Long = when (index) {
        0 -> AFTER_ICON_MS
        1 -> AFTER_LONG_PRESS_MS
        2 -> AFTER_PASTE_MS
        4 -> AFTER_PLUS_MS
        else -> 0L
    }

    /** 信号门控：等 [signal] 命中后执行第 [index] 步；超时 [fallbackMs] 回退直接执行。 */
    private fun gateNext(index: Int, cal: WechatCalibration, signal: Int, fallbackMs: Long, settleAfterSignalMs: Long = 0L) {
        if (!running) return
        waitingSignal = signal
        pendingStep = Runnable {
            waitingSignal = 0
            handler.postDelayed({ runStep(index, cal) }, settleAfterSignalMs)
        }
        handler.postDelayed({
            if (waitingSignal == signal) {
                Log.w(TAG, "signal $signal timeout for step${index + 1}, fallback")
                waitingSignal = 0
                val r = pendingStep
                pendingStep = null
                r?.run()
            }
        }, fallbackMs)
    }

    /**
     * 点完面板"视频通话"后的两种走向：
     *  - 旧版微信：弹 dialog.a4 通话类型菜单（含"视频通话"文本）→ 点 step7 确认；
     *  - 新版微信：直接拨号，不弹菜单 → 超时后【跳过 step7】，否则 step7 在拨号界面误点挂断。
     * 无论哪种，最终都走 reset 复位。
     */
    private fun gatePanelVideoOrDirectDial(cal: WechatCalibration) {
        if (!running) return
        waitingSignal = SIGNAL_CALL_MENU
        pendingStep = Runnable {
            waitingSignal = 0
            // dialog.a4 菜单弹出了，等动画完成再点 step7"视频通话"确认
            handler.postDelayed({ runStep(6, cal) }, CALL_MENU_SETTLE_MS)
        }
        handler.postDelayed({
            if (waitingSignal == SIGNAL_CALL_MENU) {
                Log.i(TAG, "call menu not appeared (direct dial), skip confirm step7")
                waitingSignal = 0
                pendingStep = null
                // 不做点 step7，等拨号界面稳定后复位
                handler.postDelayed({ reset() }, 3000)
            }
        }, AFTER_PANEL_VIDEO_MS)
    }

    private fun stepName(index: Int): String = when (index) {
        0 -> "searchIcon"
        1 -> "longPressSearchBox"
        2 -> "pasteBtn"
        3 -> "contact"
        4 -> "plus"
        5 -> "panelVideo"
        else -> "confirmVideo"
    }

    /** 搜索框直接长按呼出「粘贴」（长按 0.6s）。 */
    private fun longPressSearchBox(pt: CalibrationPoint, cal: WechatCalibration) {
        dispatchGestureAt(pt, cal, LONG_PRESS_DURATION)
    }

    /**
     * 下发坐标手势。若遮窗处于激活状态（自动拨打流程），先临时切穿透再下发，
     * 用 GestureResultCallback 检测是否真正送达；被拦截(completed=false)则重试。
     * 校准 replayStep 时无遮窗，走原始直发逻辑（无穿透、无重试）。
     */
    private fun dispatchGestureAt(pt: CalibrationPoint, cal: WechatCalibration, duration: Int): Boolean {
        return try {
            val x = pt.x * cal.screenW
            val y = pt.y * cal.screenH
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0, duration.toLong())
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            if (shieldView == null) {
                dispatchGesture(gesture, null, handler)
            } else {
                dispatchShieldedGesture(gesture, duration.toLong())
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "dispatchGesture error: ${e.message}")
            false
        }
    }

    /**
     * 遮窗激活时的穿透式手势：切穿透 → 等 WMS 处理 → 下发 → 手势时长 + 余量后恢复拦截。
     * 未用 GestureResultCallback（该回调在此 SDK 的 Kotlin 映射下 override 签名不匹配），
     * 改用固定延时恢复拦截；穿透前已留 PENETRATE_SETTLE_MS 等待 WMS，被遮窗拦截概率已很低。
     */
    private fun dispatchShieldedGesture(gesture: GestureDescription, duration: Long) {
        if (!running) return
        setShieldTouchable(false)
        handler.postDelayed({
            if (!running) return@postDelayed
            try {
                dispatchGesture(gesture, null, handler)
            } catch (e: Exception) {
                Log.e(TAG, "dispatchGesture error: ${e.message}")
            }
            // 手势完成后（duration + 余量）恢复拦截
            handler.postDelayed({
                if (running) setShieldTouchable(true)
            }, duration + PENETRATE_SETTLE_MS + 80L)
        }, PENETRATE_SETTLE_MS)
    }

    /**
     * 显示全屏硬遮窗：拦截老人无意识误触 + 大字提示。
     * 默认可触摸拦截；手势前由 setShieldTouchable(false) 临时穿透。
     * 无悬浮窗权限时静默跳过（仅记录，不阻断拨打——遮窗是防护而非必需）。
     */
    private fun showShield() {
        if (shieldView != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)
        ) {
            Log.w(TAG, "no overlay permission, skip shield")
            return
        }
        runCatching {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val tv = TextView(this).apply {
                setBackgroundColor(0x99000000.toInt())
                setTextColor(Color.WHITE)
                textSize = 30f
                gravity = Gravity.CENTER
                text = "正在拨打视频\n请勿触摸屏幕"
            }
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT,
            ).apply { gravity = Gravity.CENTER }
            wm.addView(tv, lp)
            shieldView = tv
            shieldParams = lp
            Log.i(TAG, "shield shown")
        }.onFailure { Log.e(TAG, "showShield error: ${it.message}") }
    }

    /** 隐藏并移除遮窗（幂等）。 */
    private fun hideShield() {
        val v = shieldView ?: return
        shieldView = null
        shieldParams = null
        runCatching {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(v)
        }
        Log.i(TAG, "shield hidden")
    }

    /** 切换遮窗触摸模式：true=可触摸拦截(默认)，false=FLAG_NOT_TOUCHABLE 穿透。 */
    private fun setShieldTouchable(touchable: Boolean) {
        val lp = shieldParams ?: return
        lp.flags = if (touchable) {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        runCatching {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).updateViewLayout(shieldView, lp)
        }.onFailure { Log.e(TAG, "setShieldTouchable error: ${it.message}") }
    }

    private fun writeClipboard(text: String) {
        runCatching {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("elder_wx", text))
        }
    }

    private fun readClipboard(): String? = runCatching {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
    }.getOrNull()

    private fun finishWithTimeout() {
        if (running) {
            Log.w(TAG, "timeout reached, reset")
            reset()
        }
    }

    /** 复位：停止本轮、清信号、恢复剪贴板、移除遮窗。 */
    private fun reset() {
        running = false
        waitingSignal = 0
        pendingStep = null
        handler.removeCallbacksAndMessages(null)
        clipboardBackup?.let { writeClipboard(it) }
        clipboardBackup = null
        hideShield()
        Log.i(TAG, "reset done")
    }
}
