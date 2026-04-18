package com.blockstream.green.walletabi

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.models.walletabi.WalletAbiFlowRouteViewModel
import com.blockstream.compose.screens.overview.WalletAbiDevelopmentEntry
import com.blockstream.compose.screens.walletabi.WalletAbiFlowScreen
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.database.Database
import com.blockstream.data.walletabi.flow.FakeWalletAbiFlowDriver
import com.blockstream.domain.walletabi.flow.WalletAbiFlowSnapshotRepository
import com.blockstream.domain.walletabi.flow.WalletAbiFlowStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collectLatest
import org.junit.Rule
import org.junit.Test
import org.koin.core.Koin
import org.koin.core.context.GlobalContext

class WalletAbiHappyPathTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<WalletAbiComposeTestActivity>()

    @Test
    fun walletAbiHappyPath_completes_and_returns_to_entry() {
        val koin = GlobalContext.get()
        val greenWallet = insertPreviewWallet(koin = koin)
        setWalletAbiHappyPathContent(
            koin = koin,
            greenWallet = greenWallet
        )

        completeSuccessfulApproval()
    }

    @Test
    fun walletAbiHappyPath_completes_twice_and_returns_to_entry() {
        val koin = GlobalContext.get()
        val greenWallet = insertPreviewWallet(koin = koin)
        setWalletAbiHappyPathContent(
            koin = koin,
            greenWallet = greenWallet
        )

        repeat(2) {
            completeSuccessfulApproval()
        }
    }

    private fun setWalletAbiHappyPathContent(
        koin: Koin,
        greenWallet: GreenWallet
    ) {
        fun createViewModel(): WalletAbiFlowRouteViewModel {
            return WalletAbiFlowRouteViewModel(
                greenWallet = greenWallet,
                store = koin.get<WalletAbiFlowStore>(),
                snapshotRepository = koin.get<WalletAbiFlowSnapshotRepository>(),
                driver = koin.get<FakeWalletAbiFlowDriver>()
            )
        }

        composeRule.setContent {
            GreenPreview {
                var isFlowVisible by remember { mutableStateOf(false) }
                var flowLaunchCount by remember { mutableStateOf(0) }

                if (isFlowVisible) {
                    val viewModel = remember(flowLaunchCount) {
                        createViewModel()
                    }
                    LaunchedEffect(viewModel) {
                        viewModel.sideEffect.collectLatest { sideEffect ->
                            if (sideEffect == SideEffects.NavigateBack()) {
                                isFlowVisible = false
                            }
                        }
                    }
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    WalletAbiFlowScreen(
                        state = state,
                        onIntent = viewModel::dispatch
                    )
                } else {
                    WalletAbiDevelopmentEntry(
                        visible = true,
                        onOpen = {
                            flowLaunchCount += 1
                            isFlowVisible = true
                        }
                    )
                }
            }
        }
    }

    private fun completeSuccessfulApproval() {
        composeRule.onNodeWithTag("transact_wallet_abi_entry").assertIsDisplayed()
        composeRule.onNodeWithTag("transact_wallet_abi_entry").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wallet_abi_flow_request_title").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wallet_abi_flow_request_title").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_approve_action").performClick()
        composeRule.onNodeWithTag("wallet_abi_flow_submitting").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wallet_abi_flow_success").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wallet_abi_flow_success").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_terminal_dismiss_action").performClick()
        composeRule.onNodeWithTag("transact_wallet_abi_entry").assertIsDisplayed()
    }

    private fun insertPreviewWallet(koin: Koin): GreenWallet {
        val greenWallet = previewWallet()
        runBlocking {
            koin.get<Database>().insertWallet(greenWallet)
        }
        return greenWallet
    }
}
