package com.elder.desktop.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetParserTest {

    @Test
    fun parseContactsReadsAllFields() {
        val json = """
            [
              {"name":"女儿 晓慧","phone":"13800000001","avatar":"daughter.jpg","isEmergency":true,"order":0},
              {"name":"儿子 志强","phone":"13800000002","avatar":"son.jpg","isEmergency":false,"order":1}
            ]
        """.trimIndent()
        val contacts = PresetParser.parseContacts(json)
        assertEquals(2, contacts.size)
        assertEquals("女儿 晓慧", contacts[0].name)
        assertEquals("13800000001", contacts[0].phone)
        assertEquals("daughter.jpg", contacts[0].avatarFileName)
        assertTrue(contacts[0].isEmergency)
        assertEquals(0, contacts[0].order)
        assertFalse(contacts[1].isEmergency)
    }

    @Test
    fun parseContactsAppliesDefaultsForMissingFields() {
        val json = """[{"name":"老伴","phone":"13900000000"}]"""
        val c = PresetParser.parseContacts(json).single()
        assertEquals("老伴", c.name)
        assertEquals("13900000000", c.phone)
        assertNull(c.avatarFileName)
        assertFalse(c.isEmergency)
        assertEquals(0, c.order)
    }

    @Test
    fun parseEmergencyDefaultsMessage() {
        val json = """[{"name":"女儿","phone":"13800000001"}]"""
        val e = PresetParser.parseEmergency(json).single()
        assertEquals("女儿", e.name)
        assertEquals("我需要帮助，请尽快联系我", e.message)
    }

    @Test
    fun parseWeather() {
        val json = """{"city":"淮南","condition":"晴","temp":"26°"}"""
        val w = PresetParser.parseWeather(json)
        assertEquals("淮南", w.city)
        assertEquals("晴", w.condition)
        assertEquals("26°", w.temp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedContactsJsonThrows() {
        PresetParser.parseContacts("not a json array")
    }
}
