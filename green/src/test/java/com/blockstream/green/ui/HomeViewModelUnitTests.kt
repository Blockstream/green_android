package com.blockstream.green.ui

import com.blockstream.common.database.Database
import com.blockstream.common.managers.SessionManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.models.home.HomeViewModel
import com.blockstream.green.TestViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.get
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class HomeViewModelUnitTests : TestViewModel<HomeViewModel>(){
    private fun init(walletExists: Boolean = false) = runBlockingTest {

        get<SettingsManager>().also {
            every { it.isDeviceTermsAccepted() } returns walletExists
        }

        get<SessionManager>().also {
            every { it.getEphemeralWalletSession(any(), any()) } returns mockk()
            every { it.ephemeralWallets } returns mockk()
            every { it.hardwareWallets } returns mockk()
        }

        get<Database>().also {
            every { it.walletsExistsFlow() } returns flowOf(walletExists)
            every { it.getWalletsFlow(any(), any()) } returns flowOf(mockk())
        }

        viewModel = HomeViewModel()
    }

    @Test
    fun test_initial_value() {
        init()

        assertFalse(viewModel.termsOfServiceIsChecked.value)
    }

    @Test
    fun test_when_checked() {
        init()

        viewModel.termsOfServiceIsChecked.value = true
        assertTrue(viewModel.termsOfServiceIsChecked.value)
    }

    @Test
    fun test_when_toggled() {
        init()

        viewModel.termsOfServiceIsChecked.value = true
        assertTrue(viewModel.termsOfServiceIsChecked.value)
        viewModel.termsOfServiceIsChecked.value = false
        assertFalse(viewModel.termsOfServiceIsChecked.value)
    }

    @Test
    fun whenWalletExists_termsShouldBeChecked() = runTest {
        init(true)
        assertTrue(viewModel.termsOfServiceIsChecked.value)
    }
}