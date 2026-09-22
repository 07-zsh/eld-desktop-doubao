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
        val c = repo.addContact("C", "3", null, true)

        val list = repo.observeContacts().first()
        assertEquals(1, list.count { it.isEmergency })
        assertEquals("C", list.first { it.id == c }.name)
        assertEquals("C", repo.getEmergency()?.name)
    }

    @Test
    fun updateKeepsOrderAndId() = runBlocking {
        val a = repo.addContact("A", "13800000001", null, false)
        repo.addContact("B", "13800000002", null, false)

        assertTrue(repo.updateContact(a, "A2", "13800000099", null, false))

        val list = repo.observeContacts().first()
        assertEquals("A2", list.first { it.id == a }.name)
        assertEquals("13800000099", list.first { it.id == a }.phone)
        assertEquals(0, list.first { it.id == a }.order)
    }

    @Test
    fun updateToEmergencyClearsOthers() = runBlocking {
        val a = repo.addContact("A", "1", null, true)
        val b = repo.addContact("B", "2", null, false)

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
        val a = repo.addContact("A", "1", null, false)
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
}
