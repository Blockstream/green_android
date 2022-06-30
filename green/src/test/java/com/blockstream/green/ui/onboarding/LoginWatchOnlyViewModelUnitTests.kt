package com.blockstream.green.ui.onboarding

import androidx.lifecycle.Observer
import com.blockstream.gdk.data.LoginData
import com.blockstream.gdk.data.Network
import com.blockstream.green.TestViewModel
import com.blockstream.green.data.AppEvent
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.utils.ConsumableEvent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*

@RunWith(MockitoJUnitRunner::class)
class LoginWatchOnlyViewModelUnitTests : TestViewModel<LoginWatchOnlyViewModel>() {

    @Mock
    private lateinit var sessionManager: SessionManager

    @Mock
    private lateinit var isLoginEnabledObserver: Observer<Boolean>

    @Mock
    private lateinit var errorObserver: Observer<ConsumableEvent<Throwable>>

    @Mock
    private lateinit var newWalletObserver: Observer<ConsumableEvent<AppEvent>>

    @Before
    override fun setup() {
        super.setup()

        val network = Network(
            "testnet",
            "Testnet",
            "testnet",
            false,
            false,
            true
        )

        whenever(greenSession.countly).thenReturn(countly)
        whenever(greenSession.network).thenReturn(network)
        whenever(sessionManager.getOnBoardingSession(anyOrNull())).thenReturn(greenSession)

        viewModel = LoginWatchOnlyViewModel(
            walletRepository = mock(),
            sessionManager = sessionManager,
            appKeystore = mock(),
            countly = countly,
            onboardingOptions = OnboardingOptions(isRestoreFlow = true, isWatchOnly = true, isSinglesig = false, network = network)
        )
        viewModel.isLoginEnabled.observeForever(isLoginEnabledObserver)
        viewModel.onEvent.observeForever(newWalletObserver)
        viewModel.onError.observeForever(errorObserver)
    }

    @Test
    fun login_is_disabled_with_no_inputs() {
        checkLoginDisabled()
    }

    @Test
    fun login_is_disabled_with_username() {
        viewModel.username.value = "username"
        checkLoginDisabled()
    }

    @Test
    fun login_is_disabled_with_password() {
        viewModel.password.value = "password"
        checkLoginDisabled()
    }

    private fun checkLoginDisabled() {
        verify(isLoginEnabledObserver, atLeastOnce()).onChanged(eq(false))
        verify(isLoginEnabledObserver, never()).onChanged(eq(true))
    }

    @Test
    fun login_is_enabled_with_input() {
        viewModel.username.value = "username"
        viewModel.password.value = "password"

        verify(isLoginEnabledObserver).onChanged(eq(true))
    }

    @Test
    fun test_error_with_wrong_credentials() {
        mockSession(false)

        viewModel.username.value = "username"
        viewModel.password.value = "password"

        viewModel.createNewWatchOnlyWallet()

        verify(newWalletObserver, never()).onChanged(eq(null))
        verify(errorObserver).onChanged(argThat {
            this.peekContent().message == "-1"
        })
    }

    @Test
    fun test_successful_login() {
        mockSession(true)

        viewModel.username.value = "username"
        viewModel.password.value = "password"

        viewModel.createNewWatchOnlyWallet()

        verify(newWalletObserver).onChanged(notNull())
        verify(errorObserver, never()).onChanged(any())
    }

    private fun mockSession(isSuccess: Boolean) {
        if (isSuccess) {
            whenever(greenSession.loginWatchOnly(any<Network>(), any(), any())).then {
                LoginData("")
            }
        }else{
            whenever(greenSession.loginWatchOnly(any<Network>(), any(), any())).then {
                throw Exception("-1")
            }
        }
    }

}