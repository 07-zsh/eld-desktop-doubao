package com.elder.desktop.data.model

/**
 * 相册图片条目。图片文件位于 files/preset/photos/，本类仅描述索引。
 */
data class PhotoItem(
    val id: Long,
    val fileName: String,
    val title: String,
    val order: Int,
)
