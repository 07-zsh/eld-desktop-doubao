package com.elder.desktop.data.local

import android.database.sqlite.SQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * AppDatabase 迁移测试：验证 v2 -> v3（contacts 新增 wechatRemark 列）正确且保留旧数据。
 * 修复真实场景：功能2 改了 Contact schema 但忘了升版本，导致真机旧库 Room 校验失败崩溃。
 */
@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {

    @Test
    fun v2ToV3AddsWechatRemarkColumnAndKeepsData() {
        // 构造 v2 版本的 contacts 表（无 wechatRemark 列）
        val db = SQLiteDatabase.create(null)
        db.execSQL(
            "CREATE TABLE `contacts` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `phone` TEXT NOT NULL, " +
                "`avatarFileName` TEXT, `isEmergency` INTEGER NOT NULL, `order` INTEGER NOT NULL)"
        )
        // 用 execSQL 直接插入（ContentValues.insert 在部分 Robolectric 版本返回 -1）
        db.execSQL(
            "INSERT INTO `contacts` (`name`, `phone`, `isEmergency`, `order`) " +
                "VALUES ('老伴', '13900000001', 0, 0)"
        )

        // 应用 v2 -> v3 迁移 SQL（与 MIGRATION_2_3 共享同一常量）
        db.execSQL(AppDatabase.SQL_ADD_WECHAT_REMARK)

        // wechatRemark 列已存在且可写
        db.execSQL("UPDATE `contacts` SET `wechatRemark` = '老伴' WHERE id = 1")

        val cols = mutableListOf<String>()
        db.rawQuery("PRAGMA table_info(`contacts`)", null).use { c ->
            val nameIdx = c.getColumnIndex("name")
            while (c.moveToNext()) cols.add(c.getString(nameIdx))
        }
        assertTrue("wechatRemark 列应已加入", cols.contains("wechatRemark"))

        // 旧数据保留且新列写入成功
        db.rawQuery("SELECT `name`, `wechatRemark` FROM `contacts`", null).use { c ->
            assertTrue("原联系人应保留", c.moveToFirst())
            assertEquals("老伴", c.getString(0))
            assertEquals("老伴", c.getString(1))
        }
    }
}
