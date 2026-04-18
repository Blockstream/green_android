package com.blockstream.data.walletabi.request

import org.junit.Test
import kotlin.test.assertEquals

class WalletAbiDemoRequestSourceTest {
    @Test
    fun loadRequestEnvelope_returns_real_execution_demo_payload() {
        val source = DefaultWalletAbiDemoRequestSource()

        val envelope = source.loadRequestEnvelope(
            requestId = "wallet-abi-demo-request"
        )

        assertEquals(
            """
                {
                  "jsonrpc": "2.0",
                  "id": "wallet-abi-demo-envelope",
                  "method": "wallet_abi_process_request",
                  "params": {
                    "abi_version": "wallet-abi-0.1",
                    "request_id": "wallet-abi-demo-request",
                    "network": "testnet-liquid",
                    "params": {
                      "inputs": [],
                      "outputs": [
                        {
                          "id": "output-1",
                          "amount_sat": 1000,
                          "lock": {
                            "type": "script",
                            "script": "00140000000000000000000000000000000000000000"
                          },
                          "asset": {
                            "asset_id": "144c654344aa716d6f3abcc1ca90e5641e4e2a7f633bc09fe3baf64585819a49"
                          },
                          "blinder": {
                            "type": "rand"
                          }
                        }
                      ],
                      "fee_rate_sat_kvb": 12000
                    },
                    "broadcast": true
                  }
                }
            """.trimIndent(),
            envelope
        )
    }

    @Test
    fun loadRequestEnvelope_uses_custom_loader() {
        val source = DefaultWalletAbiDemoRequestSource(
            requestEnvelopeLoader = { requestId ->
                """{"request_id":"$requestId","kind":"custom"}"""
            }
        )

        val envelope = source.loadRequestEnvelope(
            requestId = "wallet-abi-demo-request"
        )

        assertEquals(
            """{"request_id":"wallet-abi-demo-request","kind":"custom"}""",
            envelope
        )
    }
}
