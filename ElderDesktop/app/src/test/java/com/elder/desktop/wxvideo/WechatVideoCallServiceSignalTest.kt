package com.elder.desktop.wxvideo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 功能2 事件驱动：窗口事件信号匹配逻辑单测。
 * 覆盖「进聊天(ChattingUI)」与「通话菜单(dialog.a4 含视频通话)」两个信号的命中/误判。
 */
class WechatVideoCallServiceSignalTest {

    private val chattingUI = "com.tencent.mm.ui.chatting.ChattingUI"
    private val launcherUI = "com.tencent.mm.ui.LauncherUI"
    private val callMenu = "com.tencent.mm.ui.widget.dialog.a4"

    @Test
    fun `chat signal matches ChattingUI`() {
        assertTrue(
            WechatVideoCallService.matchSignal(
                WechatVideoCallService.SIGNAL_CHAT, chattingUI, null,
            ),
        )
    }

    @Test
    fun `chat signal rejects other windows`() {
        assertFalse(
            WechatVideoCallService.matchSignal(
                WechatVideoCallService.SIGNAL_CHAT, launcherUI, null,
            ),
        )
        assertFalse(
            WechatVideoCallService.matchSignal(
                WechatVideoCallService.SIGNAL_CHAT, callMenu, listOf("视频通话"),
            ),
        )
    }

    @Test
    fun `call menu signal matches dialog with video text`() {
        assertTrue(
            WechatVideoCallService.matchSignal(
                WechatVideoCallService.SIGNAL_CALL_MENU, callMenu,
                listOf("视频通话", "语音通话", "取消"),
            ),
        )
    }

    @Test
    fun `call menu signal rejects non video dialog`() {
        // 同款 dialog 但文本不含「视频通话」（例如其它菜单），应视为未命中。
        assertFalse(
            WechatVideoCallService.matchSignal(
                WechatVideoCallService.SIGNAL_CALL_MENU, callMenu,
                listOf("发送", "收藏"),
            ),
        )
        assertFalse(
            WechatVideoCallService.matchSignal(
                WechatVideoCallService.SIGNAL_CALL_MENU, callMenu, null,
            ),
        )
    }

    @Test
    fun `call menu signal rejects wrong class`() {
        assertFalse(
            WechatVideoCallService.matchSignal(
                WechatVideoCallService.SIGNAL_CALL_MENU, launcherUI,
                listOf("视频通话"),
            ),
        )
    }

    @Test
    fun `wechat main signal matches LauncherUI`() {
        // 拉起微信后等聊天列表主界面出现，再开始第 1 步。
        assertTrue(
            WechatVideoCallService.matchSignal(
                WechatVideoCallService.SIGNAL_WECHAT_MAIN, launcherUI, null,
            ),
        )
    }

    @Test
    fun `wechat main signal rejects other windows`() {
        assertFalse(
            WechatVideoCallService.matchSignal(
                WechatVideoCallService.SIGNAL_WECHAT_MAIN, chattingUI, null,
            ),
        )
        assertFalse(
            WechatVideoCallService.matchSignal(
                WechatVideoCallService.SIGNAL_WECHAT_MAIN, callMenu, listOf("视频通话"),
            ),
        )
    }

    @Test
    fun `no waiting signal never matches`() {
        assertFalse(
            WechatVideoCallService.matchSignal(0, chattingUI, null),
        )
        assertFalse(
            WechatVideoCallService.matchSignal(0, callMenu, listOf("视频通话")),
        )
    }
}
