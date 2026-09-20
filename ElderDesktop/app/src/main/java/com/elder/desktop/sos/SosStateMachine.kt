package com.elder.desktop.sos

/**
 * SOS 状态机（技术方案 §4.5）。纯 Kotlin，零 Android 依赖，可在 JVM 单测中驱动。
 *
 * ```
 * Idle ──点SOS──▶ Confirm1
 * Confirm1 ──取消──▶ Idle
 * Confirm1 ──点确认──▶ Countdown(5s)
 * Countdown ──取消──▶ Idle
 * Countdown ──归零──▶ Triggered
 * ```
 * Triggered 后进入冷却期，冷却内重复按不响应；冷却结束后可再次发起。
 */
sealed interface SosState {
    data object Idle : SosState
    data object Confirm1 : SosState
    data class Countdown(val remainingSeconds: Int) : SosState
    data object Triggered : SosState
}

class SosStateMachine(
    private val countdownSeconds: Int = DEFAULT_COUNTDOWN_SECONDS,
    private val cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    var state: SosState = SosState.Idle
        private set

    private var lastTriggerAt: Long = 0L
    private var hasTriggered: Boolean = false

    /** 点 SOS / 确认按钮。 */
    fun onSosPressed() {
        when (state) {
            SosState.Idle -> state = SosState.Confirm1
            SosState.Confirm1 -> state = SosState.Countdown(countdownSeconds)
            is SosState.Countdown -> Unit // 倒计时中忽略，防误触
            SosState.Triggered -> {
                if (!inCooldown()) state = SosState.Confirm1
            }
        }
    }

    /** 取消按钮：从确认或倒计时回到 Idle。 */
    fun onCancel() {
        if (state is SosState.Confirm1 || state is SosState.Countdown) {
            state = SosState.Idle
        }
    }

    /**
     * 倒计时走一秒。
     * @return true 表示倒计时归零、应触发拨打+短信。
     */
    fun onTick(): Boolean {
        val current = state
        if (current !is SosState.Countdown) return false
        val remaining = current.remainingSeconds - 1
        return if (remaining <= 0) {
            lastTriggerAt = clock()
            hasTriggered = true
            state = SosState.Triggered
            true
        } else {
            state = SosState.Countdown(remaining)
            false
        }
    }

    fun inCooldown(): Boolean =
        hasTriggered && clock() - lastTriggerAt < cooldownMillis

    /** 触发动作被确认执行后调用（如已完成拨号/短信）。 */
    fun resetAfterTrigger() {
        state = SosState.Idle
    }

    companion object {
        const val DEFAULT_COUNTDOWN_SECONDS = 5
        const val DEFAULT_COOLDOWN_MILLIS = 30_000L
    }
}
