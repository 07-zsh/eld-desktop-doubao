package com.elder.desktop.data.repository

import com.elder.desktop.data.local.ContactDao
import com.elder.desktop.data.model.Contact
import com.elder.desktop.data.rules.ContactRules
import kotlinx.coroutines.flow.Flow

/**
 * 联系人仓库：读（电话/家人页响应式）与写（子女编辑页）统一入口。
 * 增删改仅出现在子女设置模式，长辈阶段只读。
 */
class ContactRepository(private val dao: ContactDao) {

    fun observeContacts(): Flow<List<Contact>> = dao.observeAll()

    suspend fun getEmergency(): Contact? = dao.getEmergency()

    suspend fun getById(id: Long): Contact? = dao.getById(id)

    /**
     * 新增联系人：排末尾（order = max+1）；若设为紧急，先清空其他紧急标志（最多一人紧急）。
     * @return 新联系人的 id
     */
    suspend fun addContact(
        name: String,
        phone: String,
        avatarFileName: String?,
        isEmergency: Boolean,
    ): Long {
        val order = ContactRules.nextOrder(dao.allOrders())
        if (ContactRules.shouldClearOtherEmergency(isEmergency)) dao.clearAllEmergency()
        return dao.insert(
            Contact(
                name = name.trim(),
                phone = phone.trim(),
                avatarFileName = avatarFileName,
                isEmergency = isEmergency,
                order = order,
            )
        )
    }

    /**
     * 更新联系人：保留原 id/order；设为紧急时清空其他紧急标志。
     * @return 更新是否成功（联系人不存在时返回 false）
     */
    suspend fun updateContact(
        id: Long,
        name: String,
        phone: String,
        avatarFileName: String?,
        isEmergency: Boolean,
    ): Boolean {
        val existing = dao.getById(id) ?: return false
        if (ContactRules.shouldClearOtherEmergency(isEmergency)) dao.clearAllEmergency()
        dao.update(
            existing.copy(
                name = name.trim(),
                phone = phone.trim(),
                avatarFileName = avatarFileName,
                isEmergency = isEmergency,
            )
        )
        return true
    }

    /** 删除联系人（不删除其头像文件：头像与相册共用目录，避免误删相册照片）。 */
    suspend fun deleteContact(id: Long) {
        dao.deleteById(id)
    }
}
