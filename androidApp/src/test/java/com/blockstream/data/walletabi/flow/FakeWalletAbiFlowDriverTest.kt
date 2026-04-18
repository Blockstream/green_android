package com.blockstream.data.walletabi.flow

import org.junit.Test
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals

class FakeWalletAbiFlowDriverTest {
    @Test
    fun loadRequestEnvelope_returns_deterministic_json_rpc_request() {
        val driver = FakeWalletAbiFlowDriver()

        val envelope = driver.loadRequestEnvelope(
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
                      "inputs": [
                        {
                          "id": "input-1",
                          "utxo_source": { "kind": "wallet" },
                          "unblinding": { "kind": "known" },
                          "sequence": 1,
                          "finalizer": { "kind": "default" }
                        }
                      ],
                      "outputs": [
                        {
                          "id": "output-1",
                          "amount_sat": 1000,
                          "lock": { "kind": "pkh" },
                          "asset": { "kind": "btc" },
                          "blinder": { "kind": "default" }
                        }
                      ],
                      "fee_rate_sat_kvb": 12.5,
                      "lock_time": 500000
                    },
                    "broadcast": true
                  }
                }
            """.trimIndent(),
            envelope
        )
    }

    @Test
    fun loadRequest_returns_deterministic_software_review() {
        val driver = FakeWalletAbiFlowDriver()
        val requestId = "wallet-abi-demo-request"
        val walletId = "wallet-id"

        val review = driver.loadRequest(
            requestId = requestId,
            walletId = walletId
        )

        assertEquals(requestId, review.requestId)
        assertEquals(walletId, review.walletId)
        assertEquals("Demo payment", review.title)
        assertEquals("Approve a fake Wallet ABI request", review.message)
        assertEquals("fake-account-1", review.selectedAccountId)
        assertEquals(1, review.accounts.size)
        assertEquals("fake-account-1", review.accounts.single().accountId)
        assertEquals("Main account", review.accounts.single().name)
        assertEquals("software", review.approvalTarget.kind)
    }

    @Test
    fun resolveRequest_keeps_loaded_review_selected_account() {
        val driver = FakeWalletAbiFlowDriver()
        val review = driver.loadRequest(
            requestId = "wallet-abi-demo-request",
            walletId = "wallet-id"
        )

        val resolvedReview = driver.resolveRequest(review)

        assertEquals(review, resolvedReview)
    }

    @Test
    fun submitRequest_emits_success_sequence() = runTest {
        val driver = FakeWalletAbiFlowDriver()

        val events = driver.submitRequest(
            requestId = "wallet-abi-demo-request"
        ).toList()

        assertEquals(
            listOf(
                FakeWalletAbiSubmissionEvent.Submitted,
                FakeWalletAbiSubmissionEvent.Broadcasted,
                FakeWalletAbiSubmissionEvent.RemoteResponseSent(
                    result = WalletAbiSuccessPayload(
                        requestId = "wallet-abi-demo-request",
                        responseId = "wallet-abi-demo-response"
                    )
                )
            ),
            events
        )
    }
}
