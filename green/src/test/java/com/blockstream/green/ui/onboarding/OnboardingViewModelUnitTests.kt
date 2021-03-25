package com.blockstream.green.ui.onboarding

import androidx.lifecycle.Observer
import com.blockstream.green.TestViewModel
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.gdk.SessionManager
import com.blockstream.gdk.data.Network
import com.nhaarman.mockitokotlin2.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class OnboardingViewModelUnitTests : TestViewModel<OnboardingViewModel>() {

    @Mock
    private lateinit var sessionManager: SessionManager

    @Mock
    private lateinit var walletRepository: WalletRepository

    @Mock
    private lateinit var session: GreenSession

    @Mock
    private lateinit var eventObserver: Observer<ConsumableEvent<Any>>

    @Mock
    private lateinit var errorObserver: Observer<ConsumableEvent<Throwable>>


    private var restoreWallet: Wallet = Wallet(name = "restore", network = "testnet")

    private val network = Network(
        "testnet",
        "Testnet",
        "testnet",
        false,
        false,
        true
    )

    @Before
    fun setup() {
        whenever(sessionManager.getOnBoardingSession(anyOrNull())).thenReturn(session)

        whenever(session.loginWithMnemonic(any(), any(), any())).thenAnswer {
            // value is same as requested key
            val recovery = it.arguments[1] as String
            if (recovery != "valid") {
                throw Exception("invalid recovery phrase")
            }
        }

        setupViewModel()
    }

    private fun setupViewModel(withRestoreWallet: Boolean = false) {
        viewModel = OnboardingViewModel(
            sessionManager,
            walletRepository,
            if (withRestoreWallet) restoreWallet else null
        )

        viewModel.onEvent.observeForever(eventObserver)
        viewModel.onError.observeForever(errorObserver)
    }

    @Test
    fun test_create_new_wallet() {
        val options = OnboardingOptions(isRestoreFlow = false, network = network)

        viewModel.createNewWallet(options, "123456", "")

        verify(eventObserver).onChanged(any())
        verify(errorObserver, never()).onChanged(any())
    }

    @Test
    fun test_valid_recovery_phrase() {
        viewModel.checkRecoveryPhrase(network, "valid", "")

        verify(eventObserver).onChanged(any())
        verify(errorObserver, never()).onChanged(any())
    }

    @Test
    fun test_invalid_recovery_phrase() {
        viewModel.checkRecoveryPhrase(network, "invalid", "")

        verify(eventObserver, never()).onChanged(any())
        verify(errorObserver).onChanged(any())
    }

    @Test
    fun test_restore_with_pin() {
        val options = OnboardingOptions(isRestoreFlow = true, network = network)

        viewModel.restoreWithPin(options, "123456")

        verify(walletRepository).addWallet(any())
        verify(walletRepository, never()).updateWalletSync(any())

        verify(eventObserver).onChanged(argThat {
            val wallet = this.peekContent() as Wallet
            true
        })
        verify(errorObserver, never()).onChanged(any())
    }

    @Test
    fun test_restore_wallet_with_recovery_phrase() {
        setupViewModel(withRestoreWallet = true)

        val options = OnboardingOptions(isRestoreFlow = true, network = network)

        viewModel.restoreWithPin(options, "123456")

        verify(walletRepository, never()).addWallet(any())
        verify(walletRepository).updateWalletSync(any())

        verify(eventObserver).onChanged(argThat {
            val wallet = this.peekContent() as Wallet
            true
        })
        verify(errorObserver, never()).onChanged(any())
    }

}