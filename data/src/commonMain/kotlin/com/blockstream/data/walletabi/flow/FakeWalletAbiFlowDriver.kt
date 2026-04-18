package com.blockstream.data.walletabi.flow

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeWalletAbiFlowDriver {
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
