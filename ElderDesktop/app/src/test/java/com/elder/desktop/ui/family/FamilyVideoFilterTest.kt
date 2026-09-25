package com.elder.desktop.ui.family

import com.elder.desktop.data.model.Contact
import org.junit.Assert.assertEquals
import org.junit.Test

/** 功能2 家人页六宫格过滤（按微信备注而非 wxid）的纯函数单测。 */
class FamilyVideoFilterTest {

    @Test
    fun `keeps contacts with nonblank wechat remark`() {
        val withRemark = Contact(id = 1, name = "女儿", phone = "1", wechatRemark = "测试甲")
        val result = videoEligibleContacts(listOf(withRemark))
        assertEquals(listOf(withRemark), result)
    }

    @Test
    fun `drops contacts that only have wxid but no remark`() {
        val onlyWxid = Contact(id = 2, name = "儿子", phone = "2", wxid = "wxid_xxx")
        val result = videoEligibleContacts(listOf(onlyWxid))
        assertEquals(emptyList<Contact>(), result)
    }

    @Test
    fun `drops contacts with blank or whitespace remark`() {
        val blank = Contact(id = 3, name = "老伴", phone = "3", wechatRemark = "")
        val whitespace = Contact(id = 4, name = "老头子", phone = "4", wechatRemark = "   ")
        val result = videoEligibleContacts(listOf(blank, whitespace))
        assertEquals(emptyList<Contact>(), result)
    }

    @Test
    fun `mixed list only keeps remark contacts`() {
        val withRemark = Contact(id = 1, name = "女儿", phone = "1", wechatRemark = "测试甲")
        val onlyWxid = Contact(id = 2, name = "儿子", phone = "2", wxid = "wxid_xxx")
        val neither = Contact(id = 3, name = "老伴", phone = "3")
        val result = videoEligibleContacts(listOf(withRemark, onlyWxid, neither))
        assertEquals(listOf(withRemark), result)
    }
}
