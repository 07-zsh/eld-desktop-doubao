package com.elder.desktop.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 紧急号码 + 预设短信内容。SOS 触发后拨打第一个号码，并向全部条目发送短信。
 */
@Entity(tableName = "emergency")
data class EmergencyInfo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val message: String,
    val order: Int = 0,
)
