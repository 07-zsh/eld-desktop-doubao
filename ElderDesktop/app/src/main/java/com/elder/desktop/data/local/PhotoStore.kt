package com.elder.desktop.data.local

import android.content.Context
import com.elder.desktop.data.model.PhotoItem
import java.io.File

/**
 * 读取应用私有目录 files/preset/photos/ 下的图片，不碰系统媒体库（技术方案 §5.1）。
 */
class PhotoStore(private val context: Context) {

    fun photosDir(): File =
        File(context.filesDir, "preset/photos").apply { mkdirs() }

    fun listPhotos(): List<PhotoItem> {
        val dir = photosDir()
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f ->
            f.isFile && PHOTO_EXT.any { f.name.endsWith(it, ignoreCase = true) }
        }?.sortedBy { it.name }
            ?.mapIndexed { index, file ->
                PhotoItem(
                    id = index.toLong(),
                    fileName = file.name,
                    title = file.name.substringBeforeLast('.'),
                    order = index,
                )
            } ?: emptyList()
    }

    fun fileFor(photo: PhotoItem): File = File(photosDir(), photo.fileName)

    companion object {
        private val PHOTO_EXT = listOf(".jpg", ".jpeg", ".png", ".webp")
    }
}
