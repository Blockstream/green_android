package com.blockstream.data.gdk.data

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [AuthHandlerStatus] parsing, covering the inconsistent `auth_data`
 * payloads GDK emits: a bare boolean for SMS/email 2FA, or an object carrying
 * fields like `telegram_url` and `estimated_progress`.
 */
class AuthHandlerStatusTest {

    @Test
    fun auth_data_as_boolean_parses_as_enabled() {
        val status = AuthHandlerStatus.from(
            """{"action":"enable_2fa","attempts_remaining":3,"auth_data":true,"method":"sms","status":"resolve_code"}"""
        )
        assertEquals(AuthData.Enabled(true), status.authData)
    }

    @Test
    fun auth_data_as_object_parses_telegram_url() {
        val status = AuthHandlerStatus.from(
            """{"action":"enable_telegram","auth_data":{"telegram_url":"https://t.me/BlockstreamGreenTestnetBot?start=IXXXXX"},"method":"telegram","status":"resolve_code"}"""
        )
        assertEquals(
            AuthData.Data(telegramUrl = "https://t.me/BlockstreamGreenTestnetBot?start=IXXXXX"),
            status.authData
        )
    }

    @Test
    fun auth_data_as_object_parses_estimated_progress() {
        val status = AuthHandlerStatus.from(
            """{"action":"scan","auth_data":{"estimated_progress":42},"status":"resolve_code"}"""
        )
        assertEquals(42, (status.authData as? AuthData.Data)?.estimatedProgress)
    }

    @Test
    fun missing_auth_data_is_null() {
        val status = AuthHandlerStatus.from(
            """{"action":"get_xpubs","status":"done"}"""
        )
        assertNull(status.authData)
    }

    @Test
    fun auth_data_serializes_back_to_its_original_shape() {
        assertEquals("true", Json.encodeToString(AuthDataSerializer, AuthData.Enabled(true)))
        assertEquals(
            """{"telegram_url":"x"}""",
            Json.encodeToString(AuthDataSerializer, AuthData.Data(telegramUrl = "x"))
        )
    }
}