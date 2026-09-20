package com.elder.desktop.data.local

import android.content.Context
import com.elder.desktop.data.model.Contact
import com.elder.desktop.data.model.EmergencyInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 首启预置导入：解析 assets/preset/*.json 写 Room，拷贝 photos 到私有目录。
 * 幂等：已存在数据时跳过，重复初始化不重复写入（技术方案 §9.1）。
 */
class PresetImporter(
    private val context: Context,
    private val contactDao: ContactDao,
    private val emergencyDao: EmergencyDao,
    private val settings: SettingsStore,
) {

    sealed interface Result {
        data class Imported(
            val contacts: Int,
            val emergency: Int,
            val photos: Int,
        ) : Result

        data object AlreadyInitialized : Result
    }

    suspend fun importIfNeeded(): Result = withContext(Dispatchers.IO) {
        if (contactDao.count() > 0) return@withContext Result.AlreadyInitialized

        val contacts = runCatching {
            PresetParser.parseContacts(readAsset("preset/contacts.json"))
        }.getOrDefault(emptyList())

        val emergency = runCatching {
            PresetParser.parseEmergency(readAsset("preset/emergency.json"))
        }.getOrDefault(emptyList())

        val copiedPhotos = copyPhotosFromAssets()

        if (contacts.isNotEmpty()) contactDao.insertAll(contacts)
        if (emergency.isNotEmpty()) emergencyDao.insertAll(emergency)
        settings.markInitialized()

        Result.Imported(contacts.size, emergency.size, copiedPhotos)
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    private fun copyPhotosFromAssets(): Int {
        val targetDir = PhotoStore(context).photosDir()
        return runCatching {
            val names = context.assets.list("preset/photos") ?: emptyArray()
            names.filter { it.contains('.') }.forEach { name ->
                context.assets.open("preset/photos/$name").use { input ->
                    File(targetDir, name).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            names.size
        }.getOrDefault(0)
    }
}
