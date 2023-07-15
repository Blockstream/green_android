package com.blockstream.green.ui.onboarding

import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.models.onboarding.SetupNewWalletViewModel
import com.blockstream.green.TestViewModel
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.get
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class AddWalletViewModelUnitTests : TestViewModel<SetupNewWalletViewModel>(){
    private fun init(walletExists: Boolean = false) = runBlockingTest {

        get<SettingsManager>().also {
            every { it.isDeviceTermsAccepted() } returns walletExists
        }

        viewModel = SetupNewWalletViewModel()
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