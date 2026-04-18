package com.blockstream.domain.walletabi.flow

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class WalletAbiFlowStoreTest {

    private val review = WalletAbiFlowReview(
        title = "Send",
        message = "Review the request",
        accounts = listOf(
            WalletAbiAccountOption(
                accountId = "account-id",
                name = "Main account"
            )
        ),
        selectedAccountId = "account-id",
        approvalTarget = WalletAbiApprovalTarget.Software
    )

    @Test
    fun start_enters_loading() = runTest {
        val store = DefaultWalletAbiFlowStore()
        val requestContext = WalletAbiStartRequestContext(
            requestId = "request-id",
            walletId = "wallet-id"
        )

        store.dispatch(WalletAbiFlowIntent.Start(requestContext))

        assertEquals(
            WalletAbiFlowState.Loading(requestContext),
            store.state.value
        )
    }

    @Test
    fun request_loaded_event_updates_loading_state() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(
            WalletAbiFlowIntent.Start(
                WalletAbiStartRequestContext(
                    requestId = "request-id",
                    walletId = "wallet-id"
                )
            )
        )

        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.RequestLoaded(review)
            )
        )

        assertEquals(
            WalletAbiFlowState.RequestLoaded(review),
            store.state.value
        )
    }
}
