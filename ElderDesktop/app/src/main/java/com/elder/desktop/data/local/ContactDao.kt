package com.elder.desktop.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
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

    @Insert
    suspend fun insert(contact: Contact): Long

    @Update
    suspend fun update(contact: Contact)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getById(id: Long): Contact?

    @Query("SELECT `order` FROM contacts ORDER BY `order` ASC")
    suspend fun allOrders(): List<Int>

    /** 清空全部紧急标志：设某人为紧急时先调用，保证"最多一人紧急"（SOS 取第一条）。 */
    @Query("UPDATE contacts SET isEmergency = 0")
    suspend fun clearAllEmergency()

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun count(): Int

    @Query("DELETE FROM contacts")
    suspend fun clear()
}
