package com.blockstream.common.models

import com.blockstream.common.CountlyBase
import com.blockstream.common.database.Database
import com.blockstream.common.managers.PromoManager
import com.blockstream.common.managers.SessionManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.green.data.config.AppInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.mock.MockProvider
import org.koin.test.mock.declareMock

@OptIn(ExperimentalCoroutinesApi::class)
abstract class TestViewModel<VM : GreenViewModel> : KoinTest {
    internal lateinit var viewModel: VM

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    open fun setupInternal() {
        Dispatchers.setMain(testDispatcher)

        MockProvider.register {
            // Your way to build a Mock here
            mockkClass(it)
        }

        startKoin {
            modules(
                module {
                    single { AppInfo(userAgent = "green_test", version = "1.0.0-test", isDebug = true, isDevelopment = true) }

                    declareMock<CountlyBase> {
                        every { viewModel(any()) } returns Unit
                        every { remoteConfigUpdateEvent } returns MutableSharedFlow<Unit>()
                    }

                    declareMock<SessionManager> {

                    }

                    declareMock<Database> {
                        coEvery { insertEvent(any()) } returns Unit
                    }

                    declareMock<SettingsManager> {
                        every { getCountlyDeviceId() } returns ""
                    }

                    declareMock<PromoManager> {
                        every { promos } returns MutableStateFlow(listOf())
                    }
                }
            )
        }

        setup()
    }

    abstract fun setup()

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }
}