package com.elder.desktop.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.elder.desktop.data.model.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY `order` ASC")
    fun observeAll(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE isEmergency = 1 ORDER BY `order` ASC LIMIT 1")
    suspend fun getEmergency(): Contact?

    @Insert
    suspend fun insertAll(contacts: List<Contact>)

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun count(): Int

    @Query("DELETE FROM contacts")
    suspend fun clear()
}
