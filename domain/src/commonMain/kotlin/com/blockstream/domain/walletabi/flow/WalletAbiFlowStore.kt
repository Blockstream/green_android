package com.blockstream.domain.walletabi.flow

import com.blockstream.domain.walletabi.request.WalletAbiParsedRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

interface WalletAbiFlowStore {
    val state: StateFlow<WalletAbiFlowState>
    val outputs: Flow<WalletAbiFlowOutput>

    suspend fun dispatch(intent: WalletAbiFlowIntent)
}

class DefaultWalletAbiFlowStore : WalletAbiFlowStore {
    private val mutableState = MutableStateFlow<WalletAbiFlowState>(WalletAbiFlowState.Idle)
    private val mutableOutputs = MutableSharedFlow<WalletAbiFlowOutput>(extraBufferCapacity = 8)
    private var lastRequestContext: WalletAbiStartRequestContext? = null

    override val state: StateFlow<WalletAbiFlowState> = mutableState.asStateFlow()
    override val outputs: Flow<WalletAbiFlowOutput> = mutableOutputs.asSharedFlow()

    override suspend fun dispatch(intent: WalletAbiFlowIntent) {
        when (intent) {
            WalletAbiFlowIntent.Approve -> handleApprove()
            WalletAbiFlowIntent.Cancel -> handleCancel()
            WalletAbiFlowIntent.CancelResume -> handleCancelResume()
            WalletAbiFlowIntent.DismissTerminal -> handleDismissTerminal()
            WalletAbiFlowIntent.Reject -> handleReject()
            is WalletAbiFlowIntent.OnJadeEvent -> handleJadeEvent(intent.event)
            is WalletAbiFlowIntent.Restore -> handleRestore(intent)
            WalletAbiFlowIntent.Retry -> handleRetry()
            WalletAbiFlowIntent.Resume -> handleResume()
            is WalletAbiFlowIntent.Start -> handleStart(intent)
            WalletAbiFlowIntent.ResolveRequest -> handleResolveRequest()
            is WalletAbiFlowIntent.SelectAccount -> handleSelectAccount(intent)
            is WalletAbiFlowIntent.OnExecutionEvent -> handleExecutionEvent(intent.event)
        }
    }

    private suspend fun handleStart(intent: WalletAbiFlowIntent.Start) {
        lastRequestContext = intent.requestContext
        mutableState.value = WalletAbiFlowState.Loading(intent.requestContext)
        mutableOutputs.emit(
            WalletAbiFlowOutput.LoadRequest(intent.requestContext)
        )
    }

    private suspend fun handleExecutionEvent(event: WalletAbiExecutionEvent) {
        when (event) {
            WalletAbiExecutionEvent.Broadcasted -> handleExecutionBroadcasted()
            is WalletAbiExecutionEvent.Cancelled -> handleExecutionCancelled(event)
            WalletAbiExecutionEvent.Expired -> handleExecutionExpired()
            is WalletAbiExecutionEvent.Failed -> handleExecutionFailed(event)
            is WalletAbiExecutionEvent.RequestLoaded -> handleExecutionRequestLoaded(event)
            is WalletAbiExecutionEvent.Resolved -> handleExecutionResolved(event)
            is WalletAbiExecutionEvent.RemoteResponseSent -> handleExecutionRemoteResponseSent(event)
            WalletAbiExecutionEvent.Submitted -> handleExecutionSubmitted()
        }
    }

    private suspend fun handleExecutionRequestLoaded(event: WalletAbiExecutionEvent.RequestLoaded) {
        lastRequestContext = event.review.requestContext
        mutableState.value = WalletAbiFlowState.RequestLoaded(event.review)
        mutableOutputs.emit(
            WalletAbiFlowOutput.PersistSnapshot(
                WalletAbiResumeSnapshot(
                    review = event.review,
                    phase = WalletAbiResumePhase.REQUEST_LOADED
                )
            )
        )
    }

