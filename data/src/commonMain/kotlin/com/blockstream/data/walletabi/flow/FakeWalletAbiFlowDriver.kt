package com.blockstream.data.walletabi.flow

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeWalletAbiFlowDriver {
    fun loadRequestEnvelope(requestId: String): String {
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
        """.trimIndent()
    }

    fun loadRequest(
        requestId: String,
        walletId: String
    ): WalletAbiFlowReviewPayload {
        return WalletAbiFlowReviewPayload(
            requestId = requestId,
            walletId = walletId,
            title = "Demo payment",
            message = "Approve a fake Wallet ABI request",
            accounts = listOf(
                WalletAbiAccountOptionPayload(
                    accountId = "fake-account-1",
                    name = "Main account"
                )
            ),
            selectedAccountId = "fake-account-1",
            approvalTarget = WalletAbiApprovalTargetPayload(
                kind = "software"
            )
        )
    }

    fun resolveRequest(review: WalletAbiFlowReviewPayload): WalletAbiFlowReviewPayload {
        return review.copy(
            selectedAccountId = review.selectedAccountId
        )
    }

    fun submitRequest(requestId: String): Flow<FakeWalletAbiSubmissionEvent> = flow {
        emit(FakeWalletAbiSubmissionEvent.Submitted)
        delay(200)
        emit(FakeWalletAbiSubmissionEvent.Broadcasted)
        delay(200)
        emit(
            FakeWalletAbiSubmissionEvent.RemoteResponseSent(
                result = WalletAbiSuccessPayload(
                    requestId = requestId,
                    responseId = "wallet-abi-demo-response"
                )
            )
        )
    }
}

data class WalletAbiSuccessPayload(
    val requestId: String,
    val responseId: String
)

sealed interface FakeWalletAbiSubmissionEvent {
    data object Submitted : FakeWalletAbiSubmissionEvent
    data object Broadcasted : FakeWalletAbiSubmissionEvent

    data class RemoteResponseSent(
        val result: WalletAbiSuccessPayload
    ) : FakeWalletAbiSubmissionEvent
}
