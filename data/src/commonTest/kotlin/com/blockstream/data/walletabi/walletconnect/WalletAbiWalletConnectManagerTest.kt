package com.blockstream.data.walletabi.walletconnect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WalletAbiWalletConnectManagerTest {
    @Test
    fun requestIdTimestampMs_acceptsMillisecondIds() {
        assertEquals(
            1_776_784_008_884uL,
            walletAbiWalletConnectRequestIdTimestampMsOrNull(1_776_784_008_884uL),
        )
    }

    @Test
    fun requestIdTimestampMs_normalizesMicrosecondIds() {
        assertEquals(
            1_776_784_008_884uL,
            walletAbiWalletConnectRequestIdTimestampMsOrNull(1_776_784_008_884_800uL),
        )
    }

    @Test
    fun requestIdTimestampMs_ignoresSequentialIds() {
        assertNull(walletAbiWalletConnectRequestIdTimestampMsOrNull(42uL))
    }
}
