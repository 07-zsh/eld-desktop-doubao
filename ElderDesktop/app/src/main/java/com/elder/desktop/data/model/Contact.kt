package com.elder.desktop.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 家人联系人。由子女在设置模式预置，长辈只点不编辑。
 * avatarFileName 对应 files/preset/photos/ 下的头像文件名，可空。
 */
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val avatarFileName: String? = null,
    val isEmergency: Boolean = false,
    val order: Int = 0,
)
