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
            is WalletAbiFlowIntent.Start -> handleStart(intent)
            is WalletAbiFlowIntent.OnExecutionEvent -> handleExecutionEvent(intent.event)
        }
    }

    private fun handleStart(intent: WalletAbiFlowIntent.Start) {
        mutableState.value = WalletAbiFlowState.Loading(intent.requestContext)
    }

    private fun handleExecutionEvent(event: WalletAbiExecutionEvent) {
        when (event) {
            is WalletAbiExecutionEvent.RequestLoaded -> handleExecutionRequestLoaded(event)
        }
    }

    private fun handleExecutionRequestLoaded(event: WalletAbiExecutionEvent.RequestLoaded) {
        mutableState.value = WalletAbiFlowState.RequestLoaded(event.review)
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
}

sealed interface WalletAbiFlowIntent {
    data class Start(
        val requestContext: WalletAbiStartRequestContext
    ) : WalletAbiFlowIntent

    data class OnExecutionEvent(
        val event: WalletAbiExecutionEvent
    ) : WalletAbiFlowIntent
}

sealed interface WalletAbiFlowOutput

data class WalletAbiFlowReview(
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
}