    private suspend fun handleExecutionResolved(event: WalletAbiExecutionEvent.Resolved) {
        lastRequestContext = event.review.requestContext
        mutableState.value = WalletAbiFlowState.RequestLoaded(event.review)
        mutableOutputs.emit(
            WalletAbiFlowOutput.PersistSnapshot(
                WalletAbiResumeSnapshot(
                    review = event.review,
                    phase = WalletAbiResumePhase.REQUEST_LOADED
                )
            )
        )
    }

    private fun handleExecutionSubmitted() {
        val currentState = mutableState.value as? WalletAbiFlowState.Submitting ?: return
        mutableState.value = currentState.copy(
            stage = WalletAbiSubmittingStage.PREPARING,
            isCancelling = false
        )
    }

    private fun handleExecutionBroadcasted() {
        val currentState = mutableState.value as? WalletAbiFlowState.Submitting ?: return
        mutableState.value = currentState.copy(
            stage = WalletAbiSubmittingStage.BROADCASTING,
            isCancelling = false
        )
    }

    private suspend fun handleExecutionRemoteResponseSent(
        event: WalletAbiExecutionEvent.RemoteResponseSent
    ) {
        mutableState.value = WalletAbiFlowState.Success(event.result)
        mutableOutputs.emit(WalletAbiFlowOutput.PersistSnapshot(null))
        mutableOutputs.emit(
            WalletAbiFlowOutput.Complete(
                WalletAbiFlowTerminalResult.Success(event.result)
            )
        )
    }

    private suspend fun handleExecutionCancelled(event: WalletAbiExecutionEvent.Cancelled) {
        emitCancelled(event.reason)
    }

    private suspend fun handleSelectAccount(intent: WalletAbiFlowIntent.SelectAccount) {
        val currentState = mutableState.value as? WalletAbiFlowState.RequestLoaded ?: return
        val updatedReview = currentState.review.copy(selectedAccountId = intent.accountId)
        mutableState.value = WalletAbiFlowState.RequestLoaded(updatedReview)
        mutableOutputs.emit(
            WalletAbiFlowOutput.PersistSnapshot(
                WalletAbiResumeSnapshot(
                    review = updatedReview,
                    phase = WalletAbiResumePhase.REQUEST_LOADED
                )
            )
        )
    }

    private suspend fun handleResolveRequest() {
        val currentState = mutableState.value as? WalletAbiFlowState.RequestLoaded ?: return
        mutableOutputs.emit(
            WalletAbiFlowOutput.StartResolution(
                WalletAbiResolutionCommand(
                    requestContext = currentState.review.requestContext,
                    selectedAccountId = currentState.review.selectedAccountId
                )
            )
        )
    }

    private suspend fun handleApprove() {
        val currentState = mutableState.value as? WalletAbiFlowState.RequestLoaded ?: return
        when (val approvalTarget = currentState.review.approvalTarget) {
            is WalletAbiApprovalTarget.Jade -> handleApproveJade(currentState.review, approvalTarget)
            WalletAbiApprovalTarget.Software -> {
                mutableState.value = WalletAbiFlowState.Submitting(
                    requestContext = currentState.review.requestContext,
                    stage = WalletAbiSubmittingStage.PREPARING
                )
                mutableOutputs.emit(
                    WalletAbiFlowOutput.StartSubmission(
                        WalletAbiSubmissionCommand(
                            requestContext = currentState.review.requestContext,
                            selectedAccountId = currentState.review.selectedAccountId
                        )
                    )
                )
            }
        }
    }

