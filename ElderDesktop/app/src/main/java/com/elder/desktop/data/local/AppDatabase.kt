package com.elder.desktop.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.elder.desktop.data.model.AppEntry
import com.elder.desktop.data.model.Contact
import com.elder.desktop.data.model.EmergencyInfo

@Database(entities = [Contact::class, EmergencyInfo::class, AppEntry::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun emergencyDao(): EmergencyDao
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** v1 -> v2：新增 app_entry 表（功能3 桌面第三方应用），保留既有联系人/紧急数据。 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_entry` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`packageName` TEXT NOT NULL, " +
                        "`label` TEXT NOT NULL, " +
                        "`order` INTEGER NOT NULL)"
                )
            }
        }

        /** v2 -> v3 迁移 SQL：contacts 新增可空 wechatRemark 列（功能2 一键微信视频的备注定位）。 */
        internal const val SQL_ADD_WECHAT_REMARK = "ALTER TABLE `contacts` ADD COLUMN `wechatRemark` TEXT"

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(SQL_ADD_WECHAT_REMARK)
            }
        }

        /** v3 -> v4 迁移 SQL：contacts 新增可空 wxid 列（功能2 六宫格过滤 / 路线二深链定位身份）。 */
        internal const val SQL_ADD_WXID = "ALTER TABLE `contacts` ADD COLUMN `wxid` TEXT"

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(SQL_ADD_WXID)
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "elder.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
