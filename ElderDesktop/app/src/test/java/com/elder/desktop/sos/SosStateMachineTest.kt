package com.elder.desktop.sos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SosStateMachineTest {

    private var now = 0L
    private fun machine() = SosStateMachine(
        countdownSeconds = 5,
        cooldownMillis = 30_000L,
        clock = { now },
    )

    @Test
    fun idleToConfirm1OnFirstPress() {
        val sm = machine()
        sm.onSosPressed()
        assertEquals(SosState.Confirm1, sm.state)
    }

    @Test
    fun confirm1ToCountdownOnSecondPress() {
        val sm = machine()
        sm.onSosPressed()
        sm.onSosPressed()
        assertEquals(SosState.Countdown(5), sm.state)
    }

    @Test
    fun cancelFromConfirm1BackToIdle() {
        val sm = machine()
        sm.onSosPressed()
        sm.onCancel()
        assertEquals(SosState.Idle, sm.state)
    }

    @Test
    fun cancelFromCountdownBackToIdle() {
        val sm = machine()
        sm.onSosPressed(); sm.onSosPressed()
        sm.onCancel()
        assertEquals(SosState.Idle, sm.state)
    }

    @Test
    fun tickCountsDownAndTriggersAtZero() {
        val sm = machine()
        sm.onSosPressed(); sm.onSosPressed()
        // 5 -> 4 -> 3 -> 2 -> 1 -> 0(触发)
        assertFalse(sm.onTick()) // 4
        assertEquals(SosState.Countdown(4), sm.state)
        assertFalse(sm.onTick()) // 3
        assertFalse(sm.onTick()) // 2
        assertFalse(sm.onTick()) // 1
        assertTrue(sm.onTick())  // 触发
        assertEquals(SosState.Triggered, sm.state)
    }

    @Test
    fun tickOutsideCountdownDoesNothing() {
        val sm = machine()
        assertFalse(sm.onTick())
        assertEquals(SosState.Idle, sm.state)
    }

    @Test
    fun repeatPressIgnoredDuringCountdown() {
        val sm = machine()
        sm.onSosPressed(); sm.onSosPressed()
        sm.onSosPressed() // 第三次在 Countdown，应被忽略
        assertEquals(SosState.Countdown(5), sm.state)
    }

    @Test
    fun cooldownBlocksReTriggerRightAfterTrigger() {
        val sm = machine()
        sm.onSosPressed(); sm.onSosPressed()
        repeat(5) { sm.onTick() }
        assertEquals(SosState.Triggered, sm.state)
        assertTrue(sm.inCooldown())
        sm.onSosPressed() // 冷却内应忽略
        assertEquals(SosState.Triggered, sm.state)
    }

    @Test
    fun canRestartAfterCooldown() {
        val sm = machine()
        sm.onSosPressed(); sm.onSosPressed()
        repeat(5) { sm.onTick() }
        now += 31_000L // 越过冷却
        assertFalse(sm.inCooldown())
        sm.onSosPressed()
        assertEquals(SosState.Confirm1, sm.state)
    }
}
