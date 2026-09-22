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
 * 服务仅开启 canPerformGestures（不读窗口内容，最小权限）。
 * 时序链（自聊天列表起）：
 *  点放大镜 → 写剪贴板备注 + 长按搜索框 → 点粘贴 → 点搜索结果联系人 → 进聊天 →
 *  点右下角加号 → 点加号面板"视频通话" → 点确认"视频通话" → 发起呼叫。
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
        private const val AFTER_LONG_PRESS_MS = 500L
        private const val AFTER_PASTE_MS = 2000L
        private const val AFTER_CONTACT_MS = 2500L
        private const val AFTER_PLUS_MS = 2000L
        private const val AFTER_PANEL_VIDEO_MS = 1500L
        private const val RESTORE_CLIPBOARD_MS = 2500L
        private const val TOTAL_TIMEOUT_MS = 30000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var clipboardBackup: String? = null

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
        // 只用手势，不读节点；事件仅用于保证服务存活。
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
        if (index !in 0..6) return false
        val pt = cal.steps[index]
        return dispatchGestureAt(pt, cal, if (index == 1) LONG_PRESS_DURATION else TAP_DURATION)
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

        handler.postDelayed({ tapStep(0, cal, 0L) }, 400)
        handler.postDelayed({ finishWithTimeout() }, TOTAL_TIMEOUT_MS)
        return true
    }

    /** 递归式时序链：执行当前步并安排下一步。 */
    private fun tapStep(index: Int, cal: WechatCalibration, delay: Long) {
        if (!running) return
        handler.postDelayed({
            if (!running) return@postDelayed
            val pt = cal.steps[index]
            val ok = dispatchGestureAt(pt, cal, if (index == 1) LONG_PRESS_DURATION else TAP_DURATION)
            Log.i(TAG, "step${index + 1} (${stepName(index)}) at ${pt.x},${pt.y} ok=$ok")
            val nextDelay = when (index) {
                0 -> AFTER_ICON_MS
                1 -> AFTER_LONG_PRESS_MS
                2 -> AFTER_PASTE_MS
                3 -> AFTER_CONTACT_MS
                4 -> AFTER_PLUS_MS
                5 -> AFTER_PANEL_VIDEO_MS
                else -> 0L
            }
            if (index < 6) {
                tapStep(index + 1, cal, nextDelay)
            } else {
                // 全部步骤完成：延时恢复剪贴板并复位
                handler.postDelayed({ reset() }, RESTORE_CLIPBOARD_MS)
            }
        }, delay)
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

    /** 复位：停止本轮、恢复剪贴板。 */
    private fun reset() {
        running = false
        handler.removeCallbacksAndMessages(null)
        clipboardBackup?.let { writeClipboard(it) }
        clipboardBackup = null
        Log.i(TAG, "reset done")
    }
}
