package com.blockstream.green

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.blockstream.common.CountlyBase
import com.blockstream.green.data.config.AppInfo
import com.blockstream.common.database.Database
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.managers.PromoManager
import com.blockstream.common.managers.SessionManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.models.GreenViewModel
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
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.mock.MockProvider
import org.koin.test.mock.declareMock
import org.mockito.Mock

@OptIn(ExperimentalCoroutinesApi::class)
open class TestViewModel<VM : GreenViewModel>: KoinTest {
    internal lateinit var viewModel : VM

    @get:Rule
    val taskExecutorRule = InstantTaskExecutorRule()

    @Mock
    protected lateinit var gdkSession: GdkSession

    @Mock
    protected lateinit var countly: CountlyBase

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

                    declareMock<CountlyBase>{
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