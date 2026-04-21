package com.blockstream.data.walletabi.provider

import lwk.LwkException
import lwk.Script
import lwk.TxOut
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WalletAbiWalletSnapshotSupportTest {
    @Test
    fun jadeChainTxOut_rejects_missing_chain_prevout() {
        val error = assertFailsWith<LwkException.Generic> {
            walletAbiRequireJadeChainTxOut(
                outpointKey = "00:0",
                txOut = null,
            )
        }

        assertEquals("Wallet ABI Jade txout not found on chain for outpoint 00:0", error.msg)
    }

    @Test
    fun jadeChainTxOut_accepts_explicit_prevout() {
        val txOut = TxOut.fromExplicit(
            scriptPubkey = Script("00141111111111111111111111111111111111111111"),
            assetId = TESTNET_POLICY_ASSET,
            satoshi = 1000uL,
        )

        val resolved = walletAbiRequireJadeChainTxOut(
            outpointKey = "11:1",
            txOut = txOut,
        )

        assertEquals(txOut, resolved)
    }

    private companion object {
        const val TESTNET_POLICY_ASSET =
            "144c654344aa716d6f3abcc1ca90e5641e4e2a7f633bc09fe3baf64585819a49"
    }
}
