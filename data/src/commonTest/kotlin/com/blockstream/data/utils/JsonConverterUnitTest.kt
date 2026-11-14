package com.blockstream.data.utils

import com.blockstream.data.gdk.JsonConverter
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonConverterUnitTest {

    lateinit var jsonConverter: JsonConverter

    @BeforeTest
    fun init() {
        jsonConverter = JsonConverter(
            printGdkMessages = true,
            maskSensitiveFields = true,
            appendGdkLogs = { }
        )
    }

    @Test
    fun testMask() {
        val json = "{\"pin\":\"privacy\",\"mnemonic\":\"privacy\",\"password\":\"privacy\",\"recovery_mnemonic\":\"privacy\"}"


        assertTrue(hasSensitiveData(json))
        assertFalse(hasSensitiveData(jsonConverter.mask(json)!!))
    }

    @Test
    fun bip39_passphrase_is_masked() {
        val json = """{"credentials":{"mnemonic":"privacy","bip39_passphrase":"privacy"}}"""

        assertFalse(hasSensitiveData(jsonConverter.mask(json)!!))
    }

    @Test
    fun sweep_private_key_is_masked() {
        val json = """{"private_key":"privacy","fee_rate":1000}"""

        assertFalse(hasSensitiveData(jsonConverter.mask(json)!!))
    }

    @Test
    fun device_key_and_master_blinding_key_are_masked() {
        val json = """{"device_key":"privacy","master_blinding_key":"privacy"}"""

        assertFalse(hasSensitiveData(jsonConverter.mask(json)!!))
    }

    @Test
    fun pin_data_object_and_encrypted_data_are_masked() {
        val json = """{"pin_data":{"encrypted_data":"privacy","pin_identifier":"privacy","salt":"privacy"},"encrypted_data":"privacy"}"""

        assertFalse(hasSensitiveData(jsonConverter.mask(json)!!))
    }

    @Test
    fun liquid_core_descriptors_and_slip132_pubkeys_are_masked() {
        val json =
            """{"result":{"subaccounts":[{"pointer":0,"core_descriptors":["ct(slip77(privacy),elwpkh([e51e604d/84'/1776'/0']privacy/0/*))#xdlf33c5"],"slip132_extended_pubkey":"privacy"}]}}"""

        jsonConverter.mask(json)!!.also {
            assertFalse(hasSensitiveData(it))
            assertTrue(it.contains("\"pointer\":0"))
        }
    }

    @Test
    fun watch_only_descriptor_credentials_are_masked() {
        val json = """{"core_descriptors":["privacy"],"slip132_extended_pubkeys":["privacy"]}"""

        assertFalse(hasSensitiveData(jsonConverter.mask(json)!!))
    }

    @Test
    fun xpubs_blinders_addresses_scripts_and_txids_are_masked() {
        val json =
            """{"xpub":"privacy","xpubs":["privacy"],"amountblinder":"privacy","amountblinders":["privacy"],"assetblinder":"privacy","assetblinders":["privacy"],"blinding_key":"privacy","blinding_nonce":"privacy","nonces":["privacy"],"address":"privacy","addresses":["privacy"],"addressee":{"address":"privacy","satoshi":1000},"addressees":[{"address":"privacy"}],"script":"privacy","scripts":["privacy"],"scriptpubkey":"privacy","txhash":"privacy","txid":"privacy","fee_rate":1000}"""

        jsonConverter.mask(json)!!.also {
            assertFalse(hasSensitiveData(it))
            assertTrue(it.contains("\"fee_rate\":1000"))
        }
    }

    @Test
    fun gauth_secret_in_twofactor_config_is_masked() {
        val json =
            """{"all_methods":["email","gauth"],"email":{"confirmed":true,"enabled":true},"gauth":{"confirmed":false,"data":"otpauth://totp/Green%20Bitcoin?secret=privacy","enabled":false}}"""

        jsonConverter.mask(json)!!.also {
            assertFalse(hasSensitiveData(it))
            assertTrue(it.contains("\"email\":{\"confirmed\":true,\"enabled\":true}"))
        }
    }

    @Test
    fun descriptors_in_non_json_payloads_are_masked() {
        val notJson = """Error while handling {"core_descriptors":["privacy"]}"""

        assertFalse(hasSensitiveData(jsonConverter.mask(notJson)!!))
    }

    @Test
    fun non_json_descriptor_with_key_origin_brackets_is_not_partially_masked() {
        val notJson =
            """Error while handling {"core_descriptors":["ct(slip77(privacy),elwpkh([e51e604d/84'/1776'/0']privacy/0/*))#xdlf33c5"]}"""

        jsonConverter.mask(notJson)!!.also {
            assertFalse(hasSensitiveData(it))
            assertFalse(it.contains("xdlf33c5"))
        }
    }

    @Test
    fun non_json_payloads_without_secrets_are_preserved() {
        val notJson = """Error while handling {"fee_rate":1000}"""

        assertEquals(notJson, jsonConverter.mask(notJson))
    }

    @Test
    fun secrets_nested_in_arrays_are_masked() {
        val json = """{"wallets":[{"name":"a","mnemonic":"privacy"},{"name":"b","seed":"privacy"}]}"""

        assertFalse(hasSensitiveData(jsonConverter.mask(json)!!))
    }

    @Test
    fun secret_value_containing_escaped_quote_is_fully_masked() {
        val json = """{"password":"pri\"vacy"}"""

        jsonConverter.mask(json)!!.also {
            assertFalse(hasSensitiveData(it))
            assertFalse(it.contains("vacy"))
        }
    }

    @Test
    fun secret_field_with_whitespace_before_value_is_masked() {
        val json = """{"mnemonic" : "privacy"}"""

        assertFalse(hasSensitiveData(jsonConverter.mask(json)!!))
    }

    @Test
    fun non_secret_fields_are_preserved() {
        val json = """{"mnemonic":"privacy","fee_rate":1000,"subaccount":"Main"}"""

        jsonConverter.mask(json)!!.also {
            assertFalse(hasSensitiveData(it))
            assertTrue(it.contains("1000"))
            assertTrue(it.contains("Main"))
        }
    }

    @Test
    fun disabled_masking_leaves_payload_untouched() {
        val json = """{"mnemonic":"privacy"}"""
        val converter = JsonConverter(printGdkMessages = false, maskSensitiveFields = false, appendGdkLogs = { })

        assertEquals(json, converter.mask(json))
    }

    @Test
    fun secrets_in_non_json_payloads_are_masked() {
        val notJson = """Error while handling {"mnemonic":"privacy"}"""

        assertFalse(hasSensitiveData(jsonConverter.mask(notJson)!!))
    }

    private fun hasSensitiveData(json: String) = json.also { println(it) }.contains("privacy")
}
