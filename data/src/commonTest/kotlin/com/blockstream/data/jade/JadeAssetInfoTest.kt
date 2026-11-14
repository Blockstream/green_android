// Verifies how transaction outputs are turned into the asset_info entries sent to Jade:
// which asset ids are looked up in the registry and which entries are forwarded.
package com.blockstream.data.jade

import com.blockstream.data.gdk.data.Asset
import com.blockstream.data.gdk.data.Entity
import com.blockstream.data.gdk.data.InputOutput
import com.blockstream.data.gdk.data.LiquidAssets
import com.blockstream.data.gdk.params.AssetsParams
import com.blockstream.data.gdk.params.GetAssetsParams
import com.blockstream.data.managers.AssetsProvider
import com.blockstream.jade.api.IssuancePrevout
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JadeAssetInfoTest {

    private class RecordingAssetsProvider(
        private val registry: Map<String, Asset>,
        private val failure: Exception? = null
    ) : AssetsProvider {
        val requested = mutableListOf<List<String>>()

        override suspend fun refreshAssets(params: AssetsParams) {}

        override suspend fun getAssets(params: GetAssetsParams): LiquidAssets {
            requested += params.assets
            failure?.let { throw it }
            return LiquidAssets(assets = registry.filterKeys { it in params.assets })
        }
    }

    private val policyAsset = "144c654344aa716d6f3abcc1ca90e5641e4e2a7f633bc09fe3baf64585819a49"
    private val testAssetId = "38fca2d939696061a8f76d4e6b5eecd54e3b4221c846f24a6b279e79952850a5"
    private val unknownAssetId = "aa".repeat(32)

    private val testAssetContract = Json.parseToJsonElement(
        """{"entity":{"domain":"liquidtestnet.com"},"issuer_pubkey":"035d0f7b0207d9cc68870abfef621692bce082084ed3ca0c1ae432dd12d889be01","name":"Testnet Asset","precision":3,"ticker":"TEST","version":0}"""
    ).jsonObject

    private val testAsset = Asset(
        name = "Testnet Asset",
        assetId = testAssetId,
        precision = 3,
        ticker = "TEST",
        entity = Entity("liquidtestnet.com"),
        contract = testAssetContract,
        issuancePrevout = IssuancePrevout(
            txid = "0e19e938c74378ae83b549213a12be88ede6e32e1407bfdf50c4ec3f927408ec",
            vout = 0
        )
    )

    private val policyAssetEntry = Asset(name = "btc", assetId = policyAsset, precision = 8, ticker = "L-TEST")

    private fun output(assetId: String) = InputOutput(assetId = assetId, satoshi = 1000)

    @Test
    fun looks_up_distinct_non_policy_output_assets_only() = runTest {
        val provider = RecordingAssetsProvider(mapOf(testAssetId to testAsset))

        assetInfoForOutputs(
            outputs = listOf(output(testAssetId), output(policyAsset), output(testAssetId), output(unknownAssetId)),
            policyAsset = policyAsset,
            assetsProvider = provider
        )

        assertEquals(listOf(listOf(testAssetId, unknownAssetId)), provider.requested)
    }

    @Test
    fun maps_registry_entries_to_jade_asset_info() = runTest {
        val provider = RecordingAssetsProvider(mapOf(testAssetId to testAsset))

        val assetInfo = assetInfoForOutputs(
            outputs = listOf(output(testAssetId), output(policyAsset)),
            policyAsset = policyAsset,
            assetsProvider = provider
        )

        assertEquals(1, assetInfo.size)
        assertEquals(testAssetId, assetInfo[0].assetId)
        assertEquals(testAssetContract, assetInfo[0].contract)
        assertEquals("0e19e938c74378ae83b549213a12be88ede6e32e1407bfdf50c4ec3f927408ec", assetInfo[0].issuancePrevout.txid)
        assertEquals(0, assetInfo[0].issuancePrevout.vout)
    }

    @Test
    fun skips_assets_missing_from_registry_or_without_contract() = runTest {
        val provider = RecordingAssetsProvider(
            mapOf(testAssetId to testAsset, policyAsset to policyAssetEntry)
        )

        val assetInfo = assetInfoForOutputs(
            outputs = listOf(output(unknownAssetId), output(policyAsset), output(testAssetId)),
            policyAsset = "not-the-policy-asset-of-this-network",
            assetsProvider = provider
        )

        assertEquals(listOf(testAssetId), assetInfo.map { it.assetId })
    }

    @Test
    fun sends_no_asset_info_when_registry_lookup_fails() = runTest {
        val provider = RecordingAssetsProvider(mapOf(testAssetId to testAsset), failure = Exception("registry unavailable"))

        val assetInfo = assetInfoForOutputs(
            outputs = listOf(output(testAssetId)),
            policyAsset = policyAsset,
            assetsProvider = provider
        )

        assertTrue(assetInfo.isEmpty())
    }

    @Test
    fun does_not_query_registry_when_only_the_policy_asset_is_sent() = runTest {
        val provider = RecordingAssetsProvider(mapOf())

        val assetInfo = assetInfoForOutputs(
            outputs = listOf(output(policyAsset), output(policyAsset)),
            policyAsset = policyAsset,
            assetsProvider = provider
        )

        assertTrue(assetInfo.isEmpty())
        assertTrue(provider.requested.isEmpty())
    }
}
