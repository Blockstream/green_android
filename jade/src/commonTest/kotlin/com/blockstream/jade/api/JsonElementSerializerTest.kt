// Verifies that JsonElementSerializer encodes every JSON value kind to CBOR, since asset
// registry contracts may carry arbitrary fields beyond the ones Jade displays.
package com.blockstream.jade.api

import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalStdlibApi::class)
class JsonElementSerializerTest {

    @Test
    fun encodes_arrays_booleans_nulls_doubles_and_negative_ints_to_cbor() {
        val element = Json.parseToJsonElement("""{"a":[true,false,null,1.5,-2],"b":{"c":[]}}""")

        val cbor = Cbor { useDefiniteLengthEncoding = true }.encodeToByteArray(JsonElementSerializer, element)

        assertEquals("a2616185f5f4f6fb3ff8000000000000216162a1616380", cbor.toHexString())
    }
}
