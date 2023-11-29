package com.blockstream.common.utils

import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalUnsignedTypes::class)
class BinaryUtilsTests {
    @Test
    fun `test toHex`() {
        val hex = "415645"
        assertEquals(hex, byteArrayOf(0x41, 0x56, 0x45).toHex())
        assertEquals(hex, ubyteArrayOf(65u, 86u, 69u).toHex())
    }

    @Test
    fun `test cbor`() {
        val hex = "a2011a55b248100281d90194d9012fa4035821026cd461b193e13e5540ab8eb4e5ddb0701b8f99d090f7993f5a6124bcdeab3e2c04582010375168f285ea83ae0ac4327814d1d729e5b472dfc05f2b34a3b20f0d46862106d90130a301861854f500f500f5021a55b248100303081ac35cd782"
        val bytes = hex.hexToByteArray()
    }
}