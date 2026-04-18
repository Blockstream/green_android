package com.blockstream.compose.models.walletabi

import androidx.lifecycle.viewModelScope
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.CountlyBase
import com.blockstream.data.banner.Banner
import com.blockstream.data.config.AppInfo
import com.blockstream.data.database.Database
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.managers.PromoManager
import com.blockstream.data.managers.SessionManager
import com.blockstream.data.managers.SettingsManager
import com.blockstream.data.walletabi.flow.FakeWalletAbiFlowDriver
import com.blockstream.domain.banner.GetBannerUseCase
import com.blockstream.domain.promo.GetPromoUseCase
import com.blockstream.domain.walletabi.flow.WalletAbiAccountOption
import com.blockstream.domain.walletabi.flow.WalletAbiApprovalTarget
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
import com.blockstream.domain.walletabi.flow.WalletAbiStartRequestContext
import com.blockstream.domain.walletabi.flow.WalletAbiSubmissionCommand
import com.blockstream.domain.walletabi.flow.WalletAbiSuccessResult
import com.blockstream.domain.walletabi.request.WalletAbiNetwork
import com.blockstream.domain.walletabi.request.WalletAbiParsedRequest
import io.mockk.coVerify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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

class WalletAbiFlowRouteViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val greenWallet = previewWallet()
    private lateinit var store: FakeWalletAbiFlowStore
    private lateinit var snapshotRepository: WalletAbiFlowSnapshotRepository
    private lateinit var driver: FakeWalletAbiFlowDriver
    private val createdViewModels = mutableListOf<WalletAbiFlowRouteViewModel>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        store = FakeWalletAbiFlowStore()
        snapshotRepository = mockk(relaxed = true)
        driver = FakeWalletAbiFlowDriver()

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
        val gdkSession = mockk<GdkSession>(relaxed = true).also {
            every { it.accounts } returns MutableStateFlow(emptyList())
            every { it.isWatchOnly } returns MutableStateFlow(false)
            every { it.isConnected } returns false
            every { it.networkErrors } returns MutableSharedFlow()
        }
        val sessionManager = mockk<SessionManager>(relaxed = true).also {
            every { it.getWalletSessionOrNull(greenWallet) } returns gdkSession
            every { it.getWalletSessionOrCreate(greenWallet) } returns gdkSession
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
        assertEquals("Demo payment", review.title)
        assertEquals("Approve a fake Wallet ABI request", review.message)
        assertEquals("fake-account-1", review.selectedAccountId)
        assertEquals(WalletAbiApprovalTarget.Software, review.approvalTarget)
        assertEquals("wallet-abi-0.1", parsedRequest.abiVersion)
        assertEquals(WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID, parsedRequest.requestId)
        assertEquals(WalletAbiNetwork.TESTNET_LIQUID, parsedRequest.network)
        assertEquals(1, parsedRequest.params.inputs.size)
        assertEquals(1, parsedRequest.params.outputs.size)
        assertEquals(12.5f, parsedRequest.params.feeRateSatKvb)
        assertEquals(true, parsedRequest.broadcast)
    }

    @Test
    fun loadRequest_output_dispatches_error_for_malformed_request() = runTest(dispatcher) {
        driver = mockk {
            every { loadRequestEnvelope(any()) } returns "{"
        }

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

        assertEquals(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.Failed(
                    WalletAbiFlowError("Wallet ABI request envelope is malformed")
                )
            ),
            store.intents.last()
        )
    }

    @Test
    fun loadRequest_output_dispatches_error_for_unsupported_request() = runTest(dispatcher) {
        driver = FakeWalletAbiFlowDriver { _ ->
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
                    WalletAbiFlowError("Unsupported Wallet ABI method 'wallet_abi_get_account'")
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
        assertEquals("fake-account-1", state.review.selectedAccountId)
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
        assertEquals("wallet-abi-demo-response", state.result.responseId)
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
                        title = "Demo payment",
                        message = "Approve a fake Wallet ABI request",
                        accounts = listOf(
                            WalletAbiAccountOption(
                                accountId = "fake-account-1",
                                name = "Main account"
                            )
                        ),
                        selectedAccountId = "fake-account-1",
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
        store.state.value = WalletAbiFlowState.RequestLoaded(
            WalletAbiFlowReview(
                requestContext = WalletAbiStartRequestContext(
                    requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                    walletId = greenWallet.id
                ),
                title = "Demo payment",
                message = "Approve a fake Wallet ABI request",
                accounts = listOf(
                    WalletAbiAccountOption(
                        accountId = "fake-account-1",
                        name = "Main account"
                    )
                ),
                selectedAccountId = "fake-account-1",
                approvalTarget = WalletAbiApprovalTarget.Software
            )
        )

        createViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            driver = driver
        )

        advanceUntilIdle()

        store.mutableOutputs.emit(
            WalletAbiFlowOutput.StartResolution(
                WalletAbiResolutionCommand(
                    requestContext = WalletAbiStartRequestContext(
                        requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                        walletId = greenWallet.id
                    ),
                    selectedAccountId = "fake-account-1"
                )
            )
        )

        advanceUntilIdle()

        assertEquals(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.Resolved(
                    review = WalletAbiFlowReview(
                        requestContext = WalletAbiStartRequestContext(
                            requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                            walletId = greenWallet.id
                        ),
                        title = "Demo payment",
                        message = "Approve a fake Wallet ABI request",
                        accounts = listOf(
                            WalletAbiAccountOption(
                                accountId = "fake-account-1",
                                name = "Main account"
                            )
                        ),
                        selectedAccountId = "fake-account-1",
                        approvalTarget = WalletAbiApprovalTarget.Software
                    )
                )
            ),
            store.intents.last()
        )
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
            WalletAbiFlowOutput.StartSubmission(
                WalletAbiSubmissionCommand(
                    requestContext = WalletAbiStartRequestContext(
                        requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
                        walletId = greenWallet.id
                    ),
                    selectedAccountId = "fake-account-1"
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
                            responseId = "wallet-abi-demo-response"
                        )
                    )
                )
            ),
            store.intents.takeLast(3)
        )
    }

    @Test
    fun dismissTerminal_posts_navigate_back() = runTest(dispatcher) {
        store.state.value = WalletAbiFlowState.Success(
            WalletAbiSuccessResult(
                requestId = WalletAbiFlowRouteViewModel.DEMO_REQUEST_ID,
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
        driver: FakeWalletAbiFlowDriver = this.driver
    ): WalletAbiFlowRouteViewModel {
        return WalletAbiFlowRouteViewModel(
            greenWallet = greenWallet,
            store = store,
            snapshotRepository = snapshotRepository,
            driver = driver
        ).also { createdViewModels += it }
    }
}
