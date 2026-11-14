// Verifies the CBOR wire encoding of the sign_liquid_tx request, in particular the
// asset_info entries that carry Liquid asset registry metadata to Jade.
package com.blockstream.jade.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalStdlibApi::class)
class SignLiquidTxSerializationTest {

    @Test
    fun sign_liquid_tx_encodes_asset_info_with_contract_in_registry_key_order() {
        val contract = Json.parseToJsonElement(
            """{"entity":{"domain":"liquidtestnet.com"},"issuer_pubkey":"035d0f7b0207d9cc68870abfef621692bce082084ed3ca0c1ae432dd12d889be01","name":"Testnet Asset","precision":3,"ticker":"TEST","version":0}"""
        ).jsonObject

        val request = SignTransactionRequest(
            id = "1000",
            method = "sign_liquid_tx",
            params = SignTransactionRequestParams(
                network = "testnet-liquid",
                txn = byteArrayOf(0xf),
                numInput = 1,
                useAeSignatures = true,
                change = listOf(null),
                assetInfo = listOf(
                    AssetInfo(
                        assetId = "38fca2d939696061a8f76d4e6b5eecd54e3b4221c846f24a6b279e79952850a5",
                        contract = contract,
                        issuancePrevout = IssuancePrevout(
                            txid = "0e19e938c74378ae83b549213a12be88ede6e32e1407bfdf50c4ec3f927408ec",
                            vout = 0
                        )
                    )
                )
            )
        )

        assertEquals(
            "a36269646431303030666d6574686f646e7369676e5f6c69717569645f747866706172616d73a6676e6574776f726b6e746573746e65742d6c69717569646374786e410f6a6e756d5f696e7075747301717573655f61655f7369676e617475726573f5666368616e676581f66a61737365745f696e666f81a36861737365745f696478403338666361326439333936393630363161386637366434653662356565636435346533623432323163383436663234613662323739653739393532383530613568636f6e7472616374a666656e74697479a166646f6d61696e716c6971756964746573746e65742e636f6d6d6973737565725f7075626b65797842303335643066376230323037643963633638383730616266656636323136393262636530383230383465643363613063316165343332646431326438383962653031646e616d656d546573746e657420417373657469707265636973696f6e03667469636b657264544553546776657273696f6e007069737375616e63655f707265766f7574a2647478696478403065313965393338633734333738616538336235343932313361313262653838656465366533326531343037626664663530633465633366393237343038656364766f757400",
            request.toCborHex()
        )
    }
}
