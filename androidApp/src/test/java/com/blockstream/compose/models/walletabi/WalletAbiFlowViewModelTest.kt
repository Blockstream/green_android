package com.blockstream.compose.models.walletabi

import com.blockstream.domain.walletabi.flow.WalletAbiFlowIntent
import com.blockstream.domain.walletabi.flow.WalletAbiFlowOutput
import com.blockstream.domain.walletabi.flow.WalletAbiFlowState
import com.blockstream.domain.walletabi.flow.WalletAbiFlowStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class WalletAbiFlowViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun state_exposes_store_state() {
        val store = FakeWalletAbiFlowStore()
        val viewModel = WalletAbiFlowViewModel(store)

        assertEquals(
            WalletAbiFlowState.Idle,
            viewModel.state.value
        )
    }

    @Test
    fun dispatch_forwards_intent_to_store() = runTest(dispatcher) {
        val store = FakeWalletAbiFlowStore()
        val viewModel = WalletAbiFlowViewModel(store)
        val intent = WalletAbiFlowIntent.Reject

        viewModel.dispatch(intent)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf<WalletAbiFlowIntent>(intent),
            store.intents
        )
    }

    private class FakeWalletAbiFlowStore : WalletAbiFlowStore {
        override val state = MutableStateFlow<WalletAbiFlowState>(WalletAbiFlowState.Idle)
        override val outputs: Flow<WalletAbiFlowOutput> = emptyFlow<WalletAbiFlowOutput>()
        val intents = mutableListOf<WalletAbiFlowIntent>()

        override suspend fun dispatch(intent: WalletAbiFlowIntent) {
            intents += intent
        }
    }
}
