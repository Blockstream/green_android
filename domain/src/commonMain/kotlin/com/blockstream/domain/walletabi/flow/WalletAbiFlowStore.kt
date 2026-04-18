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
        }
    }

    private fun handleStart(intent: WalletAbiFlowIntent.Start) {
        mutableState.value = WalletAbiFlowState.Loading(intent.requestContext)
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
}

sealed interface WalletAbiFlowIntent {
    data class Start(
        val requestContext: WalletAbiStartRequestContext
    ) : WalletAbiFlowIntent
}

sealed interface WalletAbiFlowOutput
