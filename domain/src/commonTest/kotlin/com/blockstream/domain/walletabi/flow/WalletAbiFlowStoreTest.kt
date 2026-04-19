package com.blockstream.domain.walletabi.flow

import com.blockstream.domain.walletabi.request.WalletAbiInput
import com.blockstream.domain.walletabi.request.WalletAbiNetwork
import com.blockstream.domain.walletabi.request.WalletAbiOutput
import com.blockstream.domain.walletabi.request.WalletAbiParsedRequest
import com.blockstream.domain.walletabi.request.WalletAbiRuntimeParams
import com.blockstream.domain.walletabi.request.WalletAbiTxCreateRequest
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class WalletAbiFlowStoreTest {
    private val parsedRequest = WalletAbiParsedRequest.TxCreate(
        request = WalletAbiTxCreateRequest(
            abiVersion = "wallet-abi-0.1",
            requestId = "request-id",
            network = WalletAbiNetwork.TESTNET_LIQUID,
            params = WalletAbiRuntimeParams(
                inputs = listOf(
                    WalletAbiInput(
                        id = "input-1",
                        utxoSource = JsonPrimitive("utxo-source"),
                        unblinding = JsonPrimitive("unblinding"),
                        sequence = 1L,
                        finalizer = JsonPrimitive("finalizer")
                    )
                ),
                outputs = listOf(
                    WalletAbiOutput(
                        id = "output-1",
                        amountSat = 1_000L,
                        lock = JsonPrimitive("lock"),
                        asset = JsonPrimitive("asset"),
                        blinder = JsonPrimitive("blinder")
                    )
                )
            ),
            broadcast = true
        )
    )

    private val requestContext = WalletAbiStartRequestContext(
        requestId = "request-id",
        walletId = "wallet-id"
    )

    private val review = WalletAbiFlowReview(
        requestContext = requestContext,
        title = "Send",
        message = "Review the request",
        accounts = listOf(
            WalletAbiAccountOption(
                accountId = "account-id",
                name = "Main account"
            )
        ),
        selectedAccountId = "account-id",
        approvalTarget = WalletAbiApprovalTarget.Software,
        parsedRequest = parsedRequest
    )

    private val resolvedReview = review.copy(
        message = "Resolved request",
        selectedAccountId = "account-id-2"
    )

    private val successResult = WalletAbiSuccessResult(
        requestId = review.requestContext.requestId,
        txHash = "tx-hash",
        responseId = "response-id"
    )

    private val retryableExecutionError = WalletAbiFlowError(
        kind = WalletAbiFlowErrorKind.EXECUTION_FAILURE,
        phase = WalletAbiFlowPhase.SUBMISSION,
        message = "Execution failed",
        retryable = true
    )

    private val nonRetryableInvalidRequestError = WalletAbiFlowError(
        kind = WalletAbiFlowErrorKind.INVALID_REQUEST,
        phase = WalletAbiFlowPhase.LOADING,
        message = "Request is malformed",
        retryable = false
    )

    private val jadeReview = review.copy(
        approvalTarget = WalletAbiApprovalTarget.Jade(
            deviceName = "Jade",
            deviceId = "jade-id"
        )
    )

    private val jadeFailedError = WalletAbiFlowError(
        kind = WalletAbiFlowErrorKind.DEVICE_FAILURE,
        phase = WalletAbiFlowPhase.APPROVAL,
        message = "Jade failed",
        retryable = true
    )

    @Test
    fun start_enters_loading() = runTest {
        val store = DefaultWalletAbiFlowStore()

        store.dispatch(WalletAbiFlowIntent.Start(requestContext))

        assertEquals(
            WalletAbiFlowState.Loading(requestContext),
            store.state.value
        )
    }

    @Test
    fun start_emits_load_request() = runTest {
        val store = DefaultWalletAbiFlowStore()
        val output = async(start = CoroutineStart.UNDISPATCHED) { store.outputs.first() }

        store.dispatch(WalletAbiFlowIntent.Start(requestContext))

        assertEquals(
            WalletAbiFlowOutput.LoadRequest(requestContext),
            output.await()
        )
    }

    @Test
    fun request_loaded_event_updates_loading_state() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(requestContext))
        val output = async(start = CoroutineStart.UNDISPATCHED) { store.outputs.first() }

        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.RequestLoaded(review)
            )
        )

        assertEquals(
            WalletAbiFlowState.RequestLoaded(review),
            store.state.value
        )
        assertEquals(
            WalletAbiFlowOutput.PersistSnapshot(
                WalletAbiResumeSnapshot(
                    review = review,
                    phase = WalletAbiResumePhase.REQUEST_LOADED
                )
            ),
            output.await()
        )
    }

    @Test
    fun select_account_updates_request_loaded() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(requestContext))
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
                    review = review.copy(selectedAccountId = "account-id-2"),
                    phase = WalletAbiResumePhase.REQUEST_LOADED
                )
            ),
            output.await()
        )
    }

    @Test
    fun resolved_request_stays_editable() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(requestContext))
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
                    requestContext = requestContext,
                    selectedAccountId = review.selectedAccountId
                )
            ),
            output.await()
        )

        val persistOutput = async(start = CoroutineStart.UNDISPATCHED) { store.outputs.first() }

        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.Resolved(resolvedReview)
            )
        )

        assertEquals(
            WalletAbiFlowState.RequestLoaded(resolvedReview),
            store.state.value
        )
        assertEquals(
            WalletAbiFlowOutput.PersistSnapshot(
                WalletAbiResumeSnapshot(
                    review = resolvedReview,
                    phase = WalletAbiResumePhase.REQUEST_LOADED
                )
            ),
            persistOutput.await()
        )
    }

    @Test
    fun software_approve_starts_submitting_preparing() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(requestContext))
        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.RequestLoaded(review)
            )
        )
        val output = async(start = CoroutineStart.UNDISPATCHED) { store.outputs.first() }

        store.dispatch(WalletAbiFlowIntent.Approve)

        assertEquals(
            WalletAbiFlowState.Submitting(
                requestContext = requestContext,
                stage = WalletAbiSubmittingStage.PREPARING
            ),
            store.state.value
        )
        assertEquals(
            WalletAbiFlowOutput.StartSubmission(
                WalletAbiSubmissionCommand(
                    requestContext = requestContext,
                    selectedAccountId = review.selectedAccountId
                )
            ),
            output.await()
        )
    }

    @Test
    fun submitted_and_broadcasted_update_submission_stage() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(requestContext))
        store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.RequestLoaded(review)))
        store.dispatch(WalletAbiFlowIntent.Approve)

        store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.Submitted))
        assertEquals(
            WalletAbiFlowState.Submitting(
                requestContext = requestContext,
                stage = WalletAbiSubmittingStage.PREPARING
            ),
            store.state.value
        )

        store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.Broadcasted))
        assertEquals(
            WalletAbiFlowState.Submitting(
                requestContext = requestContext,
                stage = WalletAbiSubmittingStage.BROADCASTING
            ),
            store.state.value
        )
    }

    @Test
    fun remote_response_sent_completes_success() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(requestContext))
        store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.RequestLoaded(review)))
        store.dispatch(WalletAbiFlowIntent.Approve)
        store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.Submitted))
        store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.Broadcasted))

        val outputs = async(start = CoroutineStart.UNDISPATCHED) {
            store.outputs.take(2).toList()
        }
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
            listOf(
                WalletAbiFlowOutput.PersistSnapshot(null),
                WalletAbiFlowOutput.Complete(
                    WalletAbiFlowTerminalResult.Success(successResult)
                )
            ),
            outputs.await()
        )
    }

    @Test
    fun reject_ends_cancelled() = runTest {
        val store = DefaultWalletAbiFlowStore()
        val outputs = async(start = CoroutineStart.UNDISPATCHED) {
            store.outputs.take(2).toList()
        }

        store.dispatch(WalletAbiFlowIntent.Reject)

        assertEquals(
            WalletAbiFlowState.Cancelled(WalletAbiCancelledReason.UserRejected),
            store.state.value
        )
        assertEquals(
            listOf(
                WalletAbiFlowOutput.PersistSnapshot(null),
                WalletAbiFlowOutput.Complete(
                    WalletAbiFlowTerminalResult.Cancelled(WalletAbiCancelledReason.UserRejected)
                )
            ),
            outputs.await()
        )
    }

    @Test
    fun cancel_from_loading_emits_cancel_active_work() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(requestContext))
        val output = async(start = CoroutineStart.UNDISPATCHED) { store.outputs.first() }

        store.dispatch(WalletAbiFlowIntent.Cancel)

        assertEquals(
            WalletAbiFlowState.Loading(
                requestContext = requestContext,
                isCancelling = true
            ),
            store.state.value
        )
        assertEquals(
            WalletAbiFlowOutput.CancelActiveWork(WalletAbiFlowPhase.LOADING),
            output.await()
        )
    }

    @Test
    fun cancel_from_awaiting_approval_emits_cancel_active_work() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(requestContext))
        store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.RequestLoaded(jadeReview)))
        store.dispatch(WalletAbiFlowIntent.Approve)
        val output = async(start = CoroutineStart.UNDISPATCHED) { store.outputs.first() }

        store.dispatch(WalletAbiFlowIntent.Cancel)

        assertEquals(
            WalletAbiFlowState.AwaitingApproval(
                requestContext = requestContext,
                selectedAccountId = jadeReview.selectedAccountId,
                jade = WalletAbiJadeContext(
                    deviceId = "jade-id",
                    step = WalletAbiJadeStep.CONNECT,
                    message = null,
                    retryable = false
                ),
                isCancelling = true
            ),
            store.state.value
        )
        assertEquals(
            WalletAbiFlowOutput.CancelActiveWork(WalletAbiFlowPhase.APPROVAL),
            output.await()
        )
    }

    @Test
    fun cancel_from_submitting_preparing_emits_cancel_active_work() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(requestContext))
        store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.RequestLoaded(review)))
        store.dispatch(WalletAbiFlowIntent.Approve)
        val output = async(start = CoroutineStart.UNDISPATCHED) { store.outputs.first() }

        store.dispatch(WalletAbiFlowIntent.Cancel)

        assertEquals(
            WalletAbiFlowState.Submitting(
                requestContext = requestContext,
                stage = WalletAbiSubmittingStage.PREPARING,
                isCancelling = true
            ),
            store.state.value
        )
        assertEquals(
            WalletAbiFlowOutput.CancelActiveWork(WalletAbiFlowPhase.SUBMISSION),
            output.await()
        )
    }

    @Test
    fun cancel_from_submitting_broadcasting_is_ignored() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(requestContext))
        store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.RequestLoaded(review)))
        store.dispatch(WalletAbiFlowIntent.Approve)
        store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.Broadcasted))

        store.dispatch(WalletAbiFlowIntent.Cancel)

        assertEquals(
            WalletAbiFlowState.Submitting(
                requestContext = requestContext,
                stage = WalletAbiSubmittingStage.BROADCASTING
            ),
            store.state.value
        )
    }

    @Test
    fun execution_cancelled_ends_cancelled() = runTest {
        val store = DefaultWalletAbiFlowStore()
        val outputs = async(start = CoroutineStart.UNDISPATCHED) {
            store.outputs.take(2).toList()
        }

        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.Cancelled(WalletAbiCancelledReason.UserCancelled)
            )
        )

        assertEquals(
            WalletAbiFlowState.Cancelled(WalletAbiCancelledReason.UserCancelled),
            store.state.value
        )
        assertEquals(
            listOf(
                WalletAbiFlowOutput.PersistSnapshot(null),
                WalletAbiFlowOutput.Complete(
                    WalletAbiFlowTerminalResult.Cancelled(WalletAbiCancelledReason.UserCancelled)
                )
            ),
            outputs.await()
        )
    }

    @Test
    fun execution_failure_ends_error_and_clears_snapshot() = runTest {
        val store = DefaultWalletAbiFlowStore()
        val outputs = async(start = CoroutineStart.UNDISPATCHED) {
            store.outputs.take(2).toList()
        }

        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.Failed(retryableExecutionError)
            )
        )

        assertEquals(
            WalletAbiFlowState.Error(retryableExecutionError),
            store.state.value
        )
        assertEquals(
            listOf(
                WalletAbiFlowOutput.PersistSnapshot(null),
                WalletAbiFlowOutput.Complete(
                    WalletAbiFlowTerminalResult.Error(retryableExecutionError)
                )
            ),
            outputs.await()
        )
    }

    @Test
    fun retry_retryable_error_returns_to_loading() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(requestContext))
        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.Failed(retryableExecutionError)
            )
        )
        val output = async(start = CoroutineStart.UNDISPATCHED) { store.outputs.first() }

        store.dispatch(WalletAbiFlowIntent.Retry)

        assertEquals(
            WalletAbiFlowState.Loading(requestContext),
            store.state.value
        )
        assertEquals(
            WalletAbiFlowOutput.LoadRequest(requestContext),
            output.await()
        )
    }

    @Test
    fun retry_non_retryable_error_is_ignored() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(requestContext))
        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.Failed(nonRetryableInvalidRequestError)
            )
        )

        store.dispatch(WalletAbiFlowIntent.Retry)

        assertEquals(
            WalletAbiFlowState.Error(nonRetryableInvalidRequestError),
            store.state.value
        )
    }

    @Test
    fun expired_event_ends_cancelled() = runTest {
        val store = DefaultWalletAbiFlowStore()
        val outputs = async(start = CoroutineStart.UNDISPATCHED) {
            store.outputs.take(2).toList()
        }

        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.Expired)
        )

        assertEquals(
            WalletAbiFlowState.Cancelled(WalletAbiCancelledReason.RequestExpired),
            store.state.value
        )
        assertEquals(
            listOf(
                WalletAbiFlowOutput.PersistSnapshot(null),
                WalletAbiFlowOutput.Complete(
                    WalletAbiFlowTerminalResult.Cancelled(WalletAbiCancelledReason.RequestExpired)
                )
            ),
            outputs.await()
        )
    }

    @Test
    fun jade_approve_enters_awaiting_approval() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(requestContext))
        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.RequestLoaded(jadeReview)
            )
        )
        val outputs = async(start = CoroutineStart.UNDISPATCHED) {
            store.outputs.take(2).toList()
        }
        val jadeContext = WalletAbiJadeContext(
            deviceId = "jade-id",
            step = WalletAbiJadeStep.CONNECT,
            message = null,
            retryable = false
        )

        store.dispatch(WalletAbiFlowIntent.Approve)

        assertEquals(
            WalletAbiFlowState.AwaitingApproval(
                requestContext = requestContext,
                selectedAccountId = jadeReview.selectedAccountId,
                jade = jadeContext
            ),
            store.state.value
        )
        assertEquals(
            listOf(
                WalletAbiFlowOutput.PersistSnapshot(
                    WalletAbiResumeSnapshot(
                        review = jadeReview,
                        phase = WalletAbiResumePhase.AWAITING_APPROVAL,
                        jade = jadeContext
                    )
                ),
                WalletAbiFlowOutput.StartApproval(
                    WalletAbiApprovalCommand(
                        requestContext = requestContext,
                        selectedAccountId = jadeReview.selectedAccountId,
                        jade = jadeContext
                    )
                )
            ),
            outputs.await()
        )
    }

    @Test
    fun jade_events_move_awaiting_approval_to_submitting() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(requestContext))
        store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.RequestLoaded(jadeReview)))
        store.dispatch(WalletAbiFlowIntent.Approve)

        store.dispatch(WalletAbiFlowIntent.OnJadeEvent(WalletAbiJadeEvent.Connected))
        assertEquals(WalletAbiJadeStep.UNLOCK, (store.state.value as WalletAbiFlowState.AwaitingApproval).jade.step)

        store.dispatch(WalletAbiFlowIntent.OnJadeEvent(WalletAbiJadeEvent.UnlockConfirmed))
        assertEquals(WalletAbiJadeStep.REVIEW, (store.state.value as WalletAbiFlowState.AwaitingApproval).jade.step)

        store.dispatch(WalletAbiFlowIntent.OnJadeEvent(WalletAbiJadeEvent.ReviewConfirmed))
        assertEquals(WalletAbiJadeStep.SIGN, (store.state.value as WalletAbiFlowState.AwaitingApproval).jade.step)

        val output = async(start = CoroutineStart.UNDISPATCHED) { store.outputs.first() }
        store.dispatch(WalletAbiFlowIntent.OnJadeEvent(WalletAbiJadeEvent.Signed))

        assertEquals(
            WalletAbiFlowState.Submitting(
                requestContext = requestContext,
                stage = WalletAbiSubmittingStage.PREPARING,
                jade = WalletAbiJadeContext(
                    deviceId = "jade-id",
                    step = WalletAbiJadeStep.SIGN,
                    message = null,
                    retryable = false
                )
            ),
            store.state.value
        )
        assertEquals(
            WalletAbiFlowOutput.StartSubmission(
                WalletAbiSubmissionCommand(
                    requestContext = requestContext,
                    selectedAccountId = jadeReview.selectedAccountId
                )
            ),
            output.await()
        )
    }

    @Test
    fun jade_failure_ends_error() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(requestContext))
        store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.RequestLoaded(jadeReview)))
        store.dispatch(WalletAbiFlowIntent.Approve)
        val outputs = async(start = CoroutineStart.UNDISPATCHED) {
            store.outputs.take(2).toList()
        }

        store.dispatch(
            WalletAbiFlowIntent.OnJadeEvent(
                WalletAbiJadeEvent.Failed(jadeFailedError)
            )
        )

        assertEquals(
            WalletAbiFlowState.Error(jadeFailedError),
            store.state.value
        )
        assertEquals(
            listOf(
                WalletAbiFlowOutput.PersistSnapshot(null),
                WalletAbiFlowOutput.Complete(
                    WalletAbiFlowTerminalResult.Error(jadeFailedError)
                )
            ),
            outputs.await()
        )
    }

    @Test
    fun jade_disconnect_ends_retryable_device_failure() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(WalletAbiFlowIntent.Start(requestContext))
        store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.RequestLoaded(jadeReview)))
        store.dispatch(WalletAbiFlowIntent.Approve)
        val outputs = async(start = CoroutineStart.UNDISPATCHED) {
            store.outputs.take(2).toList()
        }

        store.dispatch(WalletAbiFlowIntent.OnJadeEvent(WalletAbiJadeEvent.Disconnected))

        val error = WalletAbiFlowError(
            kind = WalletAbiFlowErrorKind.DEVICE_FAILURE,
            phase = WalletAbiFlowPhase.APPROVAL,
            message = "Jade disconnected",
            retryable = true
        )
        assertEquals(
            WalletAbiFlowState.Error(error),
            store.state.value
        )
        assertEquals(
            listOf(
                WalletAbiFlowOutput.PersistSnapshot(null),
                WalletAbiFlowOutput.Complete(
                    WalletAbiFlowTerminalResult.Error(error)
                )
            ),
            outputs.await()
        )
    }

    @Test
    fun restore_request_loaded_enters_resumable() = runTest {
        val store = DefaultWalletAbiFlowStore()
        val snapshot = WalletAbiResumeSnapshot(
            review = review,
            phase = WalletAbiResumePhase.REQUEST_LOADED
        )
        val output = async(start = CoroutineStart.UNDISPATCHED) { store.outputs.first() }

        store.dispatch(WalletAbiFlowIntent.Restore(snapshot))

        assertEquals(
            WalletAbiFlowState.Resumable(snapshot),
            store.state.value
        )
        assertEquals(
            WalletAbiFlowOutput.PersistSnapshot(snapshot),
            output.await()
        )
    }

    @Test
    fun resume_request_loaded_returns_to_active_state() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(
            WalletAbiFlowIntent.Restore(
                WalletAbiResumeSnapshot(
                    review = review,
                    phase = WalletAbiResumePhase.REQUEST_LOADED
                )
            )
        )

        store.dispatch(WalletAbiFlowIntent.Resume)

        assertEquals(
            WalletAbiFlowState.RequestLoaded(review),
            store.state.value
        )
    }

    @Test
    fun cancel_resume_ends_cancelled() = runTest {
        val store = DefaultWalletAbiFlowStore()
        store.dispatch(
            WalletAbiFlowIntent.Restore(
                WalletAbiResumeSnapshot(
                    review = review,
                    phase = WalletAbiResumePhase.REQUEST_LOADED
                )
            )
        )
        val outputs = async(start = CoroutineStart.UNDISPATCHED) {
            store.outputs.take(2).toList()
        }

        store.dispatch(WalletAbiFlowIntent.CancelResume)

        assertEquals(
            WalletAbiFlowState.Cancelled(WalletAbiCancelledReason.ResumableCancelled),
            store.state.value
        )
        assertEquals(
            listOf(
                WalletAbiFlowOutput.PersistSnapshot(null),
                WalletAbiFlowOutput.Complete(
                    WalletAbiFlowTerminalResult.Cancelled(WalletAbiCancelledReason.ResumableCancelled)
                )
            ),
            outputs.await()
        )
    }

    @Test
    fun submitting_snapshot_restores_as_partial_completion_error() = runTest {
        val store = DefaultWalletAbiFlowStore()

        store.dispatch(
            WalletAbiFlowIntent.Restore(
                WalletAbiResumeSnapshot(
                    review = review,
                    phase = WalletAbiResumePhase.SUBMITTING
                )
            )
        )

        assertEquals(
            WalletAbiFlowState.Error(
                WalletAbiFlowError(
                    kind = WalletAbiFlowErrorKind.PARTIAL_COMPLETION,
                    phase = WalletAbiFlowPhase.SUBMISSION,
                    message = "Transaction status may already have changed. Check your wallet activity before retrying.",
                    retryable = false
                )
            ),
            store.state.value
        )
    }
}
