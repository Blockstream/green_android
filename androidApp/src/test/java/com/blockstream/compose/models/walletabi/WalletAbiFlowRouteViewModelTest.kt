package com.blockstream.compose.models.walletabi

import androidx.lifecycle.viewModelScope
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.navigation.WalletAbiFlowLaunchMode
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
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import com.blockstream.data.gdk.data.UtxoView
import com.blockstream.data.gdk.params.BroadcastTransactionParams
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
import com.blockstream.domain.walletabi.flow.WalletAbiRequestFamily
import com.blockstream.domain.walletabi.flow.WalletAbiResolutionState
import com.blockstream.domain.walletabi.flow.WalletAbiAccountOption
import com.blockstream.domain.walletabi.flow.WalletAbiApprovalTarget
import com.blockstream.domain.walletabi.flow.WalletAbiCancelledReason
import com.blockstream.domain.walletabi.flow.WalletAbiExecutionEvent
import com.blockstream.domain.walletabi.flow.WalletAbiExecutionDetails
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
import com.blockstream.domain.walletabi.request.DefaultWalletAbiRequestParser
import com.blockstream.domain.walletabi.request.WalletAbiParsedRequest
import com.blockstream.domain.walletabi.request.WalletAbiRequestParseResult
import com.blockstream.domain.walletabi.provider.WalletAbiExecutionContext
import com.blockstream.domain.walletabi.provider.WalletAbiExecutionContextResolver
import com.blockstream.domain.walletabi.provider.WalletAbiExecutionContextResolving
import com.blockstream.domain.walletabi.provider.WalletAbiProviderPreviewAssetDelta
import com.blockstream.domain.walletabi.provider.WalletAbiProviderPreviewOutput
import com.blockstream.domain.walletabi.provider.WalletAbiProviderPreviewOutputKind
import com.blockstream.domain.walletabi.provider.WalletAbiProviderProcessResponse
import com.blockstream.domain.walletabi.provider.WalletAbiProviderRequestPreview
import com.blockstream.domain.walletabi.provider.WalletAbiProviderRunning
import com.blockstream.domain.walletabi.provider.WalletAbiProviderStatus
import com.blockstream.domain.walletabi.provider.WalletAbiProviderTransactionInfo
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
import io.mockk.slot
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
    private lateinit var executionContextResolver: WalletAbiExecutionContextResolving
    private lateinit var providerRunner: WalletAbiProviderRunning
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
                    "00141111111111111111111111111111111111111111" -> "tlq1qqd7g4r0n7px6x7m8g7slt0g2j5g6gf8x4v0tpn"
                    else -> null
                }
            }
        )
        reviewPreviewer = WalletAbiReviewPreviewer { _, plan, _ ->
            preparedExecution(plan)
        }
        executionContextResolver = WalletAbiExecutionContextResolver()
        providerRunner = object : WalletAbiProviderRunning {
            override suspend fun run(
                context: WalletAbiExecutionContext,
                request: com.blockstream.domain.walletabi.request.WalletAbiTxCreateRequest
            ): com.blockstream.domain.walletabi.provider.WalletAbiProviderRunResult {
                error("Provider runner not configured for this test")
            }
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
    fun resume_launch_loads_snapshot_instead_of_demo_request() = runTest(dispatcher) {
        val snapshot = demoResumeSnapshot(phase = WalletAbiResumePhase.REQUEST_LOADED)
        coEvery { snapshotRepository.load(greenWallet.id) } returns snapshot
        requestSource = DefaultWalletAbiDemoRequestSource { _ ->
            error("Resume launch should not read the demo request source")
        }

        createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            requestSource = requestSource,
            launchMode = WalletAbiFlowLaunchMode.Resume,
            driver = driver
        )

        advanceUntilIdle()

        val intent = assertIs<WalletAbiFlowIntent.Restore>(store.intents.single())
        assertEquals(WalletAbiResumePhase.REQUEST_LOADED, intent.snapshot.phase)
        assertEquals(greenWallet.id, intent.snapshot.review.requestContext.walletId)
        assertEquals(WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID, intent.snapshot.review.requestContext.requestId)
        assertEquals("wallet_abi_process_request", intent.snapshot.review.method)
    }

    @Test
    fun resume_request_loaded_snapshot_rebuilds_review_and_enters_resumable() = runTest(dispatcher) {
        val realStore = DefaultWalletAbiFlowStore()
        val snapshot = demoResumeSnapshot(phase = WalletAbiResumePhase.REQUEST_LOADED)
        coEvery { snapshotRepository.load(greenWallet.id) } returns snapshot

        val viewModel = createViewModel(
            greenWallet = greenWallet,
            store = realStore,
            snapshotRepository = snapshotRepository,
            launchMode = WalletAbiFlowLaunchMode.Resume,
            driver = driver
        )

        advanceUntilIdle()

        val state = assertIs<WalletAbiFlowState.Resumable>(viewModel.state.value)
        assertEquals(WalletAbiResumePhase.REQUEST_LOADED, state.snapshot.phase)
        assertEquals(greenWallet.id, state.snapshot.review.requestContext.walletId)
        assertEquals(WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID, state.snapshot.review.requestContext.requestId)
        assertEquals("wallet_abi_process_request", state.snapshot.review.method)
        assertEquals(liquidAccount.id, state.snapshot.review.selectedAccountId)
        assertEquals("tlq1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3l4q9m", state.snapshot.review.executionDetails?.destinationAddress)
        assertEquals(3_000L, state.snapshot.review.executionDetails?.amountSat)
        assertEquals("wallet_abi_process_request", viewModel.reviewLook.value?.method)
        assertEquals(2, viewModel.reviewLook.value?.outputs?.size)
    }

    @Test
    fun resume_submitting_snapshot_restores_partial_completion_error() = runTest(dispatcher) {
        val realStore = DefaultWalletAbiFlowStore()
        coEvery { snapshotRepository.load(greenWallet.id) } returns demoResumeSnapshot(
            phase = WalletAbiResumePhase.SUBMITTING
        )

        val viewModel = createViewModel(
            greenWallet = greenWallet,
            store = realStore,
            snapshotRepository = snapshotRepository,
            launchMode = WalletAbiFlowLaunchMode.Resume,
            driver = driver
        )

        advanceUntilIdle()

        val state = assertIs<WalletAbiFlowState.Error>(viewModel.state.value)
        assertEquals(WalletAbiFlowErrorKind.PARTIAL_COMPLETION, state.error.kind)
        assertEquals(WalletAbiFlowPhase.SUBMISSION, state.error.phase)
        assertEquals(false, state.error.retryable)
    }

    @Test
    fun resume_awaiting_approval_snapshot_restarts_approval_handling() = runTest(dispatcher) {
        val realStore = DefaultWalletAbiFlowStore()
        coEvery { snapshotRepository.load(greenWallet.id) } returns demoResumeSnapshot(
            phase = WalletAbiResumePhase.AWAITING_APPROVAL
        ).copy(
            jade = com.blockstream.domain.walletabi.flow.WalletAbiJadeContext(
                deviceId = "jade-id",
                step = com.blockstream.domain.walletabi.flow.WalletAbiJadeStep.CONNECT,
                message = null,
                retryable = false
            )
        )

        val viewModel = createViewModel(
            greenWallet = greenWallet,
            store = realStore,
            snapshotRepository = snapshotRepository,
            launchMode = WalletAbiFlowLaunchMode.Resume,
            approvalTimeoutMillis = 1L,
            driver = driver
        )

        advanceUntilIdle()
        viewModel.dispatch(WalletAbiFlowIntent.Resume)
        advanceTimeBy(1L)
        advanceUntilIdle()

        val state = assertIs<WalletAbiFlowState.Error>(viewModel.state.value)
        assertEquals(WalletAbiFlowErrorKind.TIMEOUT, state.error.kind)
        assertEquals(WalletAbiFlowPhase.APPROVAL, state.error.phase)
    }

    @Test
    fun resume_without_snapshot_navigates_back() = runTest(dispatcher) {
        coEvery { snapshotRepository.load(greenWallet.id) } returns null
        val viewModel = createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            launchMode = WalletAbiFlowLaunchMode.Resume,
            driver = driver
        )
        val sideEffect = async { viewModel.sideEffect.first() }

        advanceUntilIdle()

        assertEquals(SideEffects.NavigateBack(), sideEffect.await())
        assertEquals(emptyList(), store.intents)
    }

    @Test
    fun resume_rebuild_failure_clears_snapshot_and_dispatches_non_retryable_error() = runTest(dispatcher) {
        val parsedRequest = assertIs<WalletAbiParsedRequest.TxCreate>(
            demoResumeSnapshot(WalletAbiResumePhase.REQUEST_LOADED).review.parsedRequest
        )
        val unsupportedSnapshot = demoResumeSnapshot(
            phase = WalletAbiResumePhase.REQUEST_LOADED,
            parsedRequest = parsedRequest.copy(
                request = parsedRequest.request.copy(
                    params = parsedRequest.request.params.copy(
                        outputs = listOf(
                            parsedRequest.request.params.outputs.first(),
                            parsedRequest.request.params.outputs.last().copy(
                                asset = kotlinx.serialization.json.buildJsonObject {
                                    put("asset_id", kotlinx.serialization.json.JsonPrimitive("unsupported-asset"))
                                }
                            )
                        )
                    )
                )
            )
        )
        coEvery { snapshotRepository.load(greenWallet.id) } returns unsupportedSnapshot

        createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            launchMode = WalletAbiFlowLaunchMode.Resume,
            driver = driver
        )

        advanceUntilIdle()

        coVerify(exactly = 1) { snapshotRepository.clear(greenWallet.id) }
        val intent = assertIs<WalletAbiFlowIntent.OnExecutionEvent>(store.intents.last())
        val error = assertIs<WalletAbiExecutionEvent.Failed>(intent.event).error
        assertEquals(false, error.retryable)
        assertEquals(WalletAbiFlowPhase.LOADING, error.phase)
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
        assertEquals(2, parsedRequest.params.outputs.size)
        assertEquals(12_000f, parsedRequest.params.feeRateSatKvb)
        assertEquals(true, parsedRequest.broadcast)
        assertEquals("tlq1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3l4q9m", review.executionDetails?.destinationAddress)
        assertEquals(3_000L, review.executionDetails?.amountSat)
        assertEquals(TESTNET_POLICY_ASSET, review.executionDetails?.assetId)
        assertEquals(WalletAbiNetwork.TESTNET_LIQUID.wireValue, review.executionDetails?.network)
        assertEquals(12_000L, review.executionDetails?.feeRate)

        assertEquals("wallet_abi_process_request", viewModel.reviewLook.value?.method)
        assertEquals("wallet-abi-0.1", viewModel.reviewLook.value?.abiVersion)
        assertEquals(2, viewModel.reviewLook.value?.outputs?.size)
        assertEquals("1,000 TEST-LBTC", viewModel.reviewLook.value?.outputs?.firstOrNull()?.amount)
        assertEquals("0.01 TEST-LBTC", viewModel.reviewLook.value?.transactionConfirmation?.fee)
    }

    @Test
    fun loadRequest_output_dispatches_unresolved_issuance_review() = runTest(dispatcher) {
        requestSource = WalletAbiDemoRequestSource { issuanceEnvelope(it) }

        val viewModel = createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            requestSource = requestSource
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
        assertEquals(WalletAbiRequestFamily.ISSUANCE, review.executionDetails?.requestFamily)
        assertEquals(WalletAbiResolutionState.REQUIRED, review.executionDetails?.resolutionState)
        assertEquals(2, review.executionDetails?.outputCount)
        assertEquals("Wallet ABI issuance", review.title)
        assertEquals(true, viewModel.reviewLook.value?.canResolve)
        assertEquals(false, viewModel.reviewLook.value?.canApprove)
        assertEquals(2, viewModel.reviewLook.value?.outputs?.size)
    }

    @Test
    fun startResolution_output_dispatches_ready_issuance_review() = runTest(dispatcher) {
        requestSource = WalletAbiDemoRequestSource { issuanceEnvelope(it) }
        providerRunner = fakeProviderRunner(
            requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
            network = WalletAbiNetwork.TESTNET_LIQUID,
            preview = issuancePreview(),
            txHex = "issuance-tx-hex",
            txid = "issuance-txid"
        )

        val viewModel = createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            requestSource = requestSource,
            providerRunner = providerRunner
        )

        advanceUntilIdle()

        val requestContext = WalletAbiStartRequestContext(
            requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
            walletId = greenWallet.id
        )
        store.mutableOutputs.emit(WalletAbiFlowOutput.LoadRequest(requestContext))
        advanceUntilIdle()
        store.mutableOutputs.emit(
            WalletAbiFlowOutput.StartResolution(
                WalletAbiResolutionCommand(
                    requestContext = requestContext,
                    selectedAccountId = liquidAccount.id
                )
            )
        )
        advanceUntilIdle()

        val intent = assertIs<WalletAbiFlowIntent.OnExecutionEvent>(store.intents.last())
        val review = assertIs<WalletAbiExecutionEvent.Resolved>(intent.event).review
        assertEquals(WalletAbiRequestFamily.ISSUANCE, review.executionDetails?.requestFamily)
        assertEquals(WalletAbiResolutionState.READY, review.executionDetails?.resolutionState)
        assertEquals(false, viewModel.reviewLook.value?.canResolve)
        assertEquals(true, viewModel.reviewLook.value?.canApprove)
        assertEquals(3, viewModel.reviewLook.value?.assetImpacts?.size)
        assertEquals("issuance_asset_id", viewModel.reviewLook.value?.outputs?.firstOrNull()?.assetId)
    }

    @Test
    fun approve_after_resolving_issuance_broadcasts_provider_transaction() = runTest(dispatcher) {
        requestSource = WalletAbiDemoRequestSource { issuanceEnvelope(it) }
        providerRunner = fakeProviderRunner(
            requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
            network = WalletAbiNetwork.TESTNET_LIQUID,
            preview = issuancePreview(),
            txHex = "provider-issuance-tx-hex",
            txid = "provider-issuance-txid"
        )
        val realStore = DefaultWalletAbiFlowStore()
        val broadcastParams = slot<BroadcastTransactionParams>()
        coEvery {
            walletSession.broadcastTransaction(any(), capture(broadcastParams))
        } returns ProcessedTransactionDetails(txHash = "provider-issuance-broadcast")

        val viewModel = createViewModel(
            greenWallet = greenWallet,
            launchMode = WalletAbiFlowLaunchMode.Demo,
            store = realStore,
            snapshotRepository = snapshotRepository,
            walletSession = walletSession,
            requestSource = requestSource,
            executionPlanner = executionPlanner,
            executionRunner = executionRunner,
            reviewPreviewer = reviewPreviewer,
            executionContextResolver = executionContextResolver,
            providerRunner = providerRunner
        )

        advanceUntilIdle()

        assertIs<WalletAbiFlowState.RequestLoaded>(viewModel.state.value)
        viewModel.dispatch(WalletAbiFlowIntent.ResolveRequest)
        advanceUntilIdle()
        viewModel.dispatch(WalletAbiFlowIntent.Approve)
        advanceUntilIdle()

        val success = assertIs<WalletAbiFlowState.Success>(viewModel.state.value)
        assertEquals("provider-issuance-broadcast", success.result.txHash)
        assertEquals("provider-issuance-tx-hex", broadcastParams.captured.transaction)
    }

    @Test
    fun loadRequest_output_dispatches_unresolved_reissuance_review() = runTest(dispatcher) {
        requestSource = WalletAbiDemoRequestSource {
            reissuanceEnvelope(
                requestId = it,
                tokenAssetId = "reissuance_token_asset"
            )
        }

        createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            requestSource = requestSource
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
        assertEquals(WalletAbiRequestFamily.REISSUANCE, review.executionDetails?.requestFamily)
        assertEquals(WalletAbiResolutionState.REQUIRED, review.executionDetails?.resolutionState)
        assertEquals(true, viewModelOrCreateLatest().reviewLook.value?.warnings?.any {
            it.contains("reissuance", ignoreCase = true) || it.contains("token asset", ignoreCase = true)
        })
    }

    @Test
    fun startResolution_output_dispatches_ready_reissuance_review() = runTest(dispatcher) {
        requestSource = WalletAbiDemoRequestSource {
            reissuanceEnvelope(
                requestId = it,
                tokenAssetId = "reissuance_token_asset"
            )
        }
        providerRunner = fakeProviderRunner(
            requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
            network = WalletAbiNetwork.TESTNET_LIQUID,
            preview = reissuancePreview(),
            txHex = "reissuance-tx-hex",
            txid = "reissuance-txid"
        )

        val viewModel = createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            requestSource = requestSource,
            providerRunner = providerRunner
        )

        advanceUntilIdle()

        val requestContext = WalletAbiStartRequestContext(
            requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
            walletId = greenWallet.id
        )
        store.mutableOutputs.emit(WalletAbiFlowOutput.LoadRequest(requestContext))
        advanceUntilIdle()

        store.mutableOutputs.emit(
            WalletAbiFlowOutput.StartResolution(
                WalletAbiResolutionCommand(
                    requestContext = requestContext,
                    selectedAccountId = liquidAccount.id
                )
            )
        )
        advanceUntilIdle()

        val intent = assertIs<WalletAbiFlowIntent.OnExecutionEvent>(store.intents.last())
        val review = assertIs<WalletAbiExecutionEvent.Resolved>(intent.event).review
        assertEquals(WalletAbiRequestFamily.REISSUANCE, review.executionDetails?.requestFamily)
        assertEquals(WalletAbiResolutionState.READY, review.executionDetails?.resolutionState)
        assertEquals(false, viewModel.reviewLook.value?.canResolve)
        assertEquals(true, viewModel.reviewLook.value?.canApprove)
        assertEquals(2, viewModel.reviewLook.value?.assetImpacts?.size)
        assertEquals("reissued_asset_id", viewModel.reviewLook.value?.outputs?.firstOrNull()?.assetId)
    }

    @Test
    fun approve_after_resolving_reissuance_broadcasts_provider_transaction() = runTest(dispatcher) {
        requestSource = WalletAbiDemoRequestSource {
            reissuanceEnvelope(
                requestId = it,
                tokenAssetId = "reissuance_token_asset"
            )
        }
        providerRunner = fakeProviderRunner(
            requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
            network = WalletAbiNetwork.TESTNET_LIQUID,
            preview = reissuancePreview(),
            txHex = "provider-reissuance-tx-hex",
            txid = "provider-reissuance-txid"
        )
        val realStore = DefaultWalletAbiFlowStore()
        val broadcastParams = slot<BroadcastTransactionParams>()
        coEvery {
            walletSession.broadcastTransaction(any(), capture(broadcastParams))
        } returns ProcessedTransactionDetails(txHash = "provider-reissuance-broadcast")

        val viewModel = createViewModel(
            greenWallet = greenWallet,
            launchMode = WalletAbiFlowLaunchMode.Demo,
            store = realStore,
            snapshotRepository = snapshotRepository,
            walletSession = walletSession,
            requestSource = requestSource,
            executionPlanner = executionPlanner,
            executionRunner = executionRunner,
            reviewPreviewer = reviewPreviewer,
            executionContextResolver = executionContextResolver,
            providerRunner = providerRunner
        )

        advanceUntilIdle()

        assertIs<WalletAbiFlowState.RequestLoaded>(viewModel.state.value)
        viewModel.dispatch(WalletAbiFlowIntent.ResolveRequest)
        advanceUntilIdle()
        viewModel.dispatch(WalletAbiFlowIntent.Approve)
        advanceUntilIdle()

        val success = assertIs<WalletAbiFlowState.Success>(viewModel.state.value)
        assertEquals("provider-reissuance-broadcast", success.result.txHash)
        assertEquals("provider-reissuance-tx-hex", broadcastParams.captured.transaction)
    }

    @Test
    fun resume_ready_issuance_snapshot_rebuilds_exact_review() = runTest(dispatcher) {
        requestSource = WalletAbiDemoRequestSource { error("Resume should not read the demo request source") }
        providerRunner = fakeProviderRunner(
            requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
            network = WalletAbiNetwork.TESTNET_LIQUID,
            preview = issuancePreview(),
            txHex = "issuance-ready-tx-hex",
            txid = "issuance-ready-txid"
        )
        val snapshot = demoResumeSnapshot(
            phase = WalletAbiResumePhase.REQUEST_LOADED,
            parsedRequest = assertIs<WalletAbiParsedRequest>(
                assertIs<WalletAbiRequestParseResult.Success>(
                    DefaultWalletAbiRequestParser().parse(
                        issuanceEnvelope(WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID)
                    )
                ).envelope.request
            )
        ).copy(
            review = demoResumeSnapshot(
                phase = WalletAbiResumePhase.REQUEST_LOADED,
                parsedRequest = assertIs<WalletAbiRequestParseResult.Success>(
                    DefaultWalletAbiRequestParser().parse(
                        issuanceEnvelope(WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID)
                    )
                ).envelope.request
            ).review.copy(
                method = "wallet_abi_process_request",
                executionDetails = WalletAbiExecutionDetails(
                    destinationAddress = "Issued asset to wallet",
                    amountSat = 6L,
                    assetId = "unresolved",
                    network = WalletAbiNetwork.TESTNET_LIQUID.wireValue,
                    requestFamily = WalletAbiRequestFamily.ISSUANCE,
                    resolutionState = WalletAbiResolutionState.READY,
                    outputCount = 2
                )
            )
        )
        coEvery { snapshotRepository.load(greenWallet.id) } returns snapshot

        val viewModel = createViewModel(
            greenWallet = greenWallet,
            store = DefaultWalletAbiFlowStore(),
            snapshotRepository = snapshotRepository,
            requestSource = requestSource,
            launchMode = WalletAbiFlowLaunchMode.Resume,
            providerRunner = providerRunner
        )

        advanceUntilIdle()

        val state = assertIs<WalletAbiFlowState.Resumable>(viewModel.state.value)
        assertEquals(WalletAbiResolutionState.READY, state.snapshot.review.executionDetails?.resolutionState)
        assertEquals(true, viewModel.reviewLook.value?.canApprove)
        assertEquals(false, viewModel.reviewLook.value?.canResolve)
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
    fun startApproval_output_dispatches_timeout_when_approval_exceeds_deadline() = runTest(dispatcher) {
        createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            approvalTimeoutMillis = 1L,
            driver = driver
        )
        advanceUntilIdle()

        store.mutableOutputs.emit(
            WalletAbiFlowOutput.StartApproval(
                com.blockstream.domain.walletabi.flow.WalletAbiApprovalCommand(
                    requestContext = WalletAbiStartRequestContext(
                        requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                        walletId = greenWallet.id
                    ),
                    selectedAccountId = liquidAccount.id,
                    jade = com.blockstream.domain.walletabi.flow.WalletAbiJadeContext(
                        deviceId = "jade-id",
                        step = com.blockstream.domain.walletabi.flow.WalletAbiJadeStep.CONNECT,
                        message = null,
                        retryable = false
                    )
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
                        phase = WalletAbiFlowPhase.APPROVAL,
                        message = "Wallet ABI approval timed out",
                        retryable = true
                    )
                )
            ),
            store.intents.last()
        )
    }

    @Test
    fun cancelActiveWork_approval_dispatches_user_cancelled() = runTest(dispatcher) {
        createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            driver = driver
        )
        advanceUntilIdle()

        store.mutableOutputs.emit(
            WalletAbiFlowOutput.StartApproval(
                com.blockstream.domain.walletabi.flow.WalletAbiApprovalCommand(
                    requestContext = WalletAbiStartRequestContext(
                        requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                        walletId = greenWallet.id
                    ),
                    selectedAccountId = liquidAccount.id,
                    jade = com.blockstream.domain.walletabi.flow.WalletAbiJadeContext(
                        deviceId = "jade-id",
                        step = com.blockstream.domain.walletabi.flow.WalletAbiJadeStep.CONNECT,
                        message = null,
                        retryable = false
                    )
                )
            )
        )
        advanceUntilIdle()

        store.mutableOutputs.emit(WalletAbiFlowOutput.CancelActiveWork(WalletAbiFlowPhase.APPROVAL))
        advanceUntilIdle()

        assertEquals(
            WalletAbiExecutionEvent.Cancelled(WalletAbiCancelledReason.UserCancelled),
            (store.intents.last { intent ->
                intent is WalletAbiFlowIntent.OnExecutionEvent
            } as WalletAbiFlowIntent.OnExecutionEvent).event
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
        assertEquals(3_000L, review.executionDetails?.amountSat)
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
    fun startSubmission_output_dispatches_network_failure_when_broadcast_transport_fails() = runTest(dispatcher) {
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
                error("network transport failed")
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
                        kind = WalletAbiFlowErrorKind.NETWORK_FAILURE,
                        phase = WalletAbiFlowPhase.SUBMISSION,
                        message = "network transport failed",
                        retryable = true
                    )
                )
            ),
            store.intents.last()
        )
    }

    @Test
    fun startSubmission_output_dispatches_timeout_when_prepare_exceeds_deadline() = runTest(dispatcher) {
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
            submissionTimeoutMillis = 1L,
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

        advanceTimeBy(1L)
        advanceUntilIdle()

        assertEquals(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.Failed(
                    WalletAbiFlowError(
                        kind = WalletAbiFlowErrorKind.TIMEOUT,
                        phase = WalletAbiFlowPhase.SUBMISSION,
                        message = "Wallet ABI submission timed out",
                        retryable = true
                    )
                )
            ),
            store.intents.last()
        )
    }

    @Test
    fun startSubmission_output_dispatches_partial_completion_when_broadcast_exceeds_deadline() = runTest(dispatcher) {
        val broadcastGate = CompletableDeferred<Unit>()
        val blockingRunner = object : WalletAbiExecutionRunner {
            override suspend fun prepare(
                session: GdkSession,
                preparedExecution: WalletAbiPreparedExecution
            ): WalletAbiPreparedBroadcast {
                return WalletAbiPreparedBroadcast(
                    preparedExecution = preparedExecution,
                    preparedTransaction = PreparedSoftwareTransaction(
                        transaction = preparedExecution.transaction,
                        signedTransaction = JsonObject(emptyMap())
                    )
                )
            }

            override suspend fun broadcast(
                session: GdkSession,
                preparedBroadcast: WalletAbiPreparedBroadcast,
                twoFactorResolver: com.blockstream.data.gdk.TwoFactorResolver
            ): WalletAbiExecutionResult {
                broadcastGate.await()
                return WalletAbiExecutionResult(txHash = "wallet-abi-demo-tx-hash")
            }
        }

        createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            executionRunner = blockingRunner,
            submissionTimeoutMillis = 1L,
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

        advanceTimeBy(1L)
        advanceUntilIdle()

        assertEquals(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.Failed(
                    WalletAbiFlowError(
                        kind = WalletAbiFlowErrorKind.PARTIAL_COMPLETION,
                        phase = WalletAbiFlowPhase.SUBMISSION,
                        message = "Transaction status may already have changed. Check your wallet activity before retrying.",
                        retryable = false
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
        launchMode: WalletAbiFlowLaunchMode = WalletAbiFlowLaunchMode.Demo,
        store: WalletAbiFlowStore = this.store,
        snapshotRepository: WalletAbiFlowSnapshotRepository = this.snapshotRepository,
        walletSession: GdkSession = this.walletSession,
        requestSource: WalletAbiDemoRequestSource = this.requestSource,
        executionPlanner: WalletAbiExecutionPlanner = this.executionPlanner,
        executionRunner: WalletAbiExecutionRunner = this.executionRunner,
        reviewPreviewer: WalletAbiReviewPreviewer = this.reviewPreviewer,
        executionContextResolver: WalletAbiExecutionContextResolving = this.executionContextResolver,
        providerRunner: WalletAbiProviderRunning = this.providerRunner,
        loadingTimeoutMillis: Long = 15_000L,
        approvalTimeoutMillis: Long = 120_000L,
        submissionTimeoutMillis: Long = 60_000L,
        driver: FakeWalletAbiFlowDriver = this.driver
    ): WalletAbiFlowRouteViewModel {
        return WalletAbiFlowRouteViewModel(
            greenWallet = greenWallet,
            launchMode = launchMode,
            store = store,
            snapshotRepository = snapshotRepository,
            walletSession = walletSession,
            requestSource = requestSource,
            executionPlanner = executionPlanner,
            executionRunner = executionRunner,
            reviewPreviewer = reviewPreviewer,
            executionContextResolver = executionContextResolver,
            providerRunner = providerRunner,
            loadingTimeoutMillis = loadingTimeoutMillis,
            approvalTimeoutMillis = approvalTimeoutMillis,
            submissionTimeoutMillis = submissionTimeoutMillis
        ).also { createdViewModels += it }
    }

    private fun demoResumeSnapshot(
        phase: WalletAbiResumePhase,
        parsedRequest: WalletAbiParsedRequest = defaultParsedRequest(),
        method: String = "wallet_abi_process_request"
    ): WalletAbiResumeSnapshot {
        return WalletAbiResumeSnapshot(
            review = WalletAbiFlowReview(
                requestContext = WalletAbiStartRequestContext(
                    requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                    walletId = greenWallet.id
                ),
                method = method,
                title = "Wallet ABI payment",
                message = "Approve a Wallet ABI request",
                accounts = listOf(
                    WalletAbiAccountOption(
                        accountId = liquidAccount.id,
                        name = liquidAccount.name
                    )
                ),
                selectedAccountId = liquidAccount.id,
                approvalTarget = WalletAbiApprovalTarget.Software,
                parsedRequest = parsedRequest
            ),
            phase = phase
        )
    }

    private fun defaultParsedRequest(): WalletAbiParsedRequest {
        val envelope = assertIs<WalletAbiRequestParseResult.Success>(
            DefaultWalletAbiRequestParser().parse(
                requestSource.loadRequestEnvelope(WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID)
            )
        ).envelope
        return envelope.request
    }

    private fun viewModelOrCreateLatest(): WalletAbiFlowRouteViewModel {
        return createdViewModels.last()
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
            ): com.blockstream.domain.walletabi.provider.WalletAbiProviderRunResult {
                return com.blockstream.domain.walletabi.provider.WalletAbiProviderRunResult(
                    response = WalletAbiProviderProcessResponse(
                        abiVersion = "wallet-abi-0.1",
                        requestId = requestId,
                        network = network,
                        status = WalletAbiProviderStatus.OK,
                        transaction = WalletAbiProviderTransactionInfo(
                            txHex = txHex,
                            txid = txid
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
                    responseJson = "{}"
                )
            }
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

    private fun preparedExecution(plan: com.blockstream.domain.walletabi.execution.WalletAbiExecutionPlan): WalletAbiPreparedExecution {
        return WalletAbiPreparedExecution(
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
