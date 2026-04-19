package com.blockstream.data.walletabi.request

fun interface WalletAbiDemoRequestSource {
    fun loadRequestEnvelope(requestId: String): String
}

class DefaultWalletAbiDemoRequestSource(
    private val requestEnvelopeLoader: (String) -> String = ::defaultRequestEnvelope
) : WalletAbiDemoRequestSource {
    override fun loadRequestEnvelope(requestId: String): String {
        return requestEnvelopeLoader(requestId)
    }
}

private fun defaultRequestEnvelope(requestId: String): String {
    return """
        {
          "jsonrpc": "2.0",
          "id": "wallet-abi-demo-envelope",
          "method": "wallet_abi_process_request",
          "params": {
            "abi_version": "wallet-abi-0.1",
            "request_id": "$requestId",
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
                },
                {
                  "id": "output-2",
                  "amount_sat": 2000,
                  "lock": {
                    "type": "script",
                    "script": "00141111111111111111111111111111111111111111"
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
    """.trimIndent()
}