    private suspend fun handleApproveJade(
        review: WalletAbiFlowReview,
        approvalTarget: WalletAbiApprovalTarget.Jade
    ) {
        val jade = WalletAbiJadeContext(
            deviceId = approvalTarget.deviceId,
            step = WalletAbiJadeStep.CONNECT,
            message = null,
            retryable = false
        )
        mutableState.value = WalletAbiFlowState.AwaitingApproval(
            requestContext = review.requestContext,
            selectedAccountId = review.selectedAccountId,
            jade = jade
        )
        mutableOutputs.emit(
            WalletAbiFlowOutput.PersistSnapshot(
                WalletAbiResumeSnapshot(
                    review = review,
                    phase = WalletAbiResumePhase.AWAITING_APPROVAL,
                    jade = jade
                )
            )
        )
        mutableOutputs.emit(
            WalletAbiFlowOutput.StartApproval(
                WalletAbiApprovalCommand(
                    requestContext = review.requestContext,
                    selectedAccountId = review.selectedAccountId,
                    jade = jade
                )
            )
        )
    }

    private suspend fun handleCancel() {
        when (val currentState = mutableState.value) {
            is WalletAbiFlowState.Loading -> {
                if (currentState.isCancelling) return
                mutableState.value = currentState.copy(isCancelling = true)
                mutableOutputs.emit(WalletAbiFlowOutput.CancelActiveWork(WalletAbiFlowPhase.LOADING))
            }

            is WalletAbiFlowState.AwaitingApproval -> {
                if (currentState.isCancelling) return
                mutableState.value = currentState.copy(isCancelling = true)
                mutableOutputs.emit(WalletAbiFlowOutput.CancelActiveWork(WalletAbiFlowPhase.APPROVAL))
            }

            is WalletAbiFlowState.Submitting -> {
                if (currentState.isCancelling || currentState.stage == WalletAbiSubmittingStage.BROADCASTING) {
                    return
                }
                mutableState.value = currentState.copy(isCancelling = true)
                mutableOutputs.emit(WalletAbiFlowOutput.CancelActiveWork(WalletAbiFlowPhase.SUBMISSION))
            }

            else -> Unit
        }
    }

    private suspend fun handleReject() {
        emitCancelled(WalletAbiCancelledReason.UserRejected)
    }

    private suspend fun handleExecutionExpired() {
        emitCancelled(WalletAbiCancelledReason.RequestExpired)
    }

    private suspend fun handleExecutionFailed(event: WalletAbiExecutionEvent.Failed) {
        emitError(event.error)
    }

    private suspend fun handleRestore(intent: WalletAbiFlowIntent.Restore) {
        lastRequestContext = intent.snapshot.review.requestContext
        when (intent.snapshot.phase) {
            WalletAbiResumePhase.SUBMITTING -> {
                mutableState.value = restoreSubmittingAsError()
                mutableOutputs.emit(WalletAbiFlowOutput.PersistSnapshot(null))
            }

            else -> {
                mutableOutputs.emit(
                    WalletAbiFlowOutput.PersistSnapshot(intent.snapshot)
                )
                mutableState.value = WalletAbiFlowState.Resumable(intent.snapshot)
            }
        }
    }

    private fun handleResume() {
        val currentState = mutableState.value as? WalletAbiFlowState.Resumable ?: return
        val snapshot = currentState.snapshot
        mutableState.value = when (snapshot.phase) {
            WalletAbiResumePhase.AWAITING_APPROVAL -> WalletAbiFlowState.AwaitingApproval(
                requestContext = snapshot.review.requestContext,
                selectedAccountId = snapshot.review.selectedAccountId,
                jade = snapshot.jade ?: WalletAbiJadeContext(
                    deviceId = null,
                    step = WalletAbiJadeStep.CONNECT,
                    message = null,
                    retryable = false
                )
            )

            WalletAbiResumePhase.REQUEST_LOADED -> WalletAbiFlowState.RequestLoaded(snapshot.review)
            WalletAbiResumePhase.SUBMITTING -> WalletAbiFlowState.Submitting(
                requestContext = snapshot.review.requestContext,
                stage = WalletAbiSubmittingStage.PREPARING,
                jade = snapshot.jade
            )
        }
    }

    private suspend fun handleRetry() {
        val currentState = mutableState.value as? WalletAbiFlowState.Error ?: return
        if (!currentState.error.retryable) return
        val requestContext = lastRequestContext ?: return
        mutableState.value = WalletAbiFlowState.Loading(requestContext)
        mutableOutputs.emit(
            WalletAbiFlowOutput.LoadRequest(requestContext)
        )
    }

