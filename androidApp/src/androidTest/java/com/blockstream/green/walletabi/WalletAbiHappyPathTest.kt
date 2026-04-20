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
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.navigation.WalletAbiFlowLaunchMode
import com.blockstream.compose.models.walletabi.WalletAbiFlowRouteViewModel
import com.blockstream.compose.screens.overview.WalletAbiDevelopmentEntry
import com.blockstream.compose.screens.overview.WalletAbiPendingResumeEntry
import com.blockstream.compose.screens.overview.WalletAbiTransactEntry
import com.blockstream.compose.screens.walletabi.WalletAbiFlowScreen
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.database.Database
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.PreparedSoftwareTransaction
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.Credentials
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.data.Output
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import com.blockstream.data.gdk.data.UtxoView
import com.blockstream.data.gdk.params.BroadcastTransactionParams
import com.blockstream.data.gdk.params.AddressParams
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.gdk.params.toJsonElement
import com.blockstream.data.managers.SessionManager
import com.blockstream.data.transaction.TransactionConfirmation
import com.blockstream.data.walletabi.request.DefaultWalletAbiDemoRequestSource
import com.blockstream.domain.walletabi.execution.WalletAbiPreparedExecution
import com.blockstream.domain.walletabi.execution.WalletAbiPreparedBroadcast
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionResult
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionRunner
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionPlan
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionPlanner
import com.blockstream.domain.walletabi.execution.WalletAbiReviewPreviewer
import com.blockstream.domain.walletabi.flow.WalletAbiAccountOption
import com.blockstream.domain.walletabi.flow.WalletAbiApprovalTarget
import com.blockstream.domain.walletabi.flow.WalletAbiFlowReview
import com.blockstream.domain.walletabi.flow.WalletAbiFlowSnapshotRepository
import com.blockstream.domain.walletabi.flow.WalletAbiResumePhase
import com.blockstream.domain.walletabi.flow.WalletAbiResumeSnapshot
import com.blockstream.domain.walletabi.flow.WalletAbiStartRequestContext
import com.blockstream.domain.walletabi.flow.WalletAbiFlowStore
import com.blockstream.domain.walletabi.flow.WalletAbiRequestFamily
import com.blockstream.domain.walletabi.flow.WalletAbiResolutionState
import com.blockstream.domain.walletabi.request.DefaultWalletAbiRequestParser
import com.blockstream.domain.walletabi.request.WalletAbiNetwork
import com.blockstream.domain.walletabi.request.WalletAbiParsedRequest
import com.blockstream.domain.walletabi.request.WalletAbiRequestParseResult
import com.blockstream.domain.walletabi.provider.WalletAbiExecutionContext
import com.blockstream.domain.walletabi.provider.WalletAbiProviderPreviewAssetDelta
import com.blockstream.domain.walletabi.provider.WalletAbiProviderPreviewOutput
import com.blockstream.domain.walletabi.provider.WalletAbiProviderPreviewOutputKind
import com.blockstream.domain.walletabi.provider.WalletAbiProviderProcessResponse
import com.blockstream.domain.walletabi.provider.WalletAbiProviderRequestPreview
import com.blockstream.domain.walletabi.provider.WalletAbiProviderRunResult
import com.blockstream.domain.walletabi.provider.WalletAbiProviderRunning
import com.blockstream.domain.walletabi.provider.WalletAbiProviderStatus
import com.blockstream.domain.walletabi.provider.WalletAbiProviderTransactionInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.JsonObject
import org.junit.Rule
import org.junit.Test
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import kotlin.test.assertEquals

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
    fun walletAbiIssuanceFlow_resolves_and_completes() {
        val koin = GlobalContext.get()
        val greenWallet = insertPreviewWallet(koin = koin)
        setWalletAbiProviderFlowContent(
            greenWallet = greenWallet,
            requestSource = DefaultWalletAbiDemoRequestSource { _ ->
                issuanceEnvelope(WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID)
            },
            providerRunner = fakeProviderRunner(
                requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                network = WalletAbiNetwork.TESTNET_LIQUID,
                preview = issuancePreview(),
                txHex = "issuance-provider-tx-hex",
                txid = "issuance-provider-txid",
            ),
            walletSession = mockProviderWalletSession(txHash = "issuance-provider-broadcast")
        )

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wallet_abi_flow_request_title").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wallet_abi_flow_request_title").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_resolution_status").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_resolve_action").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_resolve_action").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wallet_abi_flow_asset_impact_0").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(
            1,
            composeRule.onAllNodesWithTag("wallet_abi_flow_asset_impact_0").fetchSemanticsNodes().size
        )
        composeRule.onNodeWithTag("wallet_abi_flow_approve_action").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wallet_abi_flow_success").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wallet_abi_flow_success").assertIsDisplayed()
    }

    @Test
    fun walletAbiReissuanceFlow_resolves_and_completes() {
        val koin = GlobalContext.get()
        val greenWallet = insertPreviewWallet(koin = koin)
        setWalletAbiProviderFlowContent(
            greenWallet = greenWallet,
            requestSource = DefaultWalletAbiDemoRequestSource { _ ->
                reissuanceEnvelope(
                    requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                    tokenAssetId = "reissuance_token_asset",
                )
            },
            providerRunner = fakeProviderRunner(
                requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                network = WalletAbiNetwork.TESTNET_LIQUID,
                preview = reissuancePreview(),
                txHex = "reissuance-provider-tx-hex",
                txid = "reissuance-provider-txid",
            ),
            walletSession = mockProviderWalletSession(txHash = "reissuance-provider-broadcast")
        )

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wallet_abi_flow_request_title").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wallet_abi_flow_resolution_status").assertIsDisplayed()
        assertEquals(
            1,
            composeRule.onAllNodesWithTag("wallet_abi_flow_warning_0").fetchSemanticsNodes().size
        )
        composeRule.onNodeWithTag("wallet_abi_flow_resolve_action").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wallet_abi_flow_asset_impact_0").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(
            1,
            composeRule.onAllNodesWithTag("wallet_abi_flow_asset_impact_0").fetchSemanticsNodes().size
        )
        composeRule.onNodeWithTag("wallet_abi_flow_approve_action").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wallet_abi_flow_success").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wallet_abi_flow_success").assertIsDisplayed()
    }

    @Test
    fun walletAbiPendingResumeEntry_shows_resume_card_in_non_development_build() {
        val snapshot = demoResumeSnapshot(
            greenWallet = previewWallet(),
            phase = WalletAbiResumePhase.REQUEST_LOADED
        )

        composeRule.setContent {
            GreenPreview {
                WalletAbiTransactEntry(
                    isDevelopment = false,
                    pendingSnapshot = snapshot,
                    onOpenDemo = {},
                    onResumePending = {}
                )
            }
        }

        composeRule.onNodeWithTag("transact_wallet_abi_resume_entry").assertIsDisplayed()
        assertEquals(
            0,
            composeRule.onAllNodesWithTag("transact_wallet_abi_entry").fetchSemanticsNodes().size
        )
    }

    @Test
    fun walletAbiPendingResumeEntry_shows_attention_copy_for_submitting_snapshot() {
        val snapshot = demoResumeSnapshot(
            greenWallet = previewWallet(),
            phase = WalletAbiResumePhase.SUBMITTING
        )

        composeRule.setContent {
            GreenPreview {
                WalletAbiPendingResumeEntry(
                    snapshot = snapshot,
                    onResume = {}
                )
            }
        }

        composeRule.onNodeWithTag("transact_wallet_abi_resume_entry")
            .assertIsDisplayed()
            .assertTextContains("Wallet ABI request needs attention wallet-abi-demo-request")
    }

    @Test
    fun walletAbiPendingResumeEntry_opens_resumable_flow_and_clears_after_cancel_resume() {
        val koin = GlobalContext.get()
        val greenWallet = insertPreviewWallet(koin = koin)
        val snapshotRepository = koin.get<WalletAbiFlowSnapshotRepository>()
        val snapshot = demoResumeSnapshot(
            greenWallet = greenWallet,
            phase = WalletAbiResumePhase.REQUEST_LOADED
        )
        runBlocking {
            snapshotRepository.save(greenWallet.id, snapshot)
        }
        val walletSession = walletSession(koin = koin, greenWallet = greenWallet)

        composeRule.setContent {
            GreenPreview {
                var isFlowVisible by remember { mutableStateOf(false) }
                var pendingSnapshot by remember {
                    mutableStateOf(runBlocking { snapshotRepository.load(greenWallet.id) })
                }

                if (isFlowVisible) {
                    val viewModel = remember {
                        WalletAbiFlowRouteViewModel(
                            greenWallet = greenWallet,
                            launchMode = WalletAbiFlowLaunchMode.Resume,
                            store = koin.get<WalletAbiFlowStore>(),
                            snapshotRepository = snapshotRepository,
                            walletSession = walletSession,
                            requestSource = DefaultWalletAbiDemoRequestSource(),
                            executionPlanner = executionPlanner(),
                            executionRunner = executionRunner(),
                            reviewPreviewer = reviewPreviewer()
                        )
                    }
                    LaunchedEffect(viewModel) {
                        viewModel.sideEffect.collectLatest { sideEffect ->
                            if (sideEffect == SideEffects.NavigateBack()) {
                                pendingSnapshot = snapshotRepository.load(greenWallet.id)
                                isFlowVisible = false
                            }
                        }
                    }
                    WalletAbiFlowScreen(viewModel = viewModel)
                } else {
                    WalletAbiTransactEntry(
                        isDevelopment = true,
                        pendingSnapshot = pendingSnapshot,
                        onOpenDemo = {},
                        onResumePending = { isFlowVisible = true }
                    )
                }
            }
        }

        composeRule.onNodeWithTag("transact_wallet_abi_resume_entry").assertIsDisplayed()
        assertEquals(
            0,
            composeRule.onAllNodesWithTag("transact_wallet_abi_entry").fetchSemanticsNodes().size
        )

        composeRule.onNodeWithTag("transact_wallet_abi_resume_entry").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wallet_abi_flow_resumable").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wallet_abi_flow_cancel_resume_action").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wallet_abi_flow_cancelled").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wallet_abi_flow_terminal_dismiss_action").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("transact_wallet_abi_entry").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("transact_wallet_abi_entry").assertIsDisplayed()
        assertEquals(
            0,
            composeRule.onAllNodesWithTag("transact_wallet_abi_resume_entry").fetchSemanticsNodes().size
        )
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
                            executionRunner = executionRunner(),
                            reviewPreviewer = reviewPreviewer()
                        )
                    }
                    LaunchedEffect(viewModel) {
                        viewModel.sideEffect.collectLatest { sideEffect ->
                            if (sideEffect == SideEffects.NavigateBack()) {
                                isFlowVisible = false
                            }
                        }
                    }
                    WalletAbiFlowScreen(viewModel = viewModel)
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
        assertEquals(
            0,
            composeRule.onAllNodesWithTag("wallet_abi_flow_retry_action").fetchSemanticsNodes().size
        )
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
                            executionRunner = executionRunner(),
                            reviewPreviewer = reviewPreviewer()
                        )
                    }
                    LaunchedEffect(viewModel) {
                        viewModel.sideEffect.collectLatest { sideEffect ->
                            if (sideEffect == SideEffects.NavigateBack()) {
                                isFlowVisible = false
                            }
                        }
                    }
                    WalletAbiFlowScreen(viewModel = viewModel)
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
        assertEquals(
            0,
            composeRule.onAllNodesWithTag("wallet_abi_flow_retry_action").fetchSemanticsNodes().size
        )
    }

    @Test
    fun walletAbiFlow_retry_reloads_request_after_retryable_execution_error() {
        val koin = GlobalContext.get()
        val greenWallet = insertPreviewWallet(koin = koin)
        var executionAttempt = 0

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
                            requestSource = DefaultWalletAbiDemoRequestSource(),
                            executionPlanner = executionPlanner(),
                            executionRunner = object : WalletAbiExecutionRunner {
                                override suspend fun prepare(
                                    session: GdkSession,
                                    preparedExecution: WalletAbiPreparedExecution
                                ) = WalletAbiPreparedBroadcast(
                                    preparedExecution = preparedExecution,
                                    preparedTransaction = PreparedSoftwareTransaction(
                                        transaction = preparedExecution.transaction,
                                        signedTransaction = JsonObject(emptyMap())
                                    )
                                )

                                override suspend fun broadcast(
                                    session: GdkSession,
                                    preparedBroadcast: WalletAbiPreparedBroadcast,
                                    twoFactorResolver: com.blockstream.data.gdk.TwoFactorResolver
                                ): WalletAbiExecutionResult {
                                    return if (executionAttempt++ == 0) {
                                        error("send failed")
                                    } else {
                                        delay(200)
                                        WalletAbiExecutionResult(txHash = "wallet-abi-demo-tx-hash")
                                    }
                                }
                            },
                            reviewPreviewer = reviewPreviewer()
                        )
                    }
                    LaunchedEffect(viewModel) {
                        viewModel.sideEffect.collectLatest { sideEffect ->
                            if (sideEffect == SideEffects.NavigateBack()) {
                                isFlowVisible = false
                            }
                        }
                    }
                    WalletAbiFlowScreen(viewModel = viewModel)
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
            composeRule.onAllNodesWithTag("wallet_abi_flow_request_title").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wallet_abi_flow_approve_action").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wallet_abi_flow_error").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wallet_abi_flow_error")
            .assertIsDisplayed()
            .assertTextContains("send failed")
        composeRule.onNodeWithTag("wallet_abi_flow_retry_action").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wallet_abi_flow_request_title").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wallet_abi_flow_request_title").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_review_warning").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_approve_action").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wallet_abi_flow_success").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wallet_abi_flow_success").assertIsDisplayed()
    }

    @Test
    fun walletAbiFlow_shows_partial_completion_without_retry_when_broadcast_times_out() {
        val koin = GlobalContext.get()
        val greenWallet = insertPreviewWallet(koin = koin)

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
                            requestSource = DefaultWalletAbiDemoRequestSource(),
                            executionPlanner = executionPlanner(),
                            executionRunner = object : WalletAbiExecutionRunner {
                                override suspend fun prepare(
                                    session: GdkSession,
                                    preparedExecution: WalletAbiPreparedExecution
                                ) = WalletAbiPreparedBroadcast(
                                    preparedExecution = preparedExecution,
                                    preparedTransaction = PreparedSoftwareTransaction(
                                        transaction = preparedExecution.transaction,
                                        signedTransaction = JsonObject(emptyMap())
                                    )
                                )

                                override suspend fun broadcast(
                                    session: GdkSession,
                                    preparedBroadcast: WalletAbiPreparedBroadcast,
                                    twoFactorResolver: com.blockstream.data.gdk.TwoFactorResolver
                                ): WalletAbiExecutionResult {
                                    delay(5_000L)
                                    return WalletAbiExecutionResult(txHash = "wallet-abi-demo-tx-hash")
                                }
                            },
                            reviewPreviewer = reviewPreviewer(),
                            submissionTimeoutMillis = 1L
                        )
                    }
                    LaunchedEffect(viewModel) {
                        viewModel.sideEffect.collectLatest { sideEffect ->
                            if (sideEffect == SideEffects.NavigateBack()) {
                                isFlowVisible = false
                            }
                        }
                    }
                    WalletAbiFlowScreen(viewModel = viewModel)
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
            composeRule.onAllNodesWithTag("wallet_abi_flow_request_title").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wallet_abi_flow_approve_action").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wallet_abi_flow_error").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wallet_abi_flow_error").assertIsDisplayed()
        assertEquals(
            0,
            composeRule.onAllNodesWithTag("wallet_abi_flow_retry_action").fetchSemanticsNodes().size
        )
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
                executionRunner = executionRunner(),
                reviewPreviewer = reviewPreviewer()
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
                    WalletAbiFlowScreen(viewModel = viewModel)
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

    private fun setWalletAbiProviderFlowContent(
        greenWallet: GreenWallet,
        requestSource: DefaultWalletAbiDemoRequestSource,
        providerRunner: WalletAbiProviderRunning,
        walletSession: GdkSession,
    ) {
        val koin = GlobalContext.get()
        composeRule.setContent {
            GreenPreview {
                val viewModel = remember {
                    WalletAbiFlowRouteViewModel(
                        greenWallet = greenWallet,
                        store = koin.get<WalletAbiFlowStore>(),
                        snapshotRepository = koin.get<WalletAbiFlowSnapshotRepository>(),
                        walletSession = walletSession,
                        requestSource = requestSource,
                        executionPlanner = executionPlanner(),
                        executionRunner = executionRunner(),
                        reviewPreviewer = reviewPreviewer(),
                        providerRunner = providerRunner,
                    )
                }
                WalletAbiFlowScreen(viewModel = viewModel)
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
        composeRule.onNodeWithTag("wallet_abi_flow_review_warning").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_output_0").assertIsDisplayed()
        assertEquals(
            1,
            composeRule.onAllNodesWithTag("wallet_abi_flow_output_1").fetchSemanticsNodes().size
        )
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

    private fun demoResumeSnapshot(
        greenWallet: GreenWallet,
        phase: WalletAbiResumePhase
    ): WalletAbiResumeSnapshot {
        val account = liquidAccount()
        val requestSource = DefaultWalletAbiDemoRequestSource()
        val envelope = (DefaultWalletAbiRequestParser().parse(
            requestSource.loadRequestEnvelope(WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID)
        ) as WalletAbiRequestParseResult.Success).envelope

        return WalletAbiResumeSnapshot(
            review = WalletAbiFlowReview(
                requestContext = WalletAbiStartRequestContext(
                    requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                    walletId = greenWallet.id
                ),
                method = envelope.method.wireValue,
                title = "Wallet ABI payment",
                message = "Approve a Wallet ABI request",
                accounts = listOf(
                    WalletAbiAccountOption(
                        accountId = account.id,
                        name = account.name
                    )
                ),
                selectedAccountId = account.id,
                approvalTarget = WalletAbiApprovalTarget.Software,
                parsedRequest = envelope.request
            ),
            phase = phase
        )
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
                val selectedAccount = account.takeIf { selectedAccountId == null || selectedAccountId == account.id } ?: account
                val outputs = txCreate.request.params.outputs.mapIndexed { index, output ->
                    com.blockstream.domain.walletabi.execution.WalletAbiPlannedOutput(
                        outputId = output.id,
                        destinationAddress = if (index == 0) {
                            "tlq1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3l4q9m"
                        } else {
                            "tlq1qqd7g4r0n7px6x7m8g7slt0g2j5g6gf8x4v0tpn"
                        },
                        amountSat = output.amountSat,
                        assetId = TESTNET_POLICY_ASSET,
                        recipientScript = output.lock.toString()
                    )
                }
                return if (outputs.size == 1) {
                    com.blockstream.domain.walletabi.execution.WalletAbiSinglePaymentPlan(
                        request = txCreate,
                        accounts = listOf(account),
                        selectedAccount = selectedAccount,
                        feeRate = 12_000L,
                        output = outputs.single()
                    )
                } else {
                    com.blockstream.domain.walletabi.execution.WalletAbiSplitPaymentPlan(
                        request = txCreate,
                        accounts = listOf(account),
                        selectedAccount = selectedAccount,
                        feeRate = 12_000L,
                        outputs = outputs
                    )
                }
            }
        }
    }

    private fun executionRunner(): WalletAbiExecutionRunner {
        return object : WalletAbiExecutionRunner {
            override suspend fun prepare(
                session: GdkSession,
                preparedExecution: WalletAbiPreparedExecution
            ) = WalletAbiPreparedBroadcast(
                preparedExecution = preparedExecution,
                preparedTransaction = PreparedSoftwareTransaction(
                    transaction = preparedExecution.transaction,
                    signedTransaction = JsonObject(emptyMap())
                )
            )

            override suspend fun broadcast(
                session: GdkSession,
                preparedBroadcast: WalletAbiPreparedBroadcast,
                twoFactorResolver: com.blockstream.data.gdk.TwoFactorResolver
            ): WalletAbiExecutionResult {
                delay(200)
                return WalletAbiExecutionResult(txHash = "wallet-abi-demo-tx-hash")
            }
        }
    }

    private fun reviewPreviewer(): WalletAbiReviewPreviewer {
        return WalletAbiReviewPreviewer { _, plan, _ ->
            WalletAbiPreparedExecution(
                plan = plan,
                params = CreateTransactionParams(
                    from = plan.selectedAccount.accountAsset,
                    addressees = plan.outputs.map { output ->
                        AddressParams(
                            address = output.destinationAddress,
                            satoshi = output.amountSat,
                            assetId = output.assetId
                        )
                    }.toJsonElement(),
                    feeRate = plan.feeRate
                ),
                transaction = CreateTransaction(
                    transaction = "rawtx",
                    fee = 100L,
                    feeRate = 12L,
                    outputs = plan.outputs.map { output ->
                        Output(
                            address = output.destinationAddress,
                            assetId = output.assetId,
                            satoshi = output.amountSat
                        )
                    }
                ),
                confirmation = TransactionConfirmation(
                    utxos = plan.outputs.mapIndexed { index, output ->
                        UtxoView(
                            address = output.destinationAddress,
                            assetId = output.assetId,
                            satoshi = output.amountSat,
                            amount = if (index == 0) "1,000 TEST-LBTC" else "2,000 TEST-LBTC",
                            amountExchange = if (index == 0) "0.10 USD" else "0.20 USD"
                        )
                    },
                    fee = "0.01 TEST-LBTC",
                    feeFiat = "0.00 USD",
                    feeRate = "12 sat/vB",
                    total = "3,000.01 TEST-LBTC",
                    totalFiat = "0.30 USD"
                )
            )
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

    private fun fakeProviderRunner(
        requestId: String,
        network: WalletAbiNetwork,
        preview: WalletAbiProviderRequestPreview,
        txHex: String,
        txid: String,
    ): WalletAbiProviderRunning {
        return object : WalletAbiProviderRunning {
            override suspend fun run(
                context: WalletAbiExecutionContext,
                request: com.blockstream.domain.walletabi.request.WalletAbiTxCreateRequest
            ): WalletAbiProviderRunResult {
                return WalletAbiProviderRunResult(
                    response = WalletAbiProviderProcessResponse(
                        abiVersion = "wallet-abi-0.1",
                        requestId = requestId,
                        network = network,
                        status = WalletAbiProviderStatus.OK,
                        transaction = WalletAbiProviderTransactionInfo(
                            txHex = txHex,
                            txid = txid,
                        ),
                        artifacts = kotlinx.serialization.json.buildJsonObject {
                            put(
                                "preview",
                                com.blockstream.data.json.DefaultJson.encodeToJsonElement(
                                    WalletAbiProviderRequestPreview.serializer(),
                                    preview
                                )
                            )
                        }
                    ),
                    responseJson = "{}",
                )
            }
        }
    }

    private fun mockProviderWalletSession(txHash: String): GdkSession {
        return mockk(relaxed = true) {
            every { this@mockk.accounts } returns MutableStateFlow(listOf(liquidAccount()))
            every { this@mockk.allAccounts } returns MutableStateFlow(listOf(liquidAccount()))
            every { this@mockk.activeAccount } returns MutableStateFlow(liquidAccount())
            every { this@mockk.isConnected } returns true
            every { this@mockk.isHardwareWallet } returns false
            every { this@mockk.isWatchOnly } returns MutableStateFlow(false)
            every { this@mockk.networkErrors } returns MutableSharedFlow()
            coEvery { this@mockk.getCredentials(any()) } returns Credentials(
                mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
            )
            coEvery {
                this@mockk.broadcastTransaction(any(), any())
            } returns ProcessedTransactionDetails(txHash = txHash)
        }
    }

    private fun issuancePreview(): WalletAbiProviderRequestPreview {
        return WalletAbiProviderRequestPreview(
            assetDeltas = listOf(
                WalletAbiProviderPreviewAssetDelta(
                    assetId = "issuance_asset_id",
                    walletDeltaSat = 5
                ),
                WalletAbiProviderPreviewAssetDelta(
                    assetId = "reissuance_token_id",
                    walletDeltaSat = 1
                ),
                WalletAbiProviderPreviewAssetDelta(
                    assetId = TESTNET_POLICY_ASSET,
                    walletDeltaSat = -333
                )
            ),
            outputs = listOf(
                WalletAbiProviderPreviewOutput(
                    kind = WalletAbiProviderPreviewOutputKind.RECEIVE,
                    assetId = "issuance_asset_id",
                    amountSat = 5,
                    scriptPubkey = "0014aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                ),
                WalletAbiProviderPreviewOutput(
                    kind = WalletAbiProviderPreviewOutputKind.RECEIVE,
                    assetId = "reissuance_token_id",
                    amountSat = 1,
                    scriptPubkey = "0014bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
                ),
                WalletAbiProviderPreviewOutput(
                    kind = WalletAbiProviderPreviewOutputKind.FEE,
                    assetId = TESTNET_POLICY_ASSET,
                    amountSat = 333,
                    scriptPubkey = "6a"
                )
            )
        )
    }

    private fun reissuancePreview(): WalletAbiProviderRequestPreview {
        return WalletAbiProviderRequestPreview(
            assetDeltas = listOf(
                WalletAbiProviderPreviewAssetDelta(
                    assetId = "reissued_asset_id",
                    walletDeltaSat = 7
                ),
                WalletAbiProviderPreviewAssetDelta(
                    assetId = TESTNET_POLICY_ASSET,
                    walletDeltaSat = -222
                )
            ),
            outputs = listOf(
                WalletAbiProviderPreviewOutput(
                    kind = WalletAbiProviderPreviewOutputKind.RECEIVE,
                    assetId = "reissued_asset_id",
                    amountSat = 7,
                    scriptPubkey = "0014cccccccccccccccccccccccccccccccccccccccc"
                ),
                WalletAbiProviderPreviewOutput(
                    kind = WalletAbiProviderPreviewOutputKind.FEE,
                    assetId = TESTNET_POLICY_ASSET,
                    amountSat = 222,
                    scriptPubkey = "6a"
                )
            )
        )
    }

    private fun issuanceEnvelope(requestId: String): String {
        return """
            {
              "jsonrpc": "2.0",
              "id": "wallet-abi-issuance-envelope",
              "method": "wallet_abi_process_request",
              "params": {
                "abi_version": "wallet-abi-0.1",
                "request_id": "$requestId",
                "network": "testnet-liquid",
                "broadcast": true,
                "params": {
                  "inputs": [
                    {
                      "id": "issuance",
                      "utxo_source": {
                        "wallet": {
                          "filter": {
                            "asset": "none",
                            "amount": "none",
                            "lock": "none"
                          }
                        }
                      },
                      "unblinding": "wallet",
                      "sequence": 4294967295,
                      "issuance": {
                        "kind": "new",
                        "asset_amount_sat": 5,
                        "token_amount_sat": 1,
                        "entropy": [7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7]
                      },
                      "finalizer": {
                        "type": "wallet"
                      }
                    }
                  ],
                  "outputs": [
                    {
                      "id": "issued_asset",
                      "amount_sat": 5,
                      "lock": { "type": "wallet" },
                      "asset": { "type": "new_issuance_asset", "input_index": 0 },
                      "blinder": "wallet"
                    },
                    {
                      "id": "reissuance_token",
                      "amount_sat": 1,
                      "lock": { "type": "wallet" },
                      "asset": { "type": "new_issuance_token", "input_index": 0 },
                      "blinder": "wallet"
                    }
                  ]
                }
              }
            }
        """.trimIndent()
    }

    private fun reissuanceEnvelope(
        requestId: String,
        tokenAssetId: String,
    ): String {
        return """
            {
              "jsonrpc": "2.0",
              "id": "wallet-abi-reissuance-envelope",
              "method": "wallet_abi_process_request",
              "params": {
                "abi_version": "wallet-abi-0.1",
                "request_id": "$requestId",
                "network": "testnet-liquid",
                "broadcast": true,
                "params": {
                  "inputs": [
                    {
                      "id": "reissuance",
                      "utxo_source": {
                        "wallet": {
                          "filter": {
                            "asset": {
                              "exact": {
                                "asset_id": "$tokenAssetId"
                              }
                            },
                            "amount": "none",
                            "lock": "none"
                          }
                        }
                      },
                      "unblinding": "wallet",
                      "sequence": 4294967295,
                      "issuance": {
                        "kind": "reissue",
                        "asset_amount_sat": 7,
                        "token_amount_sat": 0,
                        "entropy": [8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8]
                      },
                      "finalizer": {
                        "type": "wallet"
                      }
                    }
                  ],
                  "outputs": [
                    {
                      "id": "reissued_asset",
                      "amount_sat": 7,
                      "lock": { "type": "wallet" },
                      "asset": { "type": "re_issuance_asset", "input_index": 0 },
                      "blinder": "wallet"
                    }
                  ]
                }
              }
            }
        """.trimIndent()
    }

    private companion object {
        const val TESTNET_POLICY_ASSET =
            "144c654344aa716d6f3abcc1ca90e5641e4e2a7f633bc09fe3baf64585819a49"
    }
}
