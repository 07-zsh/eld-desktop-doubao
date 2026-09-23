package com.elder.desktop.data.local

import com.elder.desktop.data.model.Contact
import com.elder.desktop.data.model.EmergencyInfo
import com.elder.desktop.data.model.Weather
import org.json.JSONArray
import org.json.JSONObject

/**
 * 预置数据解析。全部为纯函数，不触碰 Android Framework，便于 JVM/Robolectric 单测。
 * 解析失败时抛 [IllegalArgumentException]，由调用方决定静默降级。
 */
object PresetParser {

    fun parseContacts(json: String): List<Contact> = wrap {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Contact(
                name = o.getString("name"),
                phone = o.getString("phone"),
                avatarFileName = o.optString("avatar").takeIf { it.isNotEmpty() },
                isEmergency = o.optBoolean("isEmergency", false),
                order = o.optInt("order", i),
                wechatRemark = o.optString("wechatRemark").takeIf { it.isNotEmpty() },
                wxid = o.optString("wxid").takeIf { it.isNotEmpty() },
            )
        }
    }

    fun parseEmergency(json: String): List<EmergencyInfo> = wrap {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            EmergencyInfo(
                name = o.getString("name"),
                phone = o.getString("phone"),
                message = o.optString("message", "我需要帮助，请尽快联系我"),
                order = o.optInt("order", i),
            )
        }
    }

    fun parseWeather(json: String): Weather = wrap {
        val o = JSONObject(json)
        Weather(
            city = o.optString("city", ""),
            condition = o.optString("condition", ""),
            temp = o.optString("temp", ""),
        )
    }

    private fun <T> wrap(block: () -> T): T = try {
        block()
    } catch (e: org.json.JSONException) {
        throw IllegalArgumentException("preset json parse failed", e)
    }
}
