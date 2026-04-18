package com.blockstream.domain.walletabi.flow

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class WalletAbiFlowStoreTest {

    private val review = WalletAbiFlowReview(
        requestContext = WalletAbiStartRequestContext(
            requestId = "request-id",
            walletId = "wallet-id"
        ),
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

    private val resolvedReview = review.copy(
        message = "Resolved request",
        selectedAccountId = "account-id-2"
    )
    private val successResult = WalletAbiSuccessResult(
        requestId = review.requestContext.requestId,
        responseId = "response-id"
    )
    private val failedError = WalletAbiFlowError("Execution failed")

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

    @Test
    fun select_account_updates_request_loaded() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(review.requestContext))
        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.RequestLoaded(review)
            )
        )
        val output = async(start = CoroutineStart.UNDISPATCHED) { store.outputs.first() }

        store.dispatch(WalletAbiFlowIntent.SelectAccount("account-id-2"))

        assertEquals(
            WalletAbiFlowState.RequestLoaded(
                review.copy(selectedAccountId = "account-id-2")
            ),
            store.state.value
        )
        assertEquals(
            WalletAbiFlowOutput.PersistSnapshot(
                WalletAbiResumeSnapshot(
                    requestContext = review.requestContext,
                    selectedAccountId = "account-id-2"
                )
            ),
            output.await()
        )
    }

    @Test
    fun resolved_request_stays_editable() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(review.requestContext))
        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.RequestLoaded(review)
            )
        )
        val output = async(start = CoroutineStart.UNDISPATCHED) { store.outputs.first() }

        store.dispatch(WalletAbiFlowIntent.ResolveRequest)

        assertEquals(
            WalletAbiFlowOutput.StartResolution(
                WalletAbiResolutionCommand(
                    requestContext = review.requestContext,
                    selectedAccountId = review.selectedAccountId
                )
            ),
            output.await()
        )

        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.Resolved(resolvedReview)
            )
        )

        assertEquals(
            WalletAbiFlowState.RequestLoaded(resolvedReview),
            store.state.value
        )
    }

    @Test
    fun software_approve_starts_submitting() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(review.requestContext))
        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.RequestLoaded(review)
            )
        )
        val output = async(start = CoroutineStart.UNDISPATCHED) { store.outputs.first() }

        store.dispatch(WalletAbiFlowIntent.Approve)

        assertEquals(
            WalletAbiFlowState.Submitting(review.requestContext),
            store.state.value
        )
        assertEquals(
            WalletAbiFlowOutput.StartSubmission(
                WalletAbiSubmissionCommand(
                    requestContext = review.requestContext,
                    selectedAccountId = review.selectedAccountId
                )
            ),
            output.await()
        )
    }

    @Test
    fun remote_response_sent_completes_success() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(review.requestContext))
        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.RequestLoaded(review)
            )
        )
        store.dispatch(WalletAbiFlowIntent.Approve)

        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.Submitted)
        )
        assertEquals(
            WalletAbiFlowState.Submitting(review.requestContext),
            store.state.value
        )

        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.Broadcasted)
        )
        assertEquals(
            WalletAbiFlowState.Submitting(review.requestContext),
            store.state.value
        )

        val output = async(start = CoroutineStart.UNDISPATCHED) { store.outputs.first() }
        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.RemoteResponseSent(successResult)
            )
        )

        assertEquals(
            WalletAbiFlowState.Success(successResult),
            store.state.value
        )
        assertEquals(
            WalletAbiFlowOutput.Complete(
                WalletAbiFlowTerminalResult.Success(successResult)
            ),
            output.await()
        )
    }

    @Test
    fun reject_ends_cancelled() = runTest {
        val store = DefaultWalletAbiFlowStore()
        val output = async(start = CoroutineStart.UNDISPATCHED) { store.outputs.first() }

        store.dispatch(WalletAbiFlowIntent.Reject)

        assertEquals(
            WalletAbiFlowState.Cancelled(WalletAbiCancelledReason.UserRejected),
            store.state.value
        )
        assertEquals(
            WalletAbiFlowOutput.Complete(
                WalletAbiFlowTerminalResult.Cancelled(WalletAbiCancelledReason.UserRejected)
            ),
            output.await()
        )
    }

    @Test
    fun execution_failure_ends_error() = runTest {
        val store = DefaultWalletAbiFlowStore()
        val output = async(start = CoroutineStart.UNDISPATCHED) { store.outputs.first() }

        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.Failed(failedError)
            )
        )

        assertEquals(
            WalletAbiFlowState.Error(failedError),
            store.state.value
        )
        assertEquals(
            WalletAbiFlowOutput.Complete(
                WalletAbiFlowTerminalResult.Error(failedError)
            ),
            output.await()
        )
    }

    @Test
    fun expired_event_ends_cancelled() = runTest {
        val store = DefaultWalletAbiFlowStore()
        val output = async(start = CoroutineStart.UNDISPATCHED) { store.outputs.first() }

        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.Expired)
        )

        assertEquals(
            WalletAbiFlowState.Cancelled(WalletAbiCancelledReason.RequestExpired),
            store.state.value
        )
        assertEquals(
            WalletAbiFlowOutput.Complete(
                WalletAbiFlowTerminalResult.Cancelled(WalletAbiCancelledReason.RequestExpired)
            ),
            output.await()
        )
    }
}
