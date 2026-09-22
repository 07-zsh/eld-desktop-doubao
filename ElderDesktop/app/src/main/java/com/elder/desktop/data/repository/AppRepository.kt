package com.elder.desktop.data.repository

import android.content.pm.PackageManager
import com.elder.desktop.data.local.AppDao
import com.elder.desktop.data.model.AppEntry
import com.elder.desktop.data.rules.ContactRules
import kotlinx.coroutines.flow.Flow

/**
 * 桌面第三方应用仓库（功能3）。
 * 读：桌面六宫格响应式展示；写：子女设置「添加应用」页。
 * 桌面第三方应用固定上限 [MAX_APPS]（六宫格第三行 2 格）。
 */
class AppRepository(
    private val dao: AppDao,
    private val packageManager: PackageManager,
) {

    fun observeApps(): Flow<List<AppEntry>> = dao.observeAll()

    suspend fun getApps(): List<AppEntry> = dao.getAll()

    /**
     * 添加应用（排末尾）。达到上限 [MAX_APPS] 时返回 null，不写入；成功返回新记录 id。
     */
    suspend fun add(packageName: String, label: String): Long? {
        if (dao.count() >= MAX_APPS) return null
        val order = ContactRules.nextOrder(dao.allOrders())
        return dao.insert(AppEntry(packageName = packageName, label = label, order = order))
    }

    suspend fun remove(id: Long) {
        dao.deleteById(id)
    }

    /** 应用是否仍已安装：桌面渲染时据此决定是否显示（卸载后不显示图标，方案乙）。 */
    fun isInstalled(packageName: String): Boolean =
        runCatching { packageManager.getApplicationInfo(packageName, 0) }.isSuccess

    companion object {
        const val MAX_APPS = 2
    }
}
