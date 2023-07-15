package com.blockstream.common.utils

import com.blockstream.common.gdk.JsonConverter
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonConverterUnitTest {

    lateinit var jsonConverter: JsonConverter

    @BeforeTest
    fun init() {
        jsonConverter = JsonConverter(log = true, maskSensitiveFields = true)
    }

    @Test
    fun testMask() {
        val json = "{\"pin\":\"privacy\",\"mnemonic\":\"privacy\",\"password\":\"privacy\",\"recovery_mnemonic\":\"privacy\"}"


        assertTrue(hasSensitiveData(json))
        assertFalse(hasSensitiveData(jsonConverter.mask(json)!!))
    }

    private fun hasSensitiveData(json: String) = json.also { println(it) }.contains("privacy")
}