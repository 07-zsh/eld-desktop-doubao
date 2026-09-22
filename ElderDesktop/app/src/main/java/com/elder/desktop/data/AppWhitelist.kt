package com.elder.desktop.data

/**
 * 桌面第三方应用白名单（功能3）。与 AndroidManifest 的 `<queries>` 点名保持一致：
 * 只列出点名包名内的应用，子女在设置页从白名单勾选，最多 [AppRepository.MAX_APPS] 个。
 */
object AppWhitelist {

    data class Entry(val label: String, val packageName: String)

    val ALL: List<Entry> = listOf(
        Entry("抖音", "com.ss.android.ugc.aweme"),
        Entry("微信", "com.tencent.mm"),
        Entry("快手", "com.smile.gifmaker"),
    )
}