    private suspend fun handleCancelResume() {
        emitCancelled(WalletAbiCancelledReason.ResumableCancelled)
    }

    private suspend fun handleDismissTerminal() {
        when (mutableState.value) {
            is WalletAbiFlowState.Cancelled,
            is WalletAbiFlowState.Error,
            is WalletAbiFlowState.Success -> {
                mutableState.value = WalletAbiFlowState.Idle
            }

            else -> Unit
        }
    }

    private fun restoreSubmittingAsError(): WalletAbiFlowState.Error {
        return WalletAbiFlowState.Error(
            WalletAbiFlowError(
                kind = WalletAbiFlowErrorKind.PARTIAL_COMPLETION,
                phase = WalletAbiFlowPhase.SUBMISSION,
                message = "Transaction status may already have changed. Check your wallet activity before retrying.",
                retryable = false
            )
        )
    }

    private suspend fun handleJadeEvent(event: WalletAbiJadeEvent) {
        val currentState = mutableState.value as? WalletAbiFlowState.AwaitingApproval ?: return
        when (event) {
            WalletAbiJadeEvent.Cancelled -> {
                emitCancelled(WalletAbiCancelledReason.JadeCancelled)
            }

            WalletAbiJadeEvent.Connected -> {
                mutableState.value = currentState.copy(
                    jade = currentState.jade.copy(step = WalletAbiJadeStep.UNLOCK),
                    isCancelling = false
                )
            }

            WalletAbiJadeEvent.Disconnected -> {
                emitError(
                    WalletAbiFlowError(
                        kind = WalletAbiFlowErrorKind.DEVICE_FAILURE,
                        phase = WalletAbiFlowPhase.APPROVAL,
                        message = "Jade disconnected",
                        retryable = true
                    )
                )
            }

            is WalletAbiJadeEvent.Failed -> {
                emitError(event.error)
            }

            WalletAbiJadeEvent.ReviewConfirmed -> {
                mutableState.value = currentState.copy(
                    jade = currentState.jade.copy(step = WalletAbiJadeStep.SIGN),
                    isCancelling = false
                )
            }

            WalletAbiJadeEvent.Signed -> {
                val jade = currentState.jade.copy(step = WalletAbiJadeStep.SIGN)
                mutableState.value = WalletAbiFlowState.Submitting(
                    requestContext = currentState.requestContext,
                    stage = WalletAbiSubmittingStage.PREPARING,
                    jade = jade
                )
                mutableOutputs.emit(
                    WalletAbiFlowOutput.StartSubmission(
                        WalletAbiSubmissionCommand(
                            requestContext = currentState.requestContext,
                            selectedAccountId = currentState.selectedAccountId
                        )
                    )
                )
            }

            WalletAbiJadeEvent.UnlockConfirmed -> {
                mutableState.value = currentState.copy(
                    jade = currentState.jade.copy(step = WalletAbiJadeStep.REVIEW),
                    isCancelling = false
                )
            }
        }
    }

    private suspend fun emitCancelled(reason: WalletAbiCancelledReason) {
        val result = WalletAbiFlowTerminalResult.Cancelled(reason)
        mutableState.value = WalletAbiFlowState.Cancelled(result.reason)
        mutableOutputs.emit(WalletAbiFlowOutput.PersistSnapshot(null))
        mutableOutputs.emit(WalletAbiFlowOutput.Complete(result))
    }

    private suspend fun emitError(error: WalletAbiFlowError) {
        val result = WalletAbiFlowTerminalResult.Error(error)
        mutableState.value = WalletAbiFlowState.Error(error)
        mutableOutputs.emit(WalletAbiFlowOutput.PersistSnapshot(null))
        mutableOutputs.emit(WalletAbiFlowOutput.Complete(result))
    }
}

