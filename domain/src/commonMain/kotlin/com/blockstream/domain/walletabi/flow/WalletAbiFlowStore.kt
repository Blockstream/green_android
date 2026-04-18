package com.blockstream.domain.walletabi.flow

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
    private val mutableOutputs = MutableSharedFlow<WalletAbiFlowOutput>()

    override val state: StateFlow<WalletAbiFlowState> = mutableState.asStateFlow()
    override val outputs: Flow<WalletAbiFlowOutput> = mutableOutputs.asSharedFlow()

    override suspend fun dispatch(intent: WalletAbiFlowIntent) {
        when (intent) {
            WalletAbiFlowIntent.Approve -> handleApprove()
            is WalletAbiFlowIntent.Start -> handleStart(intent)
            is WalletAbiFlowIntent.ResolveRequest -> handleResolveRequest()
            is WalletAbiFlowIntent.SelectAccount -> handleSelectAccount(intent)
            is WalletAbiFlowIntent.OnExecutionEvent -> handleExecutionEvent(intent.event)
        }
    }

    private fun handleStart(intent: WalletAbiFlowIntent.Start) {
        mutableState.value = WalletAbiFlowState.Loading(intent.requestContext)
    }

    private fun handleExecutionEvent(event: WalletAbiExecutionEvent) {
        when (event) {
            is WalletAbiExecutionEvent.RequestLoaded -> handleExecutionRequestLoaded(event)
            is WalletAbiExecutionEvent.Resolved -> handleExecutionResolved(event)
        }
    }

    private fun handleExecutionRequestLoaded(event: WalletAbiExecutionEvent.RequestLoaded) {
        mutableState.value = WalletAbiFlowState.RequestLoaded(event.review)
    }

    private fun handleExecutionResolved(event: WalletAbiExecutionEvent.Resolved) {
        mutableState.value = WalletAbiFlowState.RequestLoaded(event.review)
    }

    private suspend fun handleSelectAccount(intent: WalletAbiFlowIntent.SelectAccount) {
        val currentState = mutableState.value as? WalletAbiFlowState.RequestLoaded ?: return
        val updatedReview = currentState.review.copy(selectedAccountId = intent.accountId)
        mutableState.value = WalletAbiFlowState.RequestLoaded(updatedReview)
        mutableOutputs.emit(
            WalletAbiFlowOutput.PersistSnapshot(
                WalletAbiResumeSnapshot(
                    requestContext = updatedReview.requestContext,
                    selectedAccountId = updatedReview.selectedAccountId
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
        mutableState.value = WalletAbiFlowState.Submitting(currentState.review.requestContext)
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

data class WalletAbiStartRequestContext(
    val requestId: String,
    val walletId: String
)

sealed interface WalletAbiFlowState {
    data object Idle : WalletAbiFlowState
    data class Loading(
        val requestContext: WalletAbiStartRequestContext
    ) : WalletAbiFlowState

    data class RequestLoaded(
        val review: WalletAbiFlowReview
    ) : WalletAbiFlowState

    data class Submitting(
        val requestContext: WalletAbiStartRequestContext
    ) : WalletAbiFlowState
}

sealed interface WalletAbiFlowIntent {
    data object Approve : WalletAbiFlowIntent

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
        val snapshot: WalletAbiResumeSnapshot
    ) : WalletAbiFlowOutput

    data class StartResolution(
        val command: WalletAbiResolutionCommand
    ) : WalletAbiFlowOutput

    data class StartSubmission(
        val command: WalletAbiSubmissionCommand
    ) : WalletAbiFlowOutput
}

data class WalletAbiResumeSnapshot(
    val requestContext: WalletAbiStartRequestContext,
    val selectedAccountId: String?
)

data class WalletAbiResolutionCommand(
    val requestContext: WalletAbiStartRequestContext,
    val selectedAccountId: String?
)

data class WalletAbiSubmissionCommand(
    val requestContext: WalletAbiStartRequestContext,
    val selectedAccountId: String?
)

data class WalletAbiFlowReview(
    val requestContext: WalletAbiStartRequestContext,
    val title: String,
    val message: String,
    val accounts: List<WalletAbiAccountOption>,
    val selectedAccountId: String?,
    val approvalTarget: WalletAbiApprovalTarget
)

data class WalletAbiAccountOption(
    val accountId: String,
    val name: String
)

sealed interface WalletAbiApprovalTarget {
    data object Software : WalletAbiApprovalTarget
}

sealed interface WalletAbiExecutionEvent {
    data class RequestLoaded(
        val review: WalletAbiFlowReview
    ) : WalletAbiExecutionEvent

    data class Resolved(
        val review: WalletAbiFlowReview
    ) : WalletAbiExecutionEvent
}
