package com.blockstream.compose.models.walletabi

import com.blockstream.compose.extensions.previewWallet
import com.blockstream.data.CountlyBase
import com.blockstream.data.banner.Banner
import com.blockstream.data.config.AppInfo
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.database.Database
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.managers.PromoManager
import com.blockstream.data.managers.SessionManager
import com.blockstream.data.managers.SettingsManager
import com.blockstream.domain.banner.GetBannerUseCase
import com.blockstream.domain.promo.GetPromoUseCase
import com.blockstream.domain.walletabi.flow.WalletAbiFlowIntent
import com.blockstream.domain.walletabi.flow.WalletAbiFlowOutput
import com.blockstream.domain.walletabi.flow.WalletAbiFlowState
import com.blockstream.domain.walletabi.flow.WalletAbiFlowStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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

class WalletAbiFlowRouteViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val greenWallet = previewWallet()
    private val store = FakeWalletAbiFlowStore()

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
            store = store
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
    fun state_exposes_store_state() {
        val viewModel = WalletAbiFlowRouteViewModel(
            greenWallet = greenWallet,
            store = store
        )

        assertEquals(
            WalletAbiFlowState.Idle,
            viewModel.state.value
        )
    }

    private class FakeWalletAbiFlowStore : WalletAbiFlowStore {
        override val state = MutableStateFlow<WalletAbiFlowState>(WalletAbiFlowState.Idle)
        override val outputs: Flow<WalletAbiFlowOutput> = emptyFlow()
        val intents = mutableListOf<WalletAbiFlowIntent>()

        override suspend fun dispatch(intent: WalletAbiFlowIntent) {
            intents += intent
        }
    }
}
