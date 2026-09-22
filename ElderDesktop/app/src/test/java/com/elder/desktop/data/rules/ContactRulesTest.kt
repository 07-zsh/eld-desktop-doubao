package com.elder.desktop.data.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactRulesTest {

    @Test
    fun nextOrderOnEmptyTableStartsAtZero() {
        assertEquals(0, ContactRules.nextOrder(emptyList()))
    }

    @Test
    fun nextOrderAppendsAfterMax() {
        assertEquals(4, ContactRules.nextOrder(listOf(0, 1, 2, 3)))
    }

    @Test
    fun nextOrderHandlesNonContiguousOrders() {
        assertEquals(6, ContactRules.nextOrder(listOf(5)))
        assertEquals(10, ContactRules.nextOrder(listOf(3, 9, 0)))
    }

    @Test
    fun shouldClearOtherEmergencyOnlyWhenEmergency() {
        assertTrue(ContactRules.shouldClearOtherEmergency(true))
        assertFalse(ContactRules.shouldClearOtherEmergency(false))
    }
}
