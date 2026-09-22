package com.elder.desktop.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.elder.desktop.data.model.AppEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM app_entry ORDER BY `order` ASC")
    fun observeAll(): Flow<List<AppEntry>>

    @Query("SELECT * FROM app_entry ORDER BY `order` ASC")
    suspend fun getAll(): List<AppEntry>

    @Insert
    suspend fun insert(app: AppEntry): Long

    @Query("DELETE FROM app_entry WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT `order` FROM app_entry ORDER BY `order` ASC")
    suspend fun allOrders(): List<Int>

    @Query("SELECT COUNT(*) FROM app_entry")
    suspend fun count(): Int

    @Query("DELETE FROM app_entry")
    suspend fun clear()
}
