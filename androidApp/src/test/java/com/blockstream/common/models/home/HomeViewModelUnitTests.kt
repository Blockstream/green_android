package com.blockstream.common.models.home

import com.blockstream.common.database.Database
import com.blockstream.common.managers.SessionManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.compose.models.home.HomeViewModel
import com.blockstream.green.TestViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.get
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class HomeViewModelUnitTests : TestViewModel<HomeViewModel>() {
    private fun init(walletExists: Boolean = false) = scope.runTest {

        get<SettingsManager>().also {
            every { it.isDeviceTermsAccepted() } returns walletExists
            every { it.isV5UpgradedFlow() } returns flowOf(true)
        }

        get<SessionManager>().also {
            every { it.getEphemeralWalletSession(any(), any()) } returns mockk()
            every { it.ephemeralWallets } returns mockk()
            every { it.hardwareWallets } returns mockk()
        }

        get<Database>().also {
            every { it.walletsExistsFlow() } returns flowOf(walletExists)
            every { it.getWalletsFlow(any(), any()) } returns flowOf(mockk())
            every { it.getAllWalletsFlow() } returns flowOf(mockk())
            coEvery { it.walletsExists() } returns walletExists
        }

        viewModel = HomeViewModel()
    }

    @Test
    fun test_initial_value() {
        init()
    }
}