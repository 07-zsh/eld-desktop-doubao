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
     * @return 校验失败返回 null；成功返回新联系人的 id
     */
    suspend fun addContact(
        name: String,
        phone: String,
        avatarFileName: String?,
        isEmergency: Boolean,
        wechatRemark: String? = null,
        wxid: String? = null,
    ): Long? {
        val remark = wechatRemark?.trim().orEmpty().takeIf { it.isNotEmpty() }
        val error = ContactRules.validateWechatRemark(remark, dao.allWechatRemarks(), editingId = null)
        if (error != null) return null
        val wxidValue = wxid?.trim().orEmpty().takeIf { it.isNotEmpty() }
        val wxidError = ContactRules.validateWxid(wxidValue, dao.allWxids(), editingId = null)
        if (wxidError != null) return null
        val order = ContactRules.nextOrder(dao.allOrders())
        if (ContactRules.shouldClearOtherEmergency(isEmergency)) dao.clearAllEmergency()
        return dao.insert(
            Contact(
                name = name.trim(),
                phone = phone.trim(),
                avatarFileName = avatarFileName,
                isEmergency = isEmergency,
                order = order,
                wechatRemark = remark,
                wxid = wxidValue,
            )
        )
    }

    /**
     * 更新联系人：保留原 id/order；设为紧急时清空其他紧急标志。
     * @return 校验失败返回 false；联系人不存在返回 false；成功返回 true
     */
    suspend fun updateContact(
        id: Long,
        name: String,
        phone: String,
        avatarFileName: String?,
        isEmergency: Boolean,
        wechatRemark: String? = null,
        wxid: String? = null,
    ): Boolean {
        val existing = dao.getById(id) ?: return false
        val remark = wechatRemark?.trim().orEmpty().takeIf { it.isNotEmpty() }
        val error = ContactRules.validateWechatRemark(
            remark, dao.otherWechatRemarks(id), editingId = id,
        )
        if (error != null) return false
        val wxidValue = wxid?.trim().orEmpty().takeIf { it.isNotEmpty() }
        val wxidError = ContactRules.validateWxid(wxidValue, dao.otherWxids(id), editingId = id)
        if (wxidError != null) return false
        if (ContactRules.shouldClearOtherEmergency(isEmergency)) dao.clearAllEmergency()
        dao.update(
            existing.copy(
                name = name.trim(),
                phone = phone.trim(),
                avatarFileName = avatarFileName,
                isEmergency = isEmergency,
                wechatRemark = remark,
                wxid = wxidValue,
            )
        )
        return true
    }

    /** 删除联系人（不删除其头像文件：头像与相册共用目录，避免误删相册照片）。 */
    suspend fun deleteContact(id: Long) {
        dao.deleteById(id)
    }
}
