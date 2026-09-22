package com.elder.desktop.data.rules

/**
 * 联系人业务规则（纯 Kotlin，不依赖 Android / Room，便于独立 JVM 单测）。
 * 与 [com.elder.desktop.data.repository.ContactRepository] 配合使用，保持规则集中可测。
 */
object ContactRules {

    /**
     * 新增联系人排末尾：取现有最大 order + 1；空表从 0 开始。
     *
     * @param existingOrders 现有全部联系人的 order 列表（可为空）
     * @return 新联系人的 order
     */
    fun nextOrder(existingOrders: List<Int>): Int =
        (existingOrders.maxOrNull() ?: -1) + 1

    /**
     * SOS 紧急号码"最多一人"规则：仅当新值标记为紧急时，需要先清空其他联系人的紧急标志，
     * 避免出现多个紧急号码导致 [com.elder.desktop.data.repository.ContactRepository.getEmergency]
     * 取到非预期对象。
     *
     * @return 是否应清除其他联系人的紧急标志
     */
    fun shouldClearOtherEmergency(isEmergency: Boolean): Boolean = isEmergency

    /**
     * 微信备注名合法性/唯一性规则（功能2 一键微信视频）。
     *
     * 微信备注用于搜索路径剪贴板定位联系人：
     *  1. 允许留空——留空表示该家人未启用微信视频（家人页不显示视频按钮）；
     *  2. 若填写则要求全表唯一（重复备注会让搜索结果/点击对象不确定，点错人风险）。
     *
     * @param remark 本次提交的微信备注（可能为 null/空白）
     * @param existingRemarks 其他联系人（排除自身 id）已有的非空微信备注
     * @param editingId 正在编辑的联系人 id；新增时为 null（null 表示不是编辑既有联系人）
     * @return 校验通过返回 null，否则返回错误提示
     */
    @Suppress("UNUSED_PARAMETER")
    fun validateWechatRemark(
        remark: String?,
        existingRemarks: List<String>,
        editingId: Long?,
    ): String? {
        val trimmed = remark?.trim().orEmpty()
        if (trimmed.isEmpty()) return null // 留空 = 不启用微信视频，合法
        if (existingRemarks.any { it == trimmed }) return "微信备注「$trimmed」已存在，请改为唯一备注"
        return null
    }
}
