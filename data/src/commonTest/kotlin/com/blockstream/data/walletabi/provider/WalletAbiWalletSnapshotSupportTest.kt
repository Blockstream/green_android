package com.blockstream.data.walletabi.provider

import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.InputOutput
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.data.Utxo
import lwk.LwkException
import lwk.Script
import lwk.TxOut
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WalletAbiWalletSnapshotSupportTest {
    @Test
    fun outspendsCache_reusesEntriesUntilTtlExpires() {
        var nowMs = 1_000L
        val cache = WalletAbiOutspendsCache(
            ttlMs = 1_000L,
            nowMs = { nowMs },
        )
        val outspends = listOf(WalletAbiEsploraOutspend(spent = false))

        assertNull(cache.get("https://example.test/api", TEST_TXID))

        cache.put("https://example.test/api/", TEST_TXID, outspends)

        assertEquals(outspends, cache.get("https://example.test/api", TEST_TXID))

        nowMs = 2_001L

        assertNull(cache.get("https://example.test/api", TEST_TXID))
    }

    @Test
    fun indexedUtxo_buildsExplicitTxOutFromWalletData() {
        val indexed = WalletAbiIndexedUtxo(
            account = liquidAccount(),
            io = InputOutput(
                scriptPubkey = TEST_SCRIPT,
                assetId = TESTNET_POLICY_ASSET,
                satoshi = 1_000L,
                txHash = TEST_TXID,
                ptIdx = 0L,
            ),
            utxo = Utxo(
                assetId = TESTNET_POLICY_ASSET,
                addressType = "p2wpkh",
                satoshi = 1_000L,
                txHash = TEST_TXID,
                index = 0L,
            ),
        )

        val txOut = assertNotNull(indexed.toWalletAbiExplicitTxOutOrNull())

        assertEquals(TESTNET_POLICY_ASSET, txOut.asset())
        assertEquals(1_000uL, txOut.value())
        assertEquals(TEST_SCRIPT, txOut.scriptPubkey().toString())
    }

    @Test
    fun indexedUtxo_doesNotBuildExplicitTxOutForBlindedWalletData() {
        val indexed = WalletAbiIndexedUtxo(
            account = liquidAccount(),
            io = InputOutput(
                scriptPubkey = TEST_SCRIPT,
                assetId = TESTNET_POLICY_ASSET,
                satoshi = 1_000L,
                txHash = TEST_TXID,
                ptIdx = 0L,
                isBlinded = true,
            ),
            utxo = Utxo(
                assetId = TESTNET_POLICY_ASSET,
                addressType = "p2wpkh",
                satoshi = 1_000L,
                txHash = TEST_TXID,
                index = 0L,
            ),
        )

        assertNull(indexed.toWalletAbiExplicitTxOutOrNull())
    }

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
        const val TEST_SCRIPT = "00141111111111111111111111111111111111111111"
        const val TEST_TXID = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

        fun liquidAccount(): Account {
            return Account(
                networkInjected = Network(
                    network = Network.GreenTestnetLiquid,
                    name = "Liquid Testnet",
                    isMainnet = false,
                    isLiquid = true,
                    isDevelopment = false,
                    policyAsset = TESTNET_POLICY_ASSET,
                ),
                gdkName = "Liquid account",
                pointer = 0L,
                type = AccountType.BIP84_SEGWIT,
            )
        }
    }
}