data class WalletAbiStartRequestContext(
    val requestId: String,
    val walletId: String
)

sealed interface WalletAbiFlowState {
    data object Idle : WalletAbiFlowState

    data class Loading(
        val requestContext: WalletAbiStartRequestContext,
        val isCancelling: Boolean = false
    ) : WalletAbiFlowState

    data class RequestLoaded(
        val review: WalletAbiFlowReview
    ) : WalletAbiFlowState

    data class AwaitingApproval(
        val requestContext: WalletAbiStartRequestContext,
        val selectedAccountId: String?,
        val jade: WalletAbiJadeContext,
        val isCancelling: Boolean = false
    ) : WalletAbiFlowState

    data class Submitting(
        val requestContext: WalletAbiStartRequestContext,
        val stage: WalletAbiSubmittingStage,
        val jade: WalletAbiJadeContext? = null,
        val isCancelling: Boolean = false
    ) : WalletAbiFlowState

    data class Success(
        val result: WalletAbiSuccessResult
    ) : WalletAbiFlowState

    data class Cancelled(
        val reason: WalletAbiCancelledReason
    ) : WalletAbiFlowState

    data class Error(
        val error: WalletAbiFlowError
    ) : WalletAbiFlowState

    data class Resumable(
        val snapshot: WalletAbiResumeSnapshot
    ) : WalletAbiFlowState
}

sealed interface WalletAbiFlowIntent {
    data object Approve : WalletAbiFlowIntent
    data object Cancel : WalletAbiFlowIntent
    data object CancelResume : WalletAbiFlowIntent
    data object DismissTerminal : WalletAbiFlowIntent
    data object Reject : WalletAbiFlowIntent
    data object Retry : WalletAbiFlowIntent
    data object Resume : WalletAbiFlowIntent

    data class OnJadeEvent(
        val event: WalletAbiJadeEvent
    ) : WalletAbiFlowIntent

    data class Restore(
        val snapshot: WalletAbiResumeSnapshot
    ) : WalletAbiFlowIntent

    data class Start(
        val requestContext: WalletAbiStartRequestContext
    ) : WalletAbiFlowIntent

    data class SelectAccount(
        val accountId: String
    ) : WalletAbiFlowIntent

    data object ResolveRequest : WalletAbiFlowIntent

    data class OnExecutionEvent(
        val event: WalletAbiExecutionEvent
    ) : WalletAbiFlowIntent
}

sealed interface WalletAbiFlowOutput {
    data class PersistSnapshot(
        val snapshot: WalletAbiResumeSnapshot?
    ) : WalletAbiFlowOutput

    data class LoadRequest(
        val requestContext: WalletAbiStartRequestContext
    ) : WalletAbiFlowOutput

    data class CancelActiveWork(
        val phase: WalletAbiFlowPhase
    ) : WalletAbiFlowOutput

    data class StartResolution(
        val command: WalletAbiResolutionCommand
    ) : WalletAbiFlowOutput

    data class StartApproval(
        val command: WalletAbiApprovalCommand
    ) : WalletAbiFlowOutput

    data class StartSubmission(
        val command: WalletAbiSubmissionCommand
    ) : WalletAbiFlowOutput

    data class Complete(
        val result: WalletAbiFlowTerminalResult
    ) : WalletAbiFlowOutput
}

data class WalletAbiResumeSnapshot(
    val review: WalletAbiFlowReview,
    val phase: WalletAbiResumePhase,
    val jade: WalletAbiJadeContext? = null
)

enum class WalletAbiResumePhase {
    AWAITING_APPROVAL,
    REQUEST_LOADED,
    SUBMITTING
}

enum class WalletAbiFlowPhase {
    LOADING,
    APPROVAL,
    SUBMISSION
}

enum class WalletAbiFlowErrorKind {
    INVALID_REQUEST,
    UNSUPPORTED_REQUEST,
    DEVICE_FAILURE,
    NETWORK_FAILURE,
    TIMEOUT,
    PARTIAL_COMPLETION,
    EXECUTION_FAILURE
}

