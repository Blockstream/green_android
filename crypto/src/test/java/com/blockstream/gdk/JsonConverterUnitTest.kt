package com.blockstream.gdk

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class JsonConverterUnitTest {

    lateinit var jsonConverter: JsonConverter

    @Before
    fun init() {
        jsonConverter = JsonConverter(log = true, maskSensitiveFields = true)
    }

    @Test
    fun testMask() {
        val json = "{\"pin\":\"privacy\",\"mnemonic\":\"privacy\",\"password\":\"privacy\",\"recovery_mnemonic\":\"privacy\"}"

        Assert.assertTrue(hasSensitiveData(json))
        Assert.assertFalse(hasSensitiveData(jsonConverter.mask(json)!!))
    }

    private fun hasSensitiveData(json: String) = json.also { println(it) }.contains("privacy")
}