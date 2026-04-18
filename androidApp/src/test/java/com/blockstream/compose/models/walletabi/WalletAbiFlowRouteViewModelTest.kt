package com.blockstream.compose.models.walletabi

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
import com.blockstream.domain.walletabi.flow.WalletAbiStartRequestContext
import com.blockstream.domain.walletabi.flow.WalletAbiSubmissionCommand
import com.blockstream.domain.walletabi.flow.WalletAbiSuccessResult
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
    private val store = FakeWalletAbiFlowStore()
    private val snapshotRepository = mockk<WalletAbiFlowSnapshotRepository>(relaxed = true)
    private val driver = FakeWalletAbiFlowDriver()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

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
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun init_dispatches_start_for_demo_request() = runTest(dispatcher) {
        WalletAbiFlowRouteViewModel(
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
                ),
                WalletAbiFlowIntent.OnExecutionEvent(
                    WalletAbiExecutionEvent.RequestLoaded(
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
                )
            ),
            store.intents
        )
    }


    @Test
    fun approve_reaches_success_with_real_store() = runTest(dispatcher) {
        val realStore = DefaultWalletAbiFlowStore()
        val viewModel = WalletAbiFlowRouteViewModel(
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
        val viewModel = WalletAbiFlowRouteViewModel(
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
        WalletAbiFlowRouteViewModel(
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

        WalletAbiFlowRouteViewModel(
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
        WalletAbiFlowRouteViewModel(
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
        val viewModel = WalletAbiFlowRouteViewModel(
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
}
