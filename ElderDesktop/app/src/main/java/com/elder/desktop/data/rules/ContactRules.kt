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
}
