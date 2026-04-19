package com.blockstream.compose.models.walletabi

import androidx.lifecycle.viewModelScope
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.CountlyBase
import com.blockstream.data.banner.Banner
import com.blockstream.data.config.AppInfo
import com.blockstream.data.database.Database
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.PreparedSoftwareTransaction
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.Credentials
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.data.Output
import com.blockstream.data.gdk.data.UtxoView
import com.blockstream.data.gdk.params.AddressParams
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.gdk.params.toJsonElement
import com.blockstream.data.managers.PromoManager
import com.blockstream.data.managers.SessionManager
import com.blockstream.data.managers.SettingsManager
import com.blockstream.data.walletabi.flow.FakeWalletAbiFlowDriver
import com.blockstream.data.walletabi.request.DefaultWalletAbiDemoRequestSource
import com.blockstream.data.walletabi.request.WalletAbiDemoRequestSource
import com.blockstream.data.transaction.TransactionConfirmation
import com.blockstream.domain.banner.GetBannerUseCase
import com.blockstream.domain.promo.GetPromoUseCase
import com.blockstream.domain.walletabi.execution.DefaultWalletAbiExecutionPlanner
import com.blockstream.domain.walletabi.execution.WalletAbiPreparedExecution
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionResult
import com.blockstream.domain.walletabi.execution.WalletAbiPreparedBroadcast
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionRunner
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionPlanner
import com.blockstream.domain.walletabi.execution.WalletAbiReviewPreviewer
import com.blockstream.domain.walletabi.execution.WalletAbiOutputAddressResolver
import com.blockstream.domain.walletabi.flow.WalletAbiAccountOption
import com.blockstream.domain.walletabi.flow.WalletAbiApprovalTarget
import com.blockstream.domain.walletabi.flow.WalletAbiCancelledReason
import com.blockstream.domain.walletabi.flow.WalletAbiExecutionEvent
import com.blockstream.domain.walletabi.flow.WalletAbiFlowIntent
import com.blockstream.domain.walletabi.flow.WalletAbiFlowOutput
import com.blockstream.domain.walletabi.flow.WalletAbiFlowReview
import com.blockstream.domain.walletabi.flow.WalletAbiFlowSnapshotRepository
import com.blockstream.domain.walletabi.flow.DefaultWalletAbiFlowStore
import com.blockstream.domain.walletabi.flow.WalletAbiResumePhase
import com.blockstream.domain.walletabi.flow.WalletAbiResumeSnapshot
import com.blockstream.domain.walletabi.flow.WalletAbiResolutionCommand
import com.blockstream.domain.walletabi.flow.WalletAbiFlowState
import com.blockstream.domain.walletabi.flow.WalletAbiFlowStore
import com.blockstream.domain.walletabi.flow.WalletAbiFlowError
import com.blockstream.domain.walletabi.flow.WalletAbiFlowErrorKind
import com.blockstream.domain.walletabi.flow.WalletAbiFlowPhase
import com.blockstream.domain.walletabi.flow.WalletAbiStartRequestContext
import com.blockstream.domain.walletabi.flow.WalletAbiSubmissionCommand
import com.blockstream.domain.walletabi.flow.WalletAbiSuccessResult
import com.blockstream.domain.walletabi.request.WalletAbiNetwork
import com.blockstream.domain.walletabi.request.WalletAbiParsedRequest
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.json.JsonObject

class WalletAbiFlowRouteViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val greenWallet = previewWallet()
    private lateinit var store: FakeWalletAbiFlowStore
    private lateinit var snapshotRepository: WalletAbiFlowSnapshotRepository
    private lateinit var driver: FakeWalletAbiFlowDriver
    private lateinit var requestSource: WalletAbiDemoRequestSource
    private lateinit var executionPlanner: WalletAbiExecutionPlanner
    private lateinit var executionRunner: WalletAbiExecutionRunner
    private lateinit var reviewPreviewer: WalletAbiReviewPreviewer
    private lateinit var walletSession: GdkSession
    private lateinit var liquidAccount: Account
    private val createdViewModels = mutableListOf<WalletAbiFlowRouteViewModel>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        store = FakeWalletAbiFlowStore()
        snapshotRepository = mockk(relaxed = true)
        driver = FakeWalletAbiFlowDriver()
        requestSource = DefaultWalletAbiDemoRequestSource()
        executionPlanner = DefaultWalletAbiExecutionPlanner(
            outputAddressResolver = WalletAbiOutputAddressResolver { output, _, _ ->
                val lock = output.lock as? kotlinx.serialization.json.JsonObject ?: return@WalletAbiOutputAddressResolver null
                when (lock["script"]?.toString()?.trim('"')) {
                    "00140000000000000000000000000000000000000000" -> "tlq1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3l4q9m"
                    else -> null
                }
            }
        )
        reviewPreviewer = WalletAbiReviewPreviewer { _, plan, _ ->
            preparedExecution(plan)
        }
        executionRunner = object : WalletAbiExecutionRunner {
            override suspend fun prepare(
                session: GdkSession,
                preparedExecution: WalletAbiPreparedExecution
            ) = com.blockstream.domain.walletabi.execution.WalletAbiPreparedBroadcast(
                preparedExecution = preparedExecution,
                preparedTransaction = PreparedSoftwareTransaction(
                    transaction = preparedExecution.transaction,
                    signedTransaction = JsonObject(emptyMap())
                )
            )

            override suspend fun broadcast(
                session: GdkSession,
                preparedBroadcast: com.blockstream.domain.walletabi.execution.WalletAbiPreparedBroadcast,
                twoFactorResolver: com.blockstream.data.gdk.TwoFactorResolver
            ): WalletAbiExecutionResult {
                return WalletAbiExecutionResult(txHash = "wallet-abi-demo-tx-hash")
            }
        }
        liquidAccount = liquidAccount()
        walletSession = mockSession(accounts = listOf(liquidAccount), activeAccount = liquidAccount)

        val countly = mockk<CountlyBase>(relaxed = true).also {
            every { it.remoteConfigUpdateEvent } returns MutableSharedFlow()
            every { it.getRemoteConfigValueForBanners() } returns listOf<Banner>()
        }
        val settingsManager = mockk<SettingsManager>(relaxed = true)
        val database = mockk<Database>(relaxed = true).also {
            every { it.getWalletFlowOrNull(greenWallet.id) } returns flowOf(greenWallet)
        }
        val promoManager = mockk<PromoManager>(relaxed = true).also {
            every { it.promos } returns MutableStateFlow(emptyList())
        }
        val sessionManager = mockk<SessionManager>(relaxed = true).also {
            every { it.getWalletSessionOrNull(greenWallet) } returns walletSession
            every { it.getWalletSessionOrCreate(greenWallet) } returns walletSession
        }

        startKoin {
            modules(
                module {
                    single { AppInfo("green_test", "1.0.0-test", isDevelopment = true, isDebug = true) }
                    single { countly }
                    single { settingsManager }
                    single { database }
                    single { promoManager }
                    single { sessionManager }
                    single { GetBannerUseCase() }
                    single { GetPromoUseCase(get(), get(), get()) }
                }
            )
        }
    }

    @After
    fun tearDown() {
        createdViewModels.forEach { viewModel ->
            viewModel.viewModelScope.cancel()
        }
        createdViewModels.clear()
        kotlinx.coroutines.test.runTest(dispatcher) {
            advanceUntilIdle()
        }
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun init_dispatches_start_for_demo_request() = runTest(dispatcher) {
        createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            driver = driver
        )

        advanceUntilIdle()

        assertEquals(
            listOf<WalletAbiFlowIntent>(
                WalletAbiFlowIntent.Start(
                    requestContext = com.blockstream.domain.walletabi.flow.WalletAbiStartRequestContext(
                        requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                        walletId = greenWallet.id
                    )
                )
            ),
            store.intents
        )
    }


    @Test
    fun loadRequest_output_dispatches_request_loaded_review() = runTest(dispatcher) {
        val viewModel = createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            driver = driver
        )

        advanceUntilIdle()

        store.mutableOutputs.emit(
            WalletAbiFlowOutput.LoadRequest(
                WalletAbiStartRequestContext(
                    requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                    walletId = greenWallet.id
                )
            )
        )

        advanceUntilIdle()

        val intent = assertIs<WalletAbiFlowIntent.OnExecutionEvent>(store.intents.last())
        val review = assertIs<WalletAbiExecutionEvent.RequestLoaded>(intent.event).review
        val parsedRequest = assertIs<WalletAbiParsedRequest.TxCreate>(review.parsedRequest).request

        assertEquals(
            WalletAbiStartRequestContext(
                requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                walletId = greenWallet.id
            ),
            review.requestContext
        )
        assertEquals("Wallet ABI payment", review.title)
        assertEquals("Approve a Wallet ABI request", review.message)
        assertEquals(liquidAccount.id, review.selectedAccountId)
        assertEquals(WalletAbiApprovalTarget.Software, review.approvalTarget)
        assertEquals("wallet-abi-0.1", parsedRequest.abiVersion)
        assertEquals(WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID, parsedRequest.requestId)
        assertEquals(WalletAbiNetwork.TESTNET_LIQUID, parsedRequest.network)
        assertEquals(listOf(liquidAccount.id), review.accounts.map { it.accountId })
        assertEquals(listOf(liquidAccount.name), review.accounts.map { it.name })
        assertEquals(0, parsedRequest.params.inputs.size)
        assertEquals(1, parsedRequest.params.outputs.size)
        assertEquals(12_000f, parsedRequest.params.feeRateSatKvb)
        assertEquals(true, parsedRequest.broadcast)
        assertEquals("tlq1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3l4q9m", review.executionDetails?.destinationAddress)
        assertEquals(1_000L, review.executionDetails?.amountSat)
        assertEquals(TESTNET_POLICY_ASSET, review.executionDetails?.assetId)
        assertEquals(WalletAbiNetwork.TESTNET_LIQUID.wireValue, review.executionDetails?.network)
        assertEquals(12_000L, review.executionDetails?.feeRate)

        assertEquals("wallet_abi_process_request", viewModel.reviewLook.value?.method)
        assertEquals("wallet-abi-0.1", viewModel.reviewLook.value?.abiVersion)
        assertEquals("1,000 TEST-LBTC", viewModel.reviewLook.value?.amount)
        assertEquals("0.01 TEST-LBTC", viewModel.reviewLook.value?.transactionConfirmation?.fee)
    }

    @Test
    fun cancelActiveWork_loading_dispatches_user_cancelled_without_request_loaded() = runTest(dispatcher) {
        val reviewGate = CompletableDeferred<Unit>()
        reviewPreviewer = WalletAbiReviewPreviewer { _, plan, _ ->
            reviewGate.await()
            preparedExecution(plan)
        }

        createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            reviewPreviewer = reviewPreviewer
        )

        store.state.value = WalletAbiFlowState.Loading(
            WalletAbiStartRequestContext(
                requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                walletId = greenWallet.id
            ),
            isCancelling = true
        )
        store.mutableOutputs.emit(
            WalletAbiFlowOutput.LoadRequest(
                WalletAbiStartRequestContext(
                    requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                    walletId = greenWallet.id
                )
            )
        )
        advanceUntilIdle()

        store.mutableOutputs.emit(WalletAbiFlowOutput.CancelActiveWork(WalletAbiFlowPhase.LOADING))
        advanceUntilIdle()

        val cancelled = store.intents.last { intent ->
            intent is WalletAbiFlowIntent.OnExecutionEvent
        } as WalletAbiFlowIntent.OnExecutionEvent
        assertEquals(
            WalletAbiExecutionEvent.Cancelled(WalletAbiCancelledReason.UserCancelled),
            cancelled.event
        )
        assertEquals(
            0,
            store.intents.count { intent ->
                intent is WalletAbiFlowIntent.OnExecutionEvent &&
                    intent.event is WalletAbiExecutionEvent.RequestLoaded
            }
        )
    }

    @Test
    fun loadRequest_output_dispatches_timeout_when_loading_exceeds_deadline() = runTest(dispatcher) {
        val reviewGate = CompletableDeferred<Unit>()
        reviewPreviewer = WalletAbiReviewPreviewer { _, plan, _ ->
            reviewGate.await()
            preparedExecution(plan)
        }

        createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            reviewPreviewer = reviewPreviewer,
            loadingTimeoutMillis = 1L
        )
        advanceUntilIdle()

        store.mutableOutputs.emit(
            WalletAbiFlowOutput.LoadRequest(
                WalletAbiStartRequestContext(
                    requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                    walletId = greenWallet.id
                )
            )
        )

        advanceTimeBy(1L)
        advanceUntilIdle()

        assertEquals(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.Failed(
                    WalletAbiFlowError(
                        kind = WalletAbiFlowErrorKind.TIMEOUT,
                        phase = WalletAbiFlowPhase.LOADING,
                        message = "Wallet ABI request timed out",
                        retryable = true
                    )
                )
            ),
            store.intents.last { intent ->
                intent is WalletAbiFlowIntent.OnExecutionEvent
            }
        )
        assertEquals(
            0,
            store.intents.count { intent ->
                intent is WalletAbiFlowIntent.OnExecutionEvent &&
                    intent.event is WalletAbiExecutionEvent.RequestLoaded
            }
        )
    }

    @Test
    fun loadRequest_output_dispatches_error_for_malformed_request() = runTest(dispatcher) {
        requestSource = DefaultWalletAbiDemoRequestSource { _ -> "{" }

        createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            walletSession = walletSession,
            requestSource = requestSource,
            executionPlanner = executionPlanner,
            driver = driver
        )

        advanceUntilIdle()

        store.mutableOutputs.emit(
            WalletAbiFlowOutput.LoadRequest(
                WalletAbiStartRequestContext(
                    requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                    walletId = greenWallet.id
                )
            )
        )

        advanceUntilIdle()

        assertEquals(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.Failed(
                    WalletAbiFlowError(
                        kind = WalletAbiFlowErrorKind.INVALID_REQUEST,
                        phase = WalletAbiFlowPhase.LOADING,
                        message = "Wallet ABI request envelope is malformed",
                        retryable = false
                    )
                )
            ),
            store.intents.last()
        )
    }

    @Test
    fun loadRequest_output_dispatches_error_for_unsupported_request() = runTest(dispatcher) {
        requestSource = DefaultWalletAbiDemoRequestSource { _ ->
            """
                {
                  "jsonrpc": "2.0",
                  "id": "wallet-abi-demo-envelope",
                  "method": "wallet_abi_get_account"
                }
            """.trimIndent()
        }

        createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            walletSession = walletSession,
            requestSource = requestSource,
            executionPlanner = executionPlanner,
            driver = driver
        )

        advanceUntilIdle()

        store.mutableOutputs.emit(
            WalletAbiFlowOutput.LoadRequest(
                WalletAbiStartRequestContext(
                    requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                    walletId = greenWallet.id
                )
            )
        )

        advanceUntilIdle()

        assertEquals(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.Failed(
                    WalletAbiFlowError(
                        kind = WalletAbiFlowErrorKind.UNSUPPORTED_REQUEST,
                        phase = WalletAbiFlowPhase.LOADING,
                        message = "Unsupported Wallet ABI method 'wallet_abi_get_account'",
                        retryable = false
                    )
                )
            ),
            store.intents.last()
        )
    }

    @Test
    fun init_reaches_request_loaded_with_real_store() = runTest(dispatcher) {
        val realStore = DefaultWalletAbiFlowStore()
        val viewModel = createViewModel(
            greenWallet = greenWallet,
            store = realStore,
            snapshotRepository = snapshotRepository,
            driver = driver
        )

        advanceUntilIdle()

        val state = assertIs<WalletAbiFlowState.RequestLoaded>(viewModel.state.value)
        assertEquals(WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID, state.review.requestContext.requestId)
        assertEquals(liquidAccount.id, state.review.selectedAccountId)
        assertIs<WalletAbiParsedRequest.TxCreate>(state.review.parsedRequest)
    }

    @Test
    fun approve_reaches_success_with_real_store() = runTest(dispatcher) {
        val realStore = DefaultWalletAbiFlowStore()
        val viewModel = createViewModel(
            greenWallet = greenWallet,
            store = realStore,
            snapshotRepository = snapshotRepository,
            driver = driver
        )

        advanceUntilIdle()

        viewModel.dispatch(WalletAbiFlowIntent.Approve)
        advanceUntilIdle()

        val state = assertIs<WalletAbiFlowState.Success>(viewModel.state.value)
        assertEquals(WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID, state.result.requestId)
        assertEquals("wallet-abi-demo-tx-hash", state.result.txHash)
    }

    @Test
    fun selectAccount_ignores_stale_refresh_completion() = runTest(dispatcher) {
        val realStore = DefaultWalletAbiFlowStore()
        val delayedAccount = liquidAccount(pointer = 1L, name = "Second liquid account")
        walletSession = mockSession(
            accounts = listOf(liquidAccount, delayedAccount),
            activeAccount = liquidAccount
        )
        val delayedRefreshGate = CompletableDeferred<Unit>()
        reviewPreviewer = WalletAbiReviewPreviewer { _, plan, _ ->
            if (plan.selectedAccount.id == delayedAccount.id) {
                delayedRefreshGate.await()
            }
            preparedExecution(plan)
        }
        val viewModel = createViewModel(
            greenWallet = greenWallet,
            store = realStore,
            snapshotRepository = snapshotRepository,
            walletSession = walletSession,
            reviewPreviewer = reviewPreviewer
        )

        advanceUntilIdle()

        viewModel.dispatch(WalletAbiFlowIntent.SelectAccount(delayedAccount.id))
        advanceUntilIdle()

        viewModel.dispatch(WalletAbiFlowIntent.SelectAccount(liquidAccount.id))
        advanceUntilIdle()

        delayedRefreshGate.complete(Unit)
        advanceUntilIdle()

        val state = assertIs<WalletAbiFlowState.RequestLoaded>(viewModel.state.value)
        assertEquals(liquidAccount.id, state.review.selectedAccountId)
        assertEquals(liquidAccount.accountAssetBalance, viewModel.reviewLook.value?.accountAssetBalance)
    }

    @Test
    fun state_exposes_store_state() {
        val viewModel = createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            driver = driver
        )

        assertEquals(
            WalletAbiFlowState.Idle,
            viewModel.state.value
        )
    }

    @Test
    fun persistSnapshot_output_is_forwarded_to_repository() = runTest(dispatcher) {
        createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            driver = driver
        )

        advanceUntilIdle()

        store.mutableOutputs.emit(
            WalletAbiFlowOutput.PersistSnapshot(
                WalletAbiResumeSnapshot(
                    review = WalletAbiFlowReview(
                        requestContext = WalletAbiStartRequestContext(
                            requestId = "wallet-abi-demo-request",
                            walletId = greenWallet.id
                        ),
                        title = "Wallet ABI payment",
                        message = "Approve a Wallet ABI request",
                        accounts = listOf(
                            WalletAbiAccountOption(
                                accountId = liquidAccount.id,
                                name = liquidAccount.name
                            )
                        ),
                        selectedAccountId = liquidAccount.id,
                        approvalTarget = WalletAbiApprovalTarget.Software
                    ),
                    phase = WalletAbiResumePhase.REQUEST_LOADED
                )
            )
        )

        advanceUntilIdle()

        coVerify(exactly = 1) {
            snapshotRepository.save(
                walletId = greenWallet.id,
                snapshot = any()
            )
        }
    }

    @Test
    fun startResolution_output_dispatches_resolved_review() = runTest(dispatcher) {
        createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            driver = driver
        )

        advanceUntilIdle()

        store.mutableOutputs.emit(
            WalletAbiFlowOutput.LoadRequest(
                WalletAbiStartRequestContext(
                    requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                    walletId = greenWallet.id
                )
            )
        )

        advanceUntilIdle()

        store.mutableOutputs.emit(
            WalletAbiFlowOutput.StartResolution(
                WalletAbiResolutionCommand(
                    requestContext = WalletAbiStartRequestContext(
                        requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                        walletId = greenWallet.id
                    ),
                    selectedAccountId = liquidAccount.id
                )
            )
        )

        advanceUntilIdle()

        val intent = assertIs<WalletAbiFlowIntent.OnExecutionEvent>(store.intents.last())
        val review = assertIs<WalletAbiExecutionEvent.Resolved>(intent.event).review
        val parsedRequest = assertIs<WalletAbiParsedRequest.TxCreate>(review.parsedRequest).request

        assertEquals(
            WalletAbiStartRequestContext(
                requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                walletId = greenWallet.id
            ),
            review.requestContext
        )
        assertEquals("Wallet ABI payment", review.title)
        assertEquals("Approve a Wallet ABI request", review.message)
        assertEquals(liquidAccount.id, review.selectedAccountId)
        assertEquals(WalletAbiApprovalTarget.Software, review.approvalTarget)
        assertEquals(WalletAbiNetwork.TESTNET_LIQUID, parsedRequest.network)
        assertEquals(WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID, parsedRequest.requestId)
        assertEquals(listOf(liquidAccount.id), review.accounts.map { it.accountId })
        assertEquals("tlq1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3l4q9m", review.executionDetails?.destinationAddress)
        assertEquals(1_000L, review.executionDetails?.amountSat)
        assertEquals(TESTNET_POLICY_ASSET, review.executionDetails?.assetId)
        assertEquals("testnet-liquid", review.executionDetails?.network)
        assertEquals(12_000L, review.executionDetails?.feeRate)
    }

    @Test
    fun startSubmission_output_dispatches_success_sequence() = runTest(dispatcher) {
        createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            driver = driver
        )

        advanceUntilIdle()

        store.mutableOutputs.emit(
            WalletAbiFlowOutput.LoadRequest(
                WalletAbiStartRequestContext(
                    requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                    walletId = greenWallet.id
                )
            )
        )
        advanceUntilIdle()

        store.mutableOutputs.emit(
            WalletAbiFlowOutput.StartSubmission(
                WalletAbiSubmissionCommand(
                    requestContext = WalletAbiStartRequestContext(
                        requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                        walletId = greenWallet.id
                    ),
                    selectedAccountId = liquidAccount.id
                )
            )
        )

        advanceUntilIdle()

        assertEquals(
            listOf(
                WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.Submitted),
                WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.Broadcasted),
                WalletAbiFlowIntent.OnExecutionEvent(
                    WalletAbiExecutionEvent.RemoteResponseSent(
                        result = WalletAbiSuccessResult(
                            requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                            txHash = "wallet-abi-demo-tx-hash"
                        )
                    )
                )
            ),
            store.intents.takeLast(3)
        )
    }

    @Test
    fun startSubmission_output_dispatches_error_when_execution_fails() = runTest(dispatcher) {
        val failingRunner = object : WalletAbiExecutionRunner {
            override suspend fun prepare(
                session: GdkSession,
                preparedExecution: WalletAbiPreparedExecution
            ) = com.blockstream.domain.walletabi.execution.WalletAbiPreparedBroadcast(
                preparedExecution = preparedExecution,
                preparedTransaction = PreparedSoftwareTransaction(
                    transaction = preparedExecution.transaction,
                    signedTransaction = JsonObject(emptyMap())
                )
            )

            override suspend fun broadcast(
                session: GdkSession,
                preparedBroadcast: com.blockstream.domain.walletabi.execution.WalletAbiPreparedBroadcast,
                twoFactorResolver: com.blockstream.data.gdk.TwoFactorResolver
            ): WalletAbiExecutionResult {
                error("send failed")
            }
        }

        createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            executionRunner = failingRunner,
            driver = driver
        )

        advanceUntilIdle()

        store.mutableOutputs.emit(
            WalletAbiFlowOutput.LoadRequest(
                WalletAbiStartRequestContext(
                    requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                    walletId = greenWallet.id
                )
            )
        )
        advanceUntilIdle()

        store.mutableOutputs.emit(
            WalletAbiFlowOutput.StartSubmission(
                WalletAbiSubmissionCommand(
                    requestContext = WalletAbiStartRequestContext(
                        requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                        walletId = greenWallet.id
                    ),
                    selectedAccountId = liquidAccount.id
                )
            )
        )

        advanceUntilIdle()

        assertEquals(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.Failed(
                    WalletAbiFlowError(
                        kind = WalletAbiFlowErrorKind.EXECUTION_FAILURE,
                        phase = WalletAbiFlowPhase.SUBMISSION,
                        message = "send failed",
                        retryable = true
                    )
                )
            ),
            store.intents.last()
        )
    }

    @Test
    fun cancelActiveWork_submission_dispatches_user_cancelled_without_broadcast() = runTest(dispatcher) {
        val prepareGate = CompletableDeferred<Unit>()
        val blockingRunner = object : WalletAbiExecutionRunner {
            override suspend fun prepare(
                session: GdkSession,
                preparedExecution: WalletAbiPreparedExecution
            ): WalletAbiPreparedBroadcast {
                prepareGate.await()
                return WalletAbiPreparedBroadcast(
                    preparedExecution = preparedExecution,
                    preparedTransaction = PreparedSoftwareTransaction(
                        transaction = preparedExecution.transaction,
                        signedTransaction = JsonObject(emptyMap())
                    )
                )
            }
        }

        createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            executionRunner = blockingRunner,
            driver = driver
        )

        advanceUntilIdle()

        store.mutableOutputs.emit(
            WalletAbiFlowOutput.LoadRequest(
                WalletAbiStartRequestContext(
                    requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                    walletId = greenWallet.id
                )
            )
        )
        advanceUntilIdle()

        store.mutableOutputs.emit(
            WalletAbiFlowOutput.StartSubmission(
                WalletAbiSubmissionCommand(
                    requestContext = WalletAbiStartRequestContext(
                        requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                        walletId = greenWallet.id
                    ),
                    selectedAccountId = liquidAccount.id
                )
            )
        )
        advanceUntilIdle()

        store.mutableOutputs.emit(WalletAbiFlowOutput.CancelActiveWork(WalletAbiFlowPhase.SUBMISSION))
        advanceUntilIdle()

        assertEquals(
            WalletAbiExecutionEvent.Cancelled(WalletAbiCancelledReason.UserCancelled),
            (store.intents.last { intent ->
                intent is WalletAbiFlowIntent.OnExecutionEvent
            } as WalletAbiFlowIntent.OnExecutionEvent).event
        )
        assertEquals(
            0,
            store.intents.count { intent ->
                intent is WalletAbiFlowIntent.OnExecutionEvent &&
                    (intent.event == WalletAbiExecutionEvent.Broadcasted ||
                        intent.event is WalletAbiExecutionEvent.RemoteResponseSent)
            }
        )
    }

    @Test
    fun dismissTerminal_posts_navigate_back() = runTest(dispatcher) {
        store.state.value = WalletAbiFlowState.Success(
            WalletAbiSuccessResult(
                requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                txHash = "wallet-abi-demo-tx-hash",
                responseId = "wallet-abi-demo-response"
            )
        )
        val viewModel = createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            driver = driver
        )

        advanceUntilIdle()

        val sideEffect = async {
            viewModel.sideEffect.first()
        }

        viewModel.dispatch(WalletAbiFlowIntent.DismissTerminal)
        advanceUntilIdle()

        assertEquals(
            SideEffects.NavigateBack(),
            sideEffect.await()
        )
    }

    private class FakeWalletAbiFlowStore : WalletAbiFlowStore {
        override val state = MutableStateFlow<WalletAbiFlowState>(WalletAbiFlowState.Idle)
        val mutableOutputs = MutableSharedFlow<WalletAbiFlowOutput>()
        override val outputs: Flow<WalletAbiFlowOutput> = mutableOutputs
        val intents = mutableListOf<WalletAbiFlowIntent>()

        override suspend fun dispatch(intent: WalletAbiFlowIntent) {
            if (intent == WalletAbiFlowIntent.DismissTerminal &&
                (state.value is WalletAbiFlowState.Success ||
                    state.value is WalletAbiFlowState.Cancelled ||
                    state.value is WalletAbiFlowState.Error)
            ) {
                state.value = WalletAbiFlowState.Idle
            }
            intents += intent
        }
    }

    private fun createViewModel(
        greenWallet: com.blockstream.data.data.GreenWallet = this.greenWallet,
        store: WalletAbiFlowStore = this.store,
        snapshotRepository: WalletAbiFlowSnapshotRepository = this.snapshotRepository,
        walletSession: GdkSession = this.walletSession,
        requestSource: WalletAbiDemoRequestSource = this.requestSource,
        executionPlanner: WalletAbiExecutionPlanner = this.executionPlanner,
        executionRunner: WalletAbiExecutionRunner = this.executionRunner,
        reviewPreviewer: WalletAbiReviewPreviewer = this.reviewPreviewer,
        loadingTimeoutMillis: Long = 15_000L,
        driver: FakeWalletAbiFlowDriver = this.driver
    ): WalletAbiFlowRouteViewModel {
        return WalletAbiFlowRouteViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            walletSession = walletSession,
            requestSource = requestSource,
            executionPlanner = executionPlanner,
            executionRunner = executionRunner,
            reviewPreviewer = reviewPreviewer,
            loadingTimeoutMillis = loadingTimeoutMillis
        ).also { createdViewModels += it }
    }

    private fun preparedExecution(plan: com.blockstream.domain.walletabi.execution.WalletAbiExecutionPlan): WalletAbiPreparedExecution {
        return WalletAbiPreparedExecution(
            plan = plan,
            params = CreateTransactionParams(
                from = plan.selectedAccount.accountAsset,
                addressees = listOf(
                    AddressParams(
                        address = plan.destinationAddress,
                        satoshi = plan.amountSat,
                        assetId = plan.assetId
                    )
                ).toJsonElement(),
                feeRate = plan.feeRate
            ),
            transaction = CreateTransaction(
                transaction = "rawtx",
                fee = 100L,
                feeRate = 12L,
                outputs = listOf(
                    Output(
                        address = plan.destinationAddress,
                        assetId = plan.assetId,
                        satoshi = plan.amountSat
                    )
                )
            ),
            confirmation = TransactionConfirmation(
                utxos = listOf(
                    UtxoView(
                        address = plan.destinationAddress,
                        assetId = plan.assetId,
                        satoshi = plan.amountSat,
                        amount = "1,000 TEST-LBTC",
                        amountExchange = "0.10 USD"
                    )
                ),
                fee = "0.01 TEST-LBTC",
                feeFiat = "0.00 USD",
                feeRate = "12 sat/vB",
                total = "1,000.01 TEST-LBTC",
                totalFiat = "0.10 USD"
            )
        )
    }

    private fun liquidAccount(
        pointer: Long = 0L,
        name: String = "Liquid account"
    ): Account {
        return account(
            name = name,
            pointer = pointer,
            network = Network(
                network = "testnet-liquid",
                name = "Liquid Testnet",
                isMainnet = false,
                isLiquid = true,
                isDevelopment = false,
                policyAsset = TESTNET_POLICY_ASSET
            )
        )
    }

    private fun account(
        name: String,
        pointer: Long,
        network: Network
    ): Account {
        val setupSession = mockk<GdkSession>(relaxed = true)
        return Account(
            gdkName = name,
            pointer = pointer,
            type = AccountType.BIP84_SEGWIT
        ).apply {
            setup(session = setupSession, network = network)
        }
    }

    private fun mockSession(
        accounts: List<Account>,
        activeAccount: Account?,
        isHardwareWallet: Boolean = false,
        mnemonic: String? = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
    ): GdkSession {
        return mockk(relaxed = true) {
            every { this@mockk.accounts } returns MutableStateFlow(accounts)
            every { this@mockk.allAccounts } returns MutableStateFlow(accounts)
            every { this@mockk.activeAccount } returns MutableStateFlow(activeAccount)
            every { this@mockk.isConnected } returns true
            every { this@mockk.isHardwareWallet } returns isHardwareWallet
            every { this@mockk.isWatchOnly } returns MutableStateFlow(false)
            every { this@mockk.networkErrors } returns MutableSharedFlow()
            coEvery { this@mockk.getCredentials(any()) } returns Credentials(mnemonic = mnemonic)
        }
    }

    private companion object {
        const val TESTNET_POLICY_ASSET =
            "144c654344aa716d6f3abcc1ca90e5641e4e2a7f633bc09fe3baf64585819a49"
    }
}
