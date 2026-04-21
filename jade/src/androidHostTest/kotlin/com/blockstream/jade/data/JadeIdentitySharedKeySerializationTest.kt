package com.blockstream.jade.data

import com.blockstream.jade.api.IdentitySharedKeyRequest
import com.blockstream.jade.api.IdentitySharedKeyRequestParams
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalStdlibApi::class)
class JadeIdentitySharedKeySerializationTest {
    @Test
    fun identitySharedKeyRequestUsesExpectedCborShape() {
        val request = IdentitySharedKeyRequest(
            id = "1000",
            params = IdentitySharedKeyRequestParams(
                identity = "ssh://wallet-abi@blockstream.green",
                curve = "nist256p1",
                theirPubKey = byteArrayOf(1, 2, 3),
                index = 0,
            ),
        )

        assertEquals(
            "a36269646431303030666d6574686f64776765745f6964656e746974795f7368617265645f6b657966706172616d73a4686964656e7469747978227373683a2f2f77616c6c65742d61626940626c6f636b73747265616d2e677265656e656375727665696e69737432353670316c74686569725f7075626b65794301020365696e64657800",
            request.toCborHex(),
        )
    }
}
