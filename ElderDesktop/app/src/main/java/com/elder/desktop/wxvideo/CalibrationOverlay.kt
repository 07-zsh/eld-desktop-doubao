package com.elder.desktop.wxvideo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 功能2 校准向导的悬浮窗（子女在微信界面上拖动十字光标定位按钮坐标）。
 *
 * 交互（A1 全手动 + 边录边验）：
 *  - 顶部显示当前步骤提示；
 *  - 子女把可拖动十字光标对准微信里的目标按钮；
 *  - 点「记录位置」保存坐标 → 点「验证此步」触发无障碍单步回放 → 观察微信是否命中 → 点「是/否」；
 *  - 是则进入下一步，否则重录；7 步全部通过后点「完成校准」。
 *
 * 坐标全部以相对比例（0~1）记录，跨分辨率可用。
 */
class CalibrationOverlay(private val context: Context) {

    interface Callback {
        fun onRecorded(index: Int, x: Float, y: Float)
        fun onVerifyRequested(index: Int)
        fun onVerified(index: Int)
        fun onRedo(index: Int)
        fun onStepChanged(index: Int)
        fun onComplete()
        fun onCancel()
    }

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val screenW = context.resources.displayMetrics.widthPixels
    private val screenH = context.resources.displayMetrics.heightPixels
    private val density = context.resources.displayMetrics.density

    private var callback: Callback? = null
    private var currentIndex = 0
    // 0 待记录  1 待验证  2 通过
    private var phase = 0

    private lateinit var prompt: TextView
    private lateinit var hint: TextView
    private lateinit var cursor: View
    private lateinit var recordBtn: Button
    private lateinit var verifyBtn: Button
    private lateinit var yesBtn: Button
    private lateinit var noBtn: Button
    private lateinit var prevBtn: Button
    private lateinit var doneBtn: Button

    private val cursorW = (48 * density).toInt()
    private val cursorH = (48 * density).toInt()

    private val overlayView: FrameLayout = buildOverlay()

    @SuppressLint("ClickableViewAccessibility")
    private fun buildOverlay(): FrameLayout {
        val root = FrameLayout(context).apply {
            setBackgroundColor(0x22000000.toInt())
            setOnTouchListener { _, _ -> true } // 拦截穿透，避免误触微信
        }

        prompt = TextView(context).apply {
            setBackgroundColor(0xEE1A2A3A.toInt())
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding((16 * density).toInt(), (12 * density).toInt(),
                (16 * density).toInt(), (12 * density).toInt())
            setText("校准")
        }
        root.addView(prompt, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP
        })

        // 可拖动十字光标（FrameLayout 内自由定位）
        cursor = View(context).apply {
            setBackgroundColor(0xFFFF6D3A.toInt())
            alpha = 0.9f
            setOnTouchListener { v, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        v.x += e.rawX - lastRawX
                        v.y += e.rawY - lastRawY
                    }
                    MotionEvent.ACTION_UP -> {
                        cursorX = ((v.x + cursorW / 2f) / screenW).coerceIn(0f, 1f)
                        cursorY = ((v.y + cursorH / 2f) / screenH).coerceIn(0f, 1f)
                    }
                }
                lastRawX = e.rawX
                lastRawY = e.rawY
                true
            }
        }
        cursor.x = ((screenW - cursorW) / 2f)
        cursor.y = (screenH * 0.45f)
        root.addView(cursor, FrameLayout.LayoutParams(cursorW, cursorH))

        hint = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            setBackgroundColor(0xAA000000.toInt())
            setPadding((8 * density).toInt(), (6 * density).toInt(),
                (8 * density).toInt(), (6 * density).toInt())
            text = "拖动橙色光标对准目标，再点「记录位置」"
        }
        root.addView(hint, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            topMargin = (120 * density).toInt()
        })

        val btnCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val row1 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        prevBtn = button("上一步") { callback?.onStepChanged(currentIndex - 1) }
        recordBtn = button("记录位置") { onRecord() }
        verifyBtn = button("验证此步") { callback?.onVerifyRequested(currentIndex); phase = 1; refresh() }
        row1.addView(prevBtn); row1.addView(recordBtn); row1.addView(verifyBtn)

        val row2 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        yesBtn = button("是(通过)") { callback?.onVerified(currentIndex); phase = 2; refresh() }
        noBtn = button("否(重录)") { callback?.onRedo(currentIndex); phase = 0; refresh() }
        doneBtn = button("完成校准") { callback?.onComplete() }
        row2.addView(yesBtn); row2.addView(noBtn); row2.addView(doneBtn)

        btnCol.addView(row1)
        btnCol.addView(row2)
        root.addView(btnCol, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        })
        return root
    }

    private var lastRawX = 0f
    private var lastRawY = 0f
    private var cursorX = 0.5f
    private var cursorY = 0.5f

    private fun button(text: String, onClick: () -> Unit): Button =
        Button(context).apply {
            this.text = text
            setOnClickListener { onClick() }
        }

    private fun onRecord() {
        callback?.onRecorded(currentIndex, cursorX, cursorY)
        phase = 1
        refresh()
    }

    fun setStep(index: Int, name: String) {
        currentIndex = index
        phase = 0
        prompt.text = "第 ${index + 1}/7 步：$name"
        refresh()
    }

    private fun refresh() {
        verifyBtn.visibility = if (phase == 0) View.GONE else View.VISIBLE
        recordBtn.visibility = if (phase == 0) View.VISIBLE else View.GONE
        yesBtn.visibility = if (phase == 1) View.VISIBLE else View.GONE
        noBtn.visibility = if (phase == 1) View.VISIBLE else View.GONE
        doneBtn.visibility = if (phase == 2 && currentIndex == 6) View.VISIBLE else View.GONE
        prevBtn.visibility = if (currentIndex == 0) View.GONE else View.VISIBLE
        hint.text = when (phase) {
            0 -> "拖动橙色光标对准微信里的目标，再点「记录位置」"
            1 -> "已记录：点「验证此步」让系统回放，观察微信是否命中，再点「是」或「否」"
            else -> "本步已验证通过，可继续下一步"
        }
    }

    fun show(cb: Callback) {
        callback = cb
        refresh()
        wm.addView(overlayView, overlayParams())
    }

    fun dismiss() {
        runCatching { wm.removeView(overlayView) }
    }

    private fun overlayParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }
}
