package com.blockstream.common.utils

import com.blockstream.common.extensions.getSafeQueryParameter
import com.eygraber.uri.toKmpUri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalUnsignedTypes::class)
class KmpUri {

    @Test
    fun test_getSafeQueryParameter() {
        "liquidnetwork:lq1qqd0562tsgz2x5ev49vlg7rfncn9mu57fqjmsuy2wmrmkny7wk3jgkjgh9fpmj2qkx600lw0g46dy7870qt9kslu63jsw4vh7a?assetid=0e99c1a6da379d1f4151fb9df90449d40d0608f6cb33a5bcbfc8c265f42bab0a&amount=1".toKmpUri()
            .also {
                // Extract specific query parameter values using opaque-safe parser
                assertEquals("0e99c1a6da379d1f4151fb9df90449d40d0608f6cb33a5bcbfc8c265f42bab0a", it.getSafeQueryParameter("assetid"))
                assertEquals("1", it.getSafeQueryParameter("amount"))
                assertNull(it.getSafeQueryParameter("null"))
            }
    }
}
