package com.blockstream.green.ui.onboarding

import androidx.lifecycle.Observer
import com.blockstream.gdk.data.*
import com.blockstream.gdk.params.LoginCredentialsParams
import com.blockstream.green.TestViewModel
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.utils.ConsumableEvent
import kotlinx.coroutines.test.runTest
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
    private lateinit var session: GdkSession

    @Mock
    private lateinit var networks: Networks

    @Mock
    private lateinit var eventObserver: Observer<ConsumableEvent<Any>>

    @Mock
    private lateinit var errorObserver: Observer<ConsumableEvent<Throwable>>

    @Mock
    private lateinit var wallet: Wallet

    private var restoreWallet: Wallet = Wallet(walletHashId = "", name = "restore", activeNetwork = "testnet", isTestnet = true)

//    private val multisigNetwork = Network(
//        "testnet",
//        "Testnet",
//        "testnet",
//        false,
//        false,
//        false
//    )

    private val testnetNetwork = Network(
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
        whenever(session.defaultNetwork).thenReturn(testnetNetwork)

        whenever(session.loginWithMnemonic(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenAnswer {
            // value is same as requested key
            val loginCredentials = it.arguments.find { it is LoginCredentialsParams } as LoginCredentialsParams
            if (loginCredentials.mnemonic == "valid") {
                LoginData("walletHashId", "")
            }else{
                throw Exception("invalid recovery phrase")
            }
        }

        whenever(session.walletHashId).thenReturn("")
        whenever(session.getCredentials(anyOrNull())).thenReturn(Credentials(mnemonic = ""))
        whenever(session.encryptWithPin(anyOrNull(), anyOrNull())).thenReturn(EncryptWithPin(networkInjected = testnetNetwork, pinData = PinData("","","")))

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
    fun test_createNewWallet() = runTest {
        val options = OnboardingOptions(isRestoreFlow = false)

        whenever(walletRepository.getSoftwareWallets()).thenReturn(listOf())
        whenever(walletRepository.insertWallet(any())).thenReturn(1)

        viewModel.createNewWallet(options, "123456", "valid")

        verify(eventObserver).onChanged(any())
        verify(errorObserver, never()).onChanged(any())
    }

    @Test
    fun test_recoveryPhrase_valid() {
        viewModel.checkRecoveryPhrase(true, "valid", "", NavigateEvent.Navigate)

        verify(eventObserver).onChanged(any())
        verify(errorObserver, never()).onChanged(any())
    }

    @Test
    fun test_recoveryPhrase_invalid() {
        viewModel.checkRecoveryPhrase(true, "invalid", "", NavigateEvent.Navigate)

        verify(eventObserver, never()).onChanged(any())
        verify(errorObserver).onChanged(any())
    }

    @Test
    fun restoreWallet_withPin() = runTest {
        val options = OnboardingOptions(isRestoreFlow = true)

        whenever(walletRepository.getSoftwareWallets()).thenReturn(listOf())
        whenever(walletRepository.insertWallet(any())).thenReturn(1)

        viewModel.restoreWallet(options, "123456","valid", "")

        verify(walletRepository).insertWallet(any())
        verify(walletRepository, never()).updateWallet(any())

        verify(eventObserver).onChanged(argThat {
            val wallet = (this.peekContent() as NavigateEvent.NavigateWithData).data as Wallet
            true
        })
        verify(errorObserver, never()).onChanged(any())
    }

    @Test
    fun restoreWallet_withRecoveryPhrase() = runTest {
        setupViewModel(withRestoreWallet = true)

        val options = OnboardingOptions(isRestoreFlow = true)

        viewModel.restoreWallet(options, "123456", "valid", "")

        verify(walletRepository, never()).insertWallet(any())
        verify(walletRepository).updateWallet(any())

        verify(eventObserver).onChanged(argThat {
            val wallet = (this.peekContent() as NavigateEvent.NavigateWithData).data as Wallet
            true
        })
        verify(errorObserver, never()).onChanged(any())
    }

    @Test
    fun walletName_onCreate_withWallets_shouldBe_incremented() = runTest {
        val options = OnboardingOptions(isRestoreFlow = false)

        whenever(wallet.id).thenReturn(3)
        whenever(walletRepository.getSoftwareWallets()).thenReturn(listOf(mock(), wallet))
        whenever(walletRepository.insertWallet(any())).thenReturn(1)

        viewModel.createNewWallet(options, "123456", "valid")

        verify(eventObserver).onChanged(argThat {
            val wallet = (this.peekContent() as NavigateEvent.NavigateWithData).data as Wallet
            Assert.assertEquals("My Wallet 4", wallet.name)
            true
        })
    }

    @Test
    fun walletName_onRestore_shouldBe_incremented() = runTest {
        val options = OnboardingOptions(isRestoreFlow = true)

        whenever(wallet.id).thenReturn(3)
        whenever(walletRepository.insertWallet(any())).thenReturn(4)
        whenever(walletRepository.getSoftwareWallets()).thenReturn(listOf(mock(), wallet))

        viewModel.restoreWallet(options, "123456", "valid", "")

        verify(eventObserver).onChanged(argThat {
            val wallet = (this.peekContent() as NavigateEvent.NavigateWithData).data as Wallet
            Assert.assertEquals("My Wallet 4", wallet.name)
            true
        })
    }

}