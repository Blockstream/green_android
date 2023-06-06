package com.blockstream.common.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class BinaryUtilsTests {
    @Test
    fun `test toHex`() {
        val hex = "415645"
        assertEquals(hex, byteArrayOf(0x41, 0x56, 0x45).toHex())
        assertEquals(hex, ubyteArrayOf(65u, 86u, 69u).toHex())
    }
}