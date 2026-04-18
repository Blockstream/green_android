package com.blockstream.green.walletabi

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
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
import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.database.Database
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.managers.SessionManager
import com.blockstream.data.walletabi.flow.FakeWalletAbiFlowDriver
import com.blockstream.data.walletabi.request.DefaultWalletAbiDemoRequestSource
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionPlan
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionPlanner
import com.blockstream.domain.walletabi.flow.WalletAbiFlowSnapshotRepository
import com.blockstream.domain.walletabi.flow.WalletAbiFlowStore
import com.blockstream.domain.walletabi.request.WalletAbiParsedRequest
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
            greenWallet = greenWallet,
            walletSession = walletSession(koin = koin, greenWallet = greenWallet)
        )

        completeSuccessfulApproval()
    }

    @Test
    fun walletAbiHappyPath_completes_twice_and_returns_to_entry() {
        val koin = GlobalContext.get()
        val greenWallet = insertPreviewWallet(koin = koin)
        setWalletAbiHappyPathContent(
            koin = koin,
            greenWallet = greenWallet,
            walletSession = walletSession(koin = koin, greenWallet = greenWallet)
        )

        repeat(2) {
            completeSuccessfulApproval()
        }
    }

    @Test
    fun walletAbiFlow_shows_error_for_unsupported_request() {
        val koin = GlobalContext.get()
        val greenWallet = insertPreviewWallet(koin = koin)
        val requestSource = DefaultWalletAbiDemoRequestSource { _ ->
            """
                {
                  "jsonrpc": "2.0",
                  "id": "wallet-abi-demo-envelope",
                  "method": "wallet_abi_get_account"
                }
            """.trimIndent()
        }

        composeRule.setContent {
            GreenPreview {
                var isFlowVisible by remember { mutableStateOf(false) }

                if (isFlowVisible) {
                    val viewModel = remember {
                        WalletAbiFlowRouteViewModel(
                            greenWallet = greenWallet,
                            store = koin.get<WalletAbiFlowStore>(),
                            snapshotRepository = koin.get<WalletAbiFlowSnapshotRepository>(),
                            walletSession = walletSession(koin = koin, greenWallet = greenWallet),
                            requestSource = requestSource,
                            executionPlanner = executionPlanner(),
                            driver = koin.get<FakeWalletAbiFlowDriver>()
                        )
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
                        onOpen = { isFlowVisible = true }
                    )
                }
            }
        }

        composeRule.onNodeWithTag("transact_wallet_abi_entry").assertIsDisplayed()
        composeRule.onNodeWithTag("transact_wallet_abi_entry").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wallet_abi_flow_error").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wallet_abi_flow_error")
            .assertIsDisplayed()
            .assertTextContains("Unsupported Wallet ABI method 'wallet_abi_get_account'")
    }

    @Test
    fun walletAbiFlow_shows_error_for_malformed_request() {
        val koin = GlobalContext.get()
        val greenWallet = insertPreviewWallet(koin = koin)
        val requestSource = DefaultWalletAbiDemoRequestSource { _ -> "{" }

        composeRule.setContent {
            GreenPreview {
                var isFlowVisible by remember { mutableStateOf(false) }

                if (isFlowVisible) {
                    val viewModel = remember {
                        WalletAbiFlowRouteViewModel(
                            greenWallet = greenWallet,
                            store = koin.get<WalletAbiFlowStore>(),
                            snapshotRepository = koin.get<WalletAbiFlowSnapshotRepository>(),
                            walletSession = walletSession(koin = koin, greenWallet = greenWallet),
                            requestSource = requestSource,
                            executionPlanner = executionPlanner(),
                            driver = koin.get<FakeWalletAbiFlowDriver>()
                        )
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
                        onOpen = { isFlowVisible = true }
                    )
                }
            }
        }

        composeRule.onNodeWithTag("transact_wallet_abi_entry").assertIsDisplayed()
        composeRule.onNodeWithTag("transact_wallet_abi_entry").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wallet_abi_flow_error").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wallet_abi_flow_error")
            .assertIsDisplayed()
            .assertTextContains("Wallet ABI request envelope is malformed")
    }

    @Test
    fun walletAbiFlow_retry_reloads_request_after_error() {
        val koin = GlobalContext.get()
        val greenWallet = insertPreviewWallet(koin = koin)
        val fallbackSource = DefaultWalletAbiDemoRequestSource()
        var loadAttempt = 0
        val requestSource = DefaultWalletAbiDemoRequestSource { requestId ->
            if (loadAttempt++ == 0) {
                "{"
            } else {
                fallbackSource.loadRequestEnvelope(requestId)
            }
        }

        composeRule.setContent {
            GreenPreview {
                var isFlowVisible by remember { mutableStateOf(false) }

                if (isFlowVisible) {
                    val viewModel = remember {
                        WalletAbiFlowRouteViewModel(
                            greenWallet = greenWallet,
                            store = koin.get<WalletAbiFlowStore>(),
                            snapshotRepository = koin.get<WalletAbiFlowSnapshotRepository>(),
                            walletSession = walletSession(koin = koin, greenWallet = greenWallet),
                            requestSource = requestSource,
                            executionPlanner = executionPlanner(),
                            driver = koin.get<FakeWalletAbiFlowDriver>()
                        )
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
                        onOpen = { isFlowVisible = true }
                    )
                }
            }
        }

        composeRule.onNodeWithTag("transact_wallet_abi_entry").assertIsDisplayed()
        composeRule.onNodeWithTag("transact_wallet_abi_entry").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wallet_abi_flow_error").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wallet_abi_flow_error")
            .assertIsDisplayed()
            .assertTextContains("Wallet ABI request envelope is malformed")
        composeRule.onNodeWithTag("wallet_abi_flow_retry_action").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wallet_abi_flow_request_title").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wallet_abi_flow_request_title").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_parsed_request_id").assertIsDisplayed()
    }

    private fun setWalletAbiHappyPathContent(
        koin: Koin,
        greenWallet: GreenWallet,
        walletSession: GdkSession
    ) {
        fun createViewModel(): WalletAbiFlowRouteViewModel {
            return WalletAbiFlowRouteViewModel(
                greenWallet = greenWallet,
                store = koin.get<WalletAbiFlowStore>(),
                snapshotRepository = koin.get<WalletAbiFlowSnapshotRepository>(),
                walletSession = walletSession,
                requestSource = DefaultWalletAbiDemoRequestSource(),
                executionPlanner = executionPlanner(),
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

    private fun walletSession(koin: Koin, greenWallet: GreenWallet): GdkSession {
        return koin.get<SessionManager>().getWalletSessionOrCreate(greenWallet)
    }

    private fun executionPlanner(): WalletAbiExecutionPlanner {
        return object : WalletAbiExecutionPlanner {
            override suspend fun plan(
                session: GdkSession,
                request: WalletAbiParsedRequest,
                selectedAccountId: String?
            ): WalletAbiExecutionPlan {
                val account = liquidAccount()
                val txCreate = request as WalletAbiParsedRequest.TxCreate
                return WalletAbiExecutionPlan(
                    request = txCreate,
                    accounts = listOf(account),
                    selectedAccount = account.takeIf { selectedAccountId == null || selectedAccountId == account.id } ?: account,
                    destinationAddress = "tlq1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3l4q9m",
                    amountSat = txCreate.request.params.outputs.single().amountSat,
                    assetId = TESTNET_POLICY_ASSET,
                    feeRate = 12_000L
                )
            }
        }
    }

    private fun liquidAccount(): Account {
        return Account(
            networkInjected = Network(
                network = "testnet-liquid",
                name = "Liquid Testnet",
                isMainnet = false,
                isLiquid = true,
                isDevelopment = false,
                policyAsset = TESTNET_POLICY_ASSET
            ),
            policyAsset = EnrichedAsset.PreviewLBTC,
            gdkName = "Liquid account",
            pointer = 0L,
            type = AccountType.BIP84_SEGWIT
        )
    }

    private companion object {
        const val TESTNET_POLICY_ASSET =
            "6f0279e9ed041c3d710a9f57d0c02928416460c4b722ae3457a11eec381c526d"
    }
}
