package com.elder.desktop.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.elder.desktop.data.model.EmergencyInfo

@Dao
interface EmergencyDao {
    @Query("SELECT * FROM emergency ORDER BY `order` ASC")
    suspend fun getAll(): List<EmergencyInfo>

    @Insert
    suspend fun insertAll(items: List<EmergencyInfo>)

    @Query("SELECT COUNT(*) FROM emergency")
    suspend fun count(): Int

    @Query("DELETE FROM emergency")
    suspend fun clear()
}
