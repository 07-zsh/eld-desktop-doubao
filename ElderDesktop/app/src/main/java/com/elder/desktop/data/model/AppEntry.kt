package com.elder.desktop.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 桌面上的第三方应用入口（功能3）。由子女在设置模式从白名单勾选，最多 2 个。
 * packageName 用于启动与「是否已安装」校验；label 为应用显示名（从包管理器读取后缓存一份）。
 */
@Entity(tableName = "app_entry")
data class AppEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val label: String,
    val order: Int = 0,
)
