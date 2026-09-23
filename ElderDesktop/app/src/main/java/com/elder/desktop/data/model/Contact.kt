package com.elder.desktop.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 家人联系人。由子女在设置模式预置，长辈只点不编辑。
 * avatarFileName 对应 files/preset/photos/ 下的头像文件名，可空。
 * wechatRemark 为微信备注名：功能2「一键微信视频」的展示标签（保留）。
 * wxid 为微信「原始 ID / 微信号」：功能2 六宫格按此过滤显示，并为路线二深链
 * （weixin://dl/chat?username=wxid）提供定位身份；留空表示该家人不进入视频入口。
 */
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val avatarFileName: String? = null,
    val isEmergency: Boolean = false,
    val order: Int = 0,
    val wechatRemark: String? = null,
    val wxid: String? = null,
)
