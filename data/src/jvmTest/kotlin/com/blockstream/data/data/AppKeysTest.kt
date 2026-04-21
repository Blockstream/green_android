package com.blockstream.data.data

import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppKeysTest {

    @Test
    fun fromText_parses_plain_json() {
        val appKeys = AppKeys.fromText(
            """
            {
              "breez_api_key":"breez",
              "greenlight_key":"greenlight-key",
              "greenlight_cert":"greenlight-cert",
              "zendesk_client_id":"zendesk",
              "reown_project_id":"reown"
            }
            """.trimIndent()
        )

        assertEquals("breez", appKeys?.breezApiKey)
        assertEquals("greenlight-key", appKeys?.greenlightKey)
        assertEquals("greenlight-cert", appKeys?.greenlightCert)
        assertEquals("zendesk", appKeys?.zendeskClientId)
        assertEquals("reown", appKeys?.reownProjectId)
    }

    @Test
    fun fromText_parses_base64_json() {
        val encoded = Base64.encode(
            """
            {
              "greenlight_cert":"greenlight-cert",
              "greenlight_key":"greenlight-key",
              "breez_api_key":"breez",
              "reown_project_id":"reown"
            }
            """.trimIndent().encodeToByteArray()
        )

        val appKeys = AppKeys.fromText(encoded)

        assertEquals("greenlight-cert", appKeys?.greenlightCert)
        assertEquals("greenlight-key", appKeys?.greenlightKey)
        assertEquals("breez", appKeys?.breezApiKey)
        assertEquals("reown", appKeys?.reownProjectId)
    }

    @Test
    fun fromText_returns_null_for_invalid_text() {
        assertNull(AppKeys.fromText("not-json-and-not-base64"))
    }

    @Test
    fun appConfig_default_keeps_reown_project_id() {
        val appConfig = AppConfig.default(
            isDebug = true,
            filesDir = "/tmp/files",
            cacheDir = "/tmp/cache",
            analyticsFeatureEnabled = true,
            lightningFeatureEnabled = true,
            storeRateEnabled = true,
            appKeysString = """
                {
                  "greenlight_cert":"greenlight-cert",
                  "greenlight_key":"greenlight-key",
                  "breez_api_key":"breez",
                  "reown_project_id":"reown"
                }
            """.trimIndent(),
        )

        assertEquals("reown", appConfig.reownProjectId)
    }
}
