package com.blockstream.data.walletabi.flow

import org.junit.Test
import kotlin.test.assertEquals

class FakeWalletAbiFlowDriverTest {
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
}
