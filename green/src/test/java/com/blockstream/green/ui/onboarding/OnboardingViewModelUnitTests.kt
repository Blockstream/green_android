package com.blockstream.green.ui.onboarding

import androidx.lifecycle.Observer
import com.blockstream.gdk.data.LoginData
import com.blockstream.gdk.data.Network
import com.blockstream.green.TestViewModel
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.utils.ConsumableEvent
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*

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

    private var restoreWallet: Wallet = Wallet(walletHashId = "", name = "restore", network = "testnet")

    private val multisigNetwork = Network(
        "testnet",
        "Testnet",
        "testnet",
        false,
        false,
        false
    )

    private val singlesigNetwork = Network(
        "electrum-testnet",
        "Electrum Testnet",
        "electrum-testnet",
        false,
        false,
        false,
        serverType = "electrum"
    )

    @Before
    override fun setup() {
        super.setup()

        whenever(sessionManager.getOnBoardingSession(anyOrNull())).thenReturn(session)

        whenever(session.loginWithMnemonic(any(), any(), any(), any())).thenAnswer {
            // value is same as requested key
            val recovery = it.arguments[1] as String
            if (recovery != "valid") {
                throw Exception("invalid recovery phrase")
            }else{
                LoginData("")
            }
        }

        whenever(session.walletHashId).thenReturn("")

        whenever(session.createNewWallet(any(), any())).thenAnswer { LoginData("") }

        setupViewModel()
    }

    private fun setupViewModel(withRestoreWallet: Boolean = false) {
        viewModel = OnboardingViewModel(
            sessionManager,
            walletRepository,
            countly,
            if (withRestoreWallet) restoreWallet else null
        )

        viewModel.onEvent.observeForever(eventObserver)
        viewModel.onError.observeForever(errorObserver)
    }

    @Test
    fun test_createNewWallet() {
        val options = OnboardingOptions(isRestoreFlow = false, network = multisigNetwork)

        viewModel.createNewWallet(options, "123456", "")

        verify(eventObserver).onChanged(any())
        verify(errorObserver, never()).onChanged(any())
    }

    @Test
    fun test_recoveryPhrase_valid() {
        viewModel.checkRecoveryPhrase(multisigNetwork, "valid", "", NavigateEvent.Navigate)

        verify(eventObserver).onChanged(any())
        verify(errorObserver, never()).onChanged(any())
    }

    @Test
    fun test_recoveryPhrase_invalid() {
        viewModel.checkRecoveryPhrase(multisigNetwork, "invalid", "", NavigateEvent.Navigate)

        verify(eventObserver, never()).onChanged(any())
        verify(errorObserver).onChanged(any())
    }

    @Test
    fun restoreWallet_withPin() {
        val options = OnboardingOptions(isRestoreFlow = true, network = multisigNetwork)

        viewModel.restoreWithPin(options, "123456")

        verify(walletRepository).addWallet(any())
        verify(walletRepository, never()).updateWalletSync(any())

        verify(eventObserver).onChanged(argThat {
            val wallet = (this.peekContent() as NavigateEvent.NavigateWithData).data as Wallet
            true
        })
        verify(errorObserver, never()).onChanged(any())
    }

    @Test
    fun restoreWallet_withRecoveryPhrase() {
        setupViewModel(withRestoreWallet = true)

        val options = OnboardingOptions(isRestoreFlow = true, network = multisigNetwork)

        viewModel.restoreWithPin(options, "123456")

        verify(walletRepository, never()).addWallet(any())
        verify(walletRepository).updateWalletSync(any())

        verify(eventObserver).onChanged(argThat {
            val wallet = (this.peekContent() as NavigateEvent.NavigateWithData).data as Wallet
            true
        })
        verify(errorObserver, never()).onChanged(any())
    }

    @Test
    fun walletName_onCreate_withoutWallets_shouldBe_multisig() {
        var options = OnboardingOptions(isRestoreFlow = false, network = multisigNetwork)
        viewModel.createNewWallet(options, "123456", "")
        verify(eventObserver, only()).onChanged(argThat {
            val wallet = (this.peekContent() as NavigateEvent.NavigateWithData).data as Wallet
            Assert.assertEquals("Multisig Testnet", wallet.name)
            true
        })
    }

    @Test
    fun walletName_onCreate_withoutWallets_shouldBe_singlesig() {
        var options = OnboardingOptions(isRestoreFlow = false, network = singlesigNetwork)
        viewModel.createNewWallet(options, "123456", "")
        verify(eventObserver, only()).onChanged(argThat {
            val wallet = (this.peekContent() as NavigateEvent.NavigateWithData).data as Wallet
            Assert.assertEquals("Singlesig Testnet", wallet.name)
            true
        })
    }

    @Test
    fun walletName_onCreate_withWallets_shouldBe_incremented() {
        val options = OnboardingOptions(isRestoreFlow = false, network = multisigNetwork)

        whenever(walletRepository.getWalletsForNetworkSync(any())).thenReturn(listOf(mock(), mock(), mock()))

        viewModel.createNewWallet(options, "123456", "")

        verify(eventObserver).onChanged(argThat {
            val wallet = (this.peekContent() as NavigateEvent.NavigateWithData).data as Wallet
            Assert.assertEquals("Multisig Testnet #4", wallet.name)
            true
        })
    }

    @Test
    fun walletName_onRestore_shouldBe_incremented() {
        val options = OnboardingOptions(isRestoreFlow = true, network = multisigNetwork)

        whenever(walletRepository.getWalletsForNetworkSync(any())).thenReturn(listOf(mock(), mock(), mock()))

        viewModel.restoreWithPin(options, "123456")

        verify(eventObserver).onChanged(argThat {
            val wallet = (this.peekContent() as NavigateEvent.NavigateWithData).data as Wallet
            Assert.assertEquals("Multisig Testnet #4", wallet.name)
            true
        })
    }

}