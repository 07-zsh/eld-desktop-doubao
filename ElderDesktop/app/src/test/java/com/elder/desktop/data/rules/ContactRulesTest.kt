package com.elder.desktop.data.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    // ---- 功能2：微信备注校验 ----

    @Test
    fun blankRemarkIsAllowedWhenVideoNotEnabled() {
        assertNull(ContactRules.validateWechatRemark(null, emptyList(), null))
        assertNull(ContactRules.validateWechatRemark("  ", emptyList(), null))
        assertNull(ContactRules.validateWechatRemark("", emptyList(), null))
    }

    @Test
    fun uniqueRemarkPasses() {
        assertNull(ContactRules.validateWechatRemark("儿子", listOf("女儿", "老伴"), null))
    }

    @Test
    fun duplicateRemarkFails() {
        val err = ContactRules.validateWechatRemark("儿子", listOf("女儿", "儿子"), null)
        assertTrue(err?.contains("儿子") == true)
    }

    @Test
    fun remarkTrimmedBeforeCompare() {
        // 前后带空格的同名备注应判重
        val err = ContactRules.validateWechatRemark("  儿子  ", listOf("儿子"), null)
        assertTrue(err != null)
    }

    // ---- 功能2（改版）：微信 ID（wxid）校验 ----

    @Test
    fun blankWxidIsAllowedWhenVideoNotEnabled() {
        assertNull(ContactRules.validateWxid(null, emptyList(), null))
        assertNull(ContactRules.validateWxid("  ", emptyList(), null))
        assertNull(ContactRules.validateWxid("", emptyList(), null))
    }

    @Test
    fun uniqueWxidPasses() {
        assertNull(ContactRules.validateWxid("wxid_aa", listOf("wxid_bb", "wxid_cc"), null))
    }

    @Test
    fun duplicateWxidFails() {
        val err = ContactRules.validateWxid("wxid_aa", listOf("wxid_bb", "wxid_aa"), null)
        assertTrue(err?.contains("wxid_aa") == true)
    }

    @Test
    fun wxidTrimmedBeforeCompare() {
        val err = ContactRules.validateWxid("  wxid_aa  ", listOf("wxid_aa"), null)
        assertTrue(err != null)
    }
}
