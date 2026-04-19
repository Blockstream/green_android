package com.blockstream.green.walletabi.live

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalletAbiLiveSplitSmokeTest {
    @Test
    fun confirms_confidential_split_request_on_liquid_testnet() {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString(ARG_ENABLE_LIVE_SMOKE) == "true")

        val pin = arguments.getString(ARG_PIN) ?: DEFAULT_PIN
        val liveDevice = WalletAbiLiveDevice()
        val senderWallet = liveDevice.unlockWalletToOverview(pin = pin)
        val firstRecipientAddress = liveDevice.recipientConfidentialAddress(pin = pin, walletIndex = 1)
        val secondRecipientAddress = liveDevice.recipientConfidentialAddress(pin = pin, walletIndex = 2)

        liveDevice.injectRequestEnvelope(
            requestEnvelope = splitRequestEnvelope(
                requestId = LIVE_REQUEST_ID,
                firstRecipientAddress = firstRecipientAddress,
                secondRecipientAddress = secondRecipientAddress
            )
        )

        liveDevice.openTransactTab()
        liveDevice.openWalletAbiDemo()
        val txHash = liveDevice.approveWalletAbiRequest()
        liveDevice.waitForTransactionConfirmation(
            wallet = senderWallet,
            pin = pin,
            txHash = txHash
        )
    }

    private fun splitRequestEnvelope(
        requestId: String,
        firstRecipientAddress: String,
        secondRecipientAddress: String
    ): String {
        return """
            {
              "jsonrpc": "2.0",
              "id": "wallet-abi-live-split-envelope",
              "method": "wallet_abi_process_request",
              "params": {
                "abi_version": "wallet-abi-0.1",
                "request_id": "$requestId",
                "network": "testnet-liquid",
                "broadcast": true,
                "params": {
                  "inputs": [],
                  "outputs": [
                    {
                      "id": "output-1",
                      "amount_sat": 700,
                      "lock": {
                        "type": "address",
                        "recipient": {
                          "confidential_address": "$firstRecipientAddress"
                        }
                      },
                      "asset": {
                        "asset_id": "${WalletAbiLiveDevice.TESTNET_POLICY_ASSET}"
                      },
                      "blinder": {
                        "type": "rand"
                      }
                    },
                    {
                      "id": "output-2",
                      "amount_sat": 900,
                      "lock": {
                        "type": "address",
                        "recipient": {
                          "confidential_address": "$secondRecipientAddress"
                        }
                      },
                      "asset": {
                        "asset_id": "${WalletAbiLiveDevice.TESTNET_POLICY_ASSET}"
                      },
                      "blinder": {
                        "type": "rand"
                      }
                    }
                  ],
                  "fee_rate_sat_kvb": 1000
                }
              }
            }
        """.trimIndent()
    }

    private companion object {
        const val ARG_ENABLE_LIVE_SMOKE = "walletAbiLiveSmoke"
        const val ARG_PIN = "walletAbiPin"
        const val DEFAULT_PIN = "111111"
        const val LIVE_REQUEST_ID = "wallet-abi-live-split-request"
    }
}
