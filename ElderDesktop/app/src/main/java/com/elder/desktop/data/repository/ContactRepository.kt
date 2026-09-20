package com.elder.desktop.data.repository

import com.elder.desktop.data.local.ContactDao
import com.elder.desktop.data.model.Contact
import kotlinx.coroutines.flow.Flow

class ContactRepository(private val dao: ContactDao) {
    fun observeContacts(): Flow<List<Contact>> = dao.observeAll()

    suspend fun getEmergency(): Contact? = dao.getEmergency()
}