enum class WalletAbiSubmittingStage {
    PREPARING,
    BROADCASTING
}

data class WalletAbiResolutionCommand(
    val requestContext: WalletAbiStartRequestContext,
    val selectedAccountId: String?
)

data class WalletAbiSubmissionCommand(
    val requestContext: WalletAbiStartRequestContext,
    val selectedAccountId: String?
)

data class WalletAbiApprovalCommand(
    val requestContext: WalletAbiStartRequestContext,
    val selectedAccountId: String?,
    val jade: WalletAbiJadeContext
)

sealed interface WalletAbiFlowTerminalResult {
    data class Success(
        val result: WalletAbiSuccessResult
    ) : WalletAbiFlowTerminalResult

    data class Cancelled(
        val reason: WalletAbiCancelledReason
    ) : WalletAbiFlowTerminalResult

    data class Error(
        val error: WalletAbiFlowError
    ) : WalletAbiFlowTerminalResult
}

data class WalletAbiSuccessResult(
    val requestId: String,
    val txHash: String? = null,
    val responseId: String? = null
)

enum class WalletAbiCancelledReason {
    JadeCancelled,
    RequestExpired,
    ResumableCancelled,
    UserCancelled,
    UserRejected
}

data class WalletAbiFlowError(
    val kind: WalletAbiFlowErrorKind,
    val phase: WalletAbiFlowPhase,
    val message: String,
    val retryable: Boolean
)

data class WalletAbiFlowReview(
    val requestContext: WalletAbiStartRequestContext,
    val title: String,
    val message: String,
    val accounts: List<WalletAbiAccountOption>,
    val selectedAccountId: String?,
    val approvalTarget: WalletAbiApprovalTarget,
    val parsedRequest: WalletAbiParsedRequest? = null,
    val executionDetails: WalletAbiExecutionDetails? = null
)

data class WalletAbiExecutionDetails(
    val destinationAddress: String,
    val amountSat: Long,
    val assetId: String,
    val network: String,
    val feeRate: Long? = null
)

data class WalletAbiAccountOption(
    val accountId: String,
    val name: String
)

sealed interface WalletAbiApprovalTarget {
    data class Jade(
        val deviceName: String?,
        val deviceId: String?
    ) : WalletAbiApprovalTarget

    data object Software : WalletAbiApprovalTarget
}

data class WalletAbiJadeContext(
    val deviceId: String?,
    val step: WalletAbiJadeStep,
    val message: String?,
    val retryable: Boolean
)

enum class WalletAbiJadeStep {
    CONNECT,
    UNLOCK,
    REVIEW,
    SIGN
}

sealed interface WalletAbiJadeEvent {
    data object Cancelled : WalletAbiJadeEvent
    data object Connected : WalletAbiJadeEvent
    data object Disconnected : WalletAbiJadeEvent
    data class Failed(
        val error: WalletAbiFlowError
    ) : WalletAbiJadeEvent
    data object ReviewConfirmed : WalletAbiJadeEvent
    data object Signed : WalletAbiJadeEvent
    data object UnlockConfirmed : WalletAbiJadeEvent
}

sealed interface WalletAbiExecutionEvent {
    data object Broadcasted : WalletAbiExecutionEvent

    data class Cancelled(
        val reason: WalletAbiCancelledReason
    ) : WalletAbiExecutionEvent

    data object Expired : WalletAbiExecutionEvent

    data class Failed(
        val error: WalletAbiFlowError
    ) : WalletAbiExecutionEvent

    data class RequestLoaded(
        val review: WalletAbiFlowReview
    ) : WalletAbiExecutionEvent

    data class Resolved(
        val review: WalletAbiFlowReview
    ) : WalletAbiExecutionEvent

    data class RemoteResponseSent(
        val result: WalletAbiSuccessResult
    ) : WalletAbiExecutionEvent

    data object Submitted : WalletAbiExecutionEvent
}
