package com.blockstream.green

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.data.CountlyBase
import com.blockstream.data.btcpricehistory.BitcoinPriceHistoryRepository
import com.blockstream.data.config.AppInfo
import com.blockstream.data.database.Database
import com.blockstream.data.managers.PromoManager
import com.blockstream.data.managers.SessionManager
import com.blockstream.data.managers.SettingsManager
import com.blockstream.domain.bitcoinpricehistory.BitcoinPriceHistoryCache
import com.blockstream.domain.bitcoinpricehistory.ObserveBitcoinPriceHistory
import com.blockstream.domain.promo.GetPromoUseCase
import com.blockstream.network.NetworkResponse
import com.blockstream.utils.Loggable.Companion.COMBINED_LOG_QUALIFIER
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.mock.MockProvider
import org.koin.test.mock.declareMock

@OptIn(ExperimentalCoroutinesApi::class)
open class TestViewModel<VM : GreenViewModel> : KoinTest {
    internal lateinit var viewModel: VM

    @get:Rule
    val taskExecutorRule = InstantTaskExecutorRule()

    protected val testDispatcher = UnconfinedTestDispatcher()

    protected val scope = TestScope(testDispatcher)

    @Before
    open fun setup() {
        Dispatchers.setMain(testDispatcher)

        MockProvider.register {
            // Your way to build a Mock here
            mockkClass(it)
        }

        startKoin {
            modules(
                module {
                    single { AppInfo("green_test", "1.0.0-test", true, true) }

                    single { GetPromoUseCase(get()) }

                    single {
                        ObserveBitcoinPriceHistory(
                            mockk<BitcoinPriceHistoryRepository> {
                                coEvery { getPriceHistory(any()) } returns NetworkResponse.Error(0, "test")
                            },
                            BitcoinPriceHistoryCache()
                        )
                    }

                    // Bucketed Loggables (e.g. Lightning) resolve their logger via this qualifier.
                    // Provide a no-op writer so tests don't depend on the file-logging module.
                    factory<Logger>(named(COMBINED_LOG_QUALIFIER)) { (tag: String) ->
                        Logger(StaticConfig(logWriterList = emptyList()), tag)
                    }

                    declareMock<CountlyBase> {
                        every { viewModel(any()) } returns Unit
                        every { remoteConfigUpdateEvent } returns MutableSharedFlow<Unit>()
                        every { updateRemoteConfig(any()) } returns Unit
                    }

                    declareMock<SettingsManager> {
                        every { isDeviceTermsAccepted() } returns false
                    }

                    declareMock<SessionManager> {
                        every { getOnBoardingSession() } returns mockk()
                        every { getWalletSessionOrOnboarding(any()) } returns mockk()
                        every { connectionChangeEvent } returns mockk()
                    }

                    declareMock<PromoManager> {
                        every { promos } returns MutableStateFlow(listOf())
                    }

                    declareMock<Database> {
                        
                    }
                }
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }
}
