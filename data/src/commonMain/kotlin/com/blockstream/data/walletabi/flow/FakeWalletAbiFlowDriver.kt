package com.blockstream.data.walletabi.flow

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
}
