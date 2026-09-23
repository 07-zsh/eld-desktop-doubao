package com.elder.desktop.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.elder.desktop.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 联系人增删改（功能1）的 Room 集成测试。 */
@RunWith(RobolectricTestRunner::class)
class ContactRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: ContactRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = ContactRepository(db.contactDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addContactAppendsToEndWithOrder() = runBlocking {
        repo.addContact("A", "13800000001", null, false)
        repo.addContact("B", "13800000002", null, false)
        repo.addContact("C", "13800000003", null, false)

        val list = repo.observeContacts().first()
        assertEquals(listOf("A", "B", "C"), list.map { it.name })
        assertEquals(listOf(0, 1, 2), list.map { it.order })
    }

    @Test
    fun addEmergencyClearsPreviousEmergency() = runBlocking {
        repo.addContact("A", "1", null, true)
        repo.addContact("B", "2", null, false)
        val c = repo.addContact("C", "3", null, true)!!

        val list = repo.observeContacts().first()
        assertEquals(1, list.count { it.isEmergency })
        assertEquals("C", list.first { it.id == c }.name)
        assertEquals("C", repo.getEmergency()?.name)
    }

    @Test
    fun updateKeepsOrderAndId() = runBlocking {
        val a = repo.addContact("A", "13800000001", null, false)!!
        repo.addContact("B", "13800000002", null, false)

        assertTrue(repo.updateContact(a, "A2", "13800000099", null, false))

        val list = repo.observeContacts().first()
        assertEquals("A2", list.first { it.id == a }.name)
        assertEquals("13800000099", list.first { it.id == a }.phone)
        assertEquals(0, list.first { it.id == a }.order)
    }

    @Test
    fun updateToEmergencyClearsOthers() = runBlocking {
        val a = repo.addContact("A", "1", null, true)!!
        val b = repo.addContact("B", "2", null, false)!!

        assertTrue(repo.updateContact(b, "B", "2", null, true))

        val list = repo.observeContacts().first()
        assertEquals(1, list.count { it.isEmergency })
        assertEquals("B", repo.getEmergency()?.name)
        assertFalse(list.first { it.id == a }.isEmergency)
    }

    @Test
    fun updateMissingContactReturnsFalse() = runBlocking {
        assertFalse(repo.updateContact(999L, "X", "1", null, false))
        assertNull(repo.getById(999L))
    }

    @Test
    fun deleteRemovesContact() = runBlocking {
        val a = repo.addContact("A", "1", null, false)!!
        repo.addContact("B", "2", null, false)

        repo.deleteContact(a)

        val list = repo.observeContacts().first()
        assertEquals(listOf("B"), list.map { it.name })
        assertNull(repo.getById(a))
    }

    @Test
    fun deleteAllLeavesEmpty() = runBlocking {
        repo.addContact("A", "1", null, false)
        repo.deleteContact(repo.observeContacts().first().single().id)
        assertTrue(repo.observeContacts().first().isEmpty())
    }

    // ---- 功能2：微信备注存储与唯一性 ----

    @Test
    fun addContactStoresWechatRemark() = runBlocking {
        val id = repo.addContact("儿子", "13800000001", null, false, "儿子 志强")!!

        val list = repo.observeContacts().first()
        assertEquals("儿子 志强", list.first { it.id == id }.wechatRemark)
    }

    @Test
    fun blankRemarkStoresNull() = runBlocking {
        val id = repo.addContact("老伴", "13900000001", null, false, "  ")!!

        val list = repo.observeContacts().first()
        assertNull(list.first { it.id == id }.wechatRemark)
    }

    @Test
    fun addContactWithDuplicateRemarkRejected() = runBlocking {
        repo.addContact("A", "1", null, false, "同备注")
        val result = repo.addContact("B", "2", null, false, "同备注")

        assertNull(result)
        assertEquals(1, repo.observeContacts().first().size)
    }

    @Test
    fun updateContactWithOwnRemarkKeptAllowed() = runBlocking {
        val id = repo.addContact("儿子", "13800000001", null, false, "儿子")!!

        assertTrue(repo.updateContact(id, "儿子", "13800000002", null, false, "儿子"))

        val list = repo.observeContacts().first()
        assertEquals("儿子", list.first { it.id == id }.wechatRemark)
    }

    @Test
    fun updateContactWithDuplicateRemarkRejected() = runBlocking {
        repo.addContact("A", "1", null, false, "备注A")
        val idB = repo.addContact("B", "2", null, false, "备注B")!!

        val ok = repo.updateContact(idB, "B", "2", null, false, "备注A")

        assertFalse(ok)
        assertEquals("备注B", repo.getById(idB)?.wechatRemark)
    }

    // ---- 功能2（改版）：微信 ID（wxid）存储与唯一性 ----

    @Test
    fun addContactStoresWxid() = runBlocking {
        val id = repo.addContact("女儿", "13800000001", null, false, "晓慧", "wxid_xiaohui")!!

        val list = repo.observeContacts().first()
        assertEquals("wxid_xiaohui", list.first { it.id == id }.wxid)
    }

    @Test
    fun blankWxidStoresNull() = runBlocking {
        val id = repo.addContact("老伴", "13900000001", null, false, "老伴", "  ")!!

        val list = repo.observeContacts().first()
        assertNull(list.first { it.id == id }.wxid)
    }

    @Test
    fun addContactWithDuplicateWxidRejected() = runBlocking {
        repo.addContact("A", "1", null, false, null, "wxid_same")
        val result = repo.addContact("B", "2", null, false, null, "wxid_same")

        assertNull(result)
        assertEquals(1, repo.observeContacts().first().size)
    }

    @Test
    fun updateContactWithWxidAndOwnKeptAllowed() = runBlocking {
        val id = repo.addContact("女儿", "13800000001", null, false, "晓慧", "wxid_xiaohui")!!

        assertTrue(repo.updateContact(id, "女儿", "13800000002", null, false, "晓慧", "wxid_xiaohui"))

        val list = repo.observeContacts().first()
        assertEquals("wxid_xiaohui", list.first { it.id == id }.wxid)
    }

    @Test
    fun updateContactWithDuplicateWxidRejected() = runBlocking {
        repo.addContact("A", "1", null, false, null, "wxid_A")
        val idB = repo.addContact("B", "2", null, false, null, "wxid_B")!!

        val ok = repo.updateContact(idB, "B", "2", null, false, null, "wxid_A")

        assertFalse(ok)
        assertEquals("wxid_B", repo.getById(idB)?.wxid)
    }
}
