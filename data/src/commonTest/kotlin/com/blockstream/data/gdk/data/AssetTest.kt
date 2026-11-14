// Verifies that Asset retains the registry fields Jade needs to authenticate asset metadata
// when parsed from GDK's get_assets JSON.
package com.blockstream.data.gdk.data

import com.blockstream.data.gdk.JsonConverter
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AssetTest {

    @Test
    fun keeps_registry_contract_and_issuance_prevout_from_gdk_json() {
        val json = """{"asset_id":"38fca2d939696061a8f76d4e6b5eecd54e3b4221c846f24a6b279e79952850a5","contract":{"entity":{"domain":"liquidtestnet.com"},"issuer_pubkey":"035d0f7b0207d9cc68870abfef621692bce082084ed3ca0c1ae432dd12d889be01","name":"Testnet Asset","precision":3,"ticker":"TEST","version":0},"entity":{"domain":"liquidtestnet.com"},"issuance_prevout":{"txid":"0e19e938c74378ae83b549213a12be88ede6e32e1407bfdf50c4ec3f927408ec","vout":0},"issuance_txin":{"txid":"0e19e938c74378ae83b549213a12be88ede6e32e1407bfdf50c4ec3f927408ec","vin":0},"issuer_pubkey":"035d0f7b0207d9cc68870abfef621692bce082084ed3ca0c1ae432dd12d889be01","name":"Testnet Asset","precision":3,"ticker":"TEST","version":0}"""

        val asset = JsonConverter.JsonDeserializer.decodeFromString(Asset.serializer(), json)

        assertEquals(
            listOf("entity", "issuer_pubkey", "name", "precision", "ticker", "version"),
            asset.contract!!.keys.toList()
        )
        assertEquals("TEST", asset.contract!!["ticker"]!!.jsonPrimitive.content)
        assertEquals("0e19e938c74378ae83b549213a12be88ede6e32e1407bfdf50c4ec3f927408ec", asset.issuancePrevout!!.txid)
        assertEquals(0, asset.issuancePrevout!!.vout)
    }

    @Test
    fun policy_asset_has_no_contract() {
        val json = """{"asset_id":"144c654344aa716d6f3abcc1ca90e5641e4e2a7f633bc09fe3baf64585819a49","contract":null,"entity":null,"issuance_prevout":{"txid":"0000000000000000000000000000000000000000000000000000000000000000","vout":0},"name":"btc","precision":8,"ticker":"L-TEST","version":0}"""

        val asset = JsonConverter.JsonDeserializer.decodeFromString(Asset.serializer(), json)

        assertNull(asset.contract)
    }
}
