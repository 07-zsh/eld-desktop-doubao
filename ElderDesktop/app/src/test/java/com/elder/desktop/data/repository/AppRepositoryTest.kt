package com.elder.desktop.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.elder.desktop.data.local.AppDao
import com.elder.desktop.data.local.AppDatabase
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

/** 桌面第三方应用（功能3）的 Room 集成测试：排末尾、上限 2、移除、安装状态。 */
@RunWith(RobolectricTestRunner::class)
class AppRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: AppDao
    private lateinit var repo: AppRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.appDao()
        repo = AppRepository(dao, context.packageManager)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addEnforcesLimitOfTwo() = runBlocking {
        assertTrue(repo.add("com.app.A", "A") != null)
        assertTrue(repo.add("com.app.B", "B") != null)
        assertNull(repo.add("com.app.C", "C"))

        val list = repo.getApps()
        assertEquals(2, list.size)
        assertEquals(listOf("com.app.A", "com.app.B"), list.map { it.packageName })
    }

    @Test
    fun addAppendsToEndWithOrder() = runBlocking {
        repo.add("com.app.A", "A")
        repo.add("com.app.B", "B")

        val list = repo.getApps()
        assertEquals(listOf(0, 1), list.map { it.order })
    }

    @Test
    fun addAfterRemoveStillAppendsToEnd() = runBlocking {
        val a = repo.add("com.app.A", "A")!!
        repo.add("com.app.B", "B")
        repo.remove(a)

        assertTrue(repo.add("com.app.C", "C") != null)

        val list = repo.getApps()
        assertEquals(listOf("com.app.B", "com.app.C"), list.map { it.packageName })
        // 排末尾：移除首位后，C 仍排到末尾，order 递增不复用被删位
        assertEquals(listOf(1, 2), list.map { it.order })
    }

    @Test
    fun removeDeletesByAppId() = runBlocking {
        val a = repo.add("com.app.A", "A")!!
        repo.add("com.app.B", "B")

        repo.remove(a)

        val list = repo.getApps()
        assertEquals(listOf("com.app.B"), list.map { it.packageName })
    }

    @Test
    fun isInstalledChecksPackageManager() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(repo.isInstalled(context.packageName))
        assertFalse(repo.isInstalled("com.nonexistent.xyz"))
    }
}
