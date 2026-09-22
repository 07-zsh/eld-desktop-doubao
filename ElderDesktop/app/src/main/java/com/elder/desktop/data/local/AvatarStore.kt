package com.elder.desktop.data.local

import android.content.Context
import android.net.Uri
import java.io.File
import kotlin.random.Random

/**
 * 子女自定义联系人头像：把所选图片拷贝进应用私有目录 preset/photos/，返回文件名存入 Contact.avatarFileName。
 * 与相册共用目录（与预置照片一致）；删除联系人时不删除文件，避免误删相册照片（功能1 决策）。
 * 使用 GetContent 的系统文件选择器授权 URI，免存储权限（含 Android 7.0+）。
 */
object AvatarStore {

    fun avatarDir(context: Context): File =
        File(context.filesDir, "preset/photos").apply { mkdirs() }

    fun fileFor(context: Context, fileName: String?): File? {
        if (fileName.isNullOrEmpty()) return null
        return File(avatarDir(context), fileName)
    }

    /** 拷贝所选图片到私有目录，返回生成的文件名（含扩展名，按 MIME 推断）。 */
    fun copyFromUri(context: Context, uri: Uri): String {
        val mime = context.contentResolver.getType(uri)
        val ext = when {
            mime?.contains("png") == true -> ".png"
            mime?.contains("webp") == true -> ".webp"
            else -> ".jpg"
        }
        val fileName = "avatar_${System.currentTimeMillis()}_${Random.nextInt(1000)}$ext"
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("无法读取所选图片")
        input.use { ins ->
            File(avatarDir(context), fileName).outputStream().use { outs ->
                ins.copyTo(outs)
            }
        }
        return fileName
    }
}
