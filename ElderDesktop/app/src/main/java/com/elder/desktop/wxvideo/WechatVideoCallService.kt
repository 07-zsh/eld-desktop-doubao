package com.elder.desktop.wxvideo

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
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
        private const val AFTER_ICON_MS = 2500L
        private const val AFTER_LONG_PRESS_MS = 500L
        private const val AFTER_PASTE_MS = 2000L
        private const val AFTER_CONTACT_MS = 2500L
        private const val AFTER_PLUS_MS = 2000L
        private const val AFTER_PANEL_VIDEO_MS = 1500L
        private const val RESTORE_CLIPBOARD_MS = 2500L
        private const val TOTAL_TIMEOUT_MS = 45000L

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
        val dur = if (index == 1) LONG_PRESS_DURATION else TAP_DURATION
        val ok = dispatchGestureAt(pt, cal, dur)
        Log.i(
            TAG,
            "replayStep index=$index at ${pt.x},${pt.y} screen=${cal.screenW}x${cal.screenH} dur=$dur ok=$ok",
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
        val dur = if (index == 1) LONG_PRESS_DURATION else TAP_DURATION
        val ok = dispatchGestureAt(pt, cal, dur)
        Log.i(TAG, "step${index + 1} (${stepName(index)}) at ${pt.x},${pt.y} ok=$ok")
        when (index) {
            // 点完联系人结果：等 ChattingUI（已进聊天）再点「＋」，避免点空。
            3 -> gateNext(4, cal, SIGNAL_CHAT, AFTER_CONTACT_MS)
            // 点完面板"视频通话"：等 dialog.a4 菜单弹出再点菜单"视频通话"。
            5 -> gateNext(6, cal, SIGNAL_CALL_MENU, AFTER_PANEL_VIDEO_MS)
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
    private fun gateNext(index: Int, cal: WechatCalibration, signal: Int, fallbackMs: Long) {
        if (!running) return
        waitingSignal = signal
        pendingStep = Runnable {
            waitingSignal = 0
            runStep(index, cal)
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

    private fun stepName(index: Int): String = when (index) {
        0 -> "searchIcon"
        1 -> "longPressSearchBox"
        2 -> "pasteBtn"
        3 -> "contact"
        4 -> "plus"
        5 -> "panelVideo"
        else -> "confirmVideo"
    }

    private fun dispatchGestureAt(pt: CalibrationPoint, cal: WechatCalibration, duration: Int): Boolean {
        return try {
            val x = pt.x * cal.screenW
            val y = pt.y * cal.screenH
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0, duration.toLong())
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, handler)
        } catch (e: Exception) {
            Log.e(TAG, "dispatchGesture error: ${e.message}")
            false
        }
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

    /** 复位：停止本轮、清信号、恢复剪贴板。 */
    private fun reset() {
        running = false
        waitingSignal = 0
        pendingStep = null
        handler.removeCallbacksAndMessages(null)
        clipboardBackup?.let { writeClipboard(it) }
        clipboardBackup = null
        Log.i(TAG, "reset done")
    }
}
