package com.elder.desktop.wxvideo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
 * 触摸穿透设计（关键，解决"悬浮窗锁死屏幕、无法操作微信"）：
 *  - 全屏 UI 层：FLAG_NOT_TOUCHABLE，只显示半透明遮罩 + 提示，完全不接收触摸，触摸穿透到微信；
 *  - 十字光标：独立小窗口（FLAG_NOT_TOUCH_MODAL），可拖动，窗口外触摸穿透；
 *  - 底部按钮：独立小窗口（FLAG_NOT_TOUCH_MODAL），可点击，窗口外触摸穿透。
 *  这样子女能自由操作微信（切应用、进搜索页、长按输入框），只有光标 / 按钮可点。
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
    // 统一用 getRealMetrics(物理全屏宽高) 做归一化/反归一化，与 CalibrationScreen/Service 的 cal.screenW/H 一致。
    // 之前用 displayMetrics(可能不含导航栏/状态栏) 会导致光标记录坐标与实际点击坐标系统性偏差，
    // 表现为"光标对准了目标、点击却偏到别处"。
    private val screenW: Int
    private val screenH: Int

    init {
        val (w, h) = realMetrics(context)
        screenW = w
        screenH = h
    }
    private val density = context.resources.displayMetrics.density

    private var callback: Callback? = null
    private var currentIndex = 0
    // 0 待记录  1 待验证  2 通过
    private var phase = 0

    private lateinit var prompt: TextView
    private lateinit var hint: TextView
    private lateinit var recordBtn: Button
    private lateinit var verifyBtn: Button
    private lateinit var yesBtn: Button
    private lateinit var noBtn: Button
    private lateinit var prevBtn: Button
    private lateinit var doneBtn: Button

    private val cursorW = (28 * density).toInt()
    private val cursorH = (28 * density).toInt()

    private val uiView: View = buildUi()
    private val btnView: View = buildButtons()
    private val cursorView: View = buildCursor()

    // 窗口参数
    private lateinit var uiParams: WindowManager.LayoutParams
    private lateinit var btnParams: WindowManager.LayoutParams
    private lateinit var cursorParams: WindowManager.LayoutParams

    private var cursorX = 0.5f
    private var cursorY = 0.45f

    private fun buildUi(): View {
        val root = FrameLayout(context).apply {
            setBackgroundColor(0x22000000.toInt())
        }
        prompt = TextView(context).apply {
            setBackgroundColor(0x8C1A2A3A.toInt())
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding((16 * density).toInt(), (12 * density).toInt(),
                (16 * density).toInt(), (12 * density).toInt())
            setText("校准")
        }
        // 指引条限定在左上角约 55% 宽度、自动换行，避免横跨全屏盖住微信顶部右上角（放大镜/搜索框）等校准目标。
        root.addView(prompt, FrameLayout.LayoutParams(
            (screenW * 0.55f).toInt(), FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.START
        })

        hint = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            setBackgroundColor(0x59000000.toInt())
            setPadding((8 * density).toInt(), (6 * density).toInt(),
                (8 * density).toInt(), (6 * density).toInt())
            text = "拖动橙色光标对准目标，再点「记录位置」"
        }
        root.addView(hint, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            topMargin = (150 * density).toInt()
        })
        return root
    }

    private fun buildButtons(): View {
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
        yesBtn = button("是(通过)") { callback?.onVerified(currentIndex) }
        noBtn = button("否(重录)") { callback?.onRedo(currentIndex); phase = 0; refresh() }
        doneBtn = button("完成校准") { callback?.onComplete() }
        row2.addView(yesBtn); row2.addView(noBtn); row2.addView(doneBtn)

        btnCol.addView(row1)
        btnCol.addView(row2)
        return btnCol
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildCursor(): View {
        // 中心十字准星：明确指出「实际记录/点击点 = 方框正中心」，避免以为整个方框都是有效区。
        return object : View(context) {
            private val paint = Paint()
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val cx = width / 2f
                val cy = height / 2f
                val arm = width / 2f - 2f * density
                // 黑描边 + 白十字，保证任何底色上都清晰。
                paint.color = Color.BLACK
                paint.strokeWidth = 4f * density
                canvas.drawLine(cx - arm, cy, cx + arm, cy, paint)
                canvas.drawLine(cx, cy - arm, cx, cy + arm, paint)
                paint.color = Color.WHITE
                paint.strokeWidth = 2f * density
                canvas.drawLine(cx - arm, cy, cx + arm, cy, paint)
                canvas.drawLine(cx, cy - arm, cx, cy + arm, paint)
                // 中心点
                paint.color = Color.BLACK
                canvas.drawCircle(cx, cy, 2f * density, paint)
            }
        }.apply {
            setBackgroundColor(0xCCFF6D3A.toInt())
            alpha = 0.95f
            setOnTouchListener { _, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        // 按下点在光标内的偏移 = 窗口左上角坐标 - 手指屏幕坐标
                        dragDx = cursorParams.x - e.rawX
                        dragDy = cursorParams.y - e.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        cursorParams.x = (e.rawX + dragDx).toInt()
                        cursorParams.y = (e.rawY + dragDy).toInt()
                        wm.updateViewLayout(cursorView, cursorParams)
                    }
                    MotionEvent.ACTION_UP -> {
                        // 记录光标在屏幕的【真实物理位置】(含状态栏)：getLocationOnScreen 反映 view 实际渲染位置。
                        // 不能用 cursorParams 推断——TYPE_APPLICATION_OVERLAY 的 LayoutParams.y 按内容区(不含状态栏)
                        // 解释，而拖动用 rawY(含状态栏)，会系统性少一个状态栏高度，导致记录坐标偏上、点击偏到上方。
                        val loc = IntArray(2)
                        cursorView.getLocationOnScreen(loc)
                        cursorX = ((loc[0] + cursorW / 2f) / screenW).coerceIn(0f, 1f)
                        cursorY = ((loc[1] + cursorH / 2f) / screenH).coerceIn(0f, 1f)
                    }
                }
                true
            }
        }
    }

    private var dragDx = 0f
    private var dragDy = 0f

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
        // 层级：UI 层在最下，按钮层中间，光标层最上
        uiParams = windowParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            Gravity.TOP or Gravity.START, 0, 0,
        )
        wm.addView(uiView, uiParams)

        btnParams = windowParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 0,
        )
        wm.addView(btnView, btnParams)

        cursorParams = windowParams(
            cursorW, cursorH,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            Gravity.TOP or Gravity.START,
            (screenW - cursorW) / 2, (screenH * 0.45f).toInt(),
        )
        wm.addView(cursorView, cursorParams)
    }

    fun dismiss() {
        runCatching { wm.removeView(uiView) }
        runCatching { wm.removeView(btnView) }
        runCatching { wm.removeView(cursorView) }
    }

    private fun windowParams(
        w: Int, h: Int, flags: Int, gravity: Int, x: Int, y: Int,
    ): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            w, h,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            flags,
            PixelFormat.TRANSLUCENT,
        ).apply { this.gravity = gravity; this.x = x; this.y = y }

    /** 物理全屏宽高（与点击/校准存储一致）。 */
    private fun realMetrics(context: Context): Pair<Int, Int> = try {
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
