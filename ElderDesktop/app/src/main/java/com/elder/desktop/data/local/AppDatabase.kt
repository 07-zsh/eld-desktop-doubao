package com.elder.desktop.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.elder.desktop.data.model.Contact
import com.elder.desktop.data.model.EmergencyInfo

@Database(entities = [Contact::class, EmergencyInfo::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun emergencyDao(): EmergencyDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "elder.db"
                ).build().also { instance = it }
            }
    }
}
