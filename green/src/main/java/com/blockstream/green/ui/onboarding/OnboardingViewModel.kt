package com.blockstream.green.ui.onboarding

import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.gdk.SessionManager
import com.blockstream.gdk.data.Network
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.database.CredentialType
import com.blockstream.green.database.LoginCredentials
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.observable


open class OnboardingViewModel(
    val sessionManager: SessionManager,
    val walletRepository: WalletRepository,
    private val restoreWallet: Wallet?
) : AppViewModel() {
    val session = sessionManager.getOnBoardingSession(restoreWallet)

    fun createNewWallet(options: OnboardingOptions, pin: String, mnemonic: String?) {

        session.observable {
            val network = options.network!!
            it.createNewWallet(network, mnemonic)

            var pinData = it.setPin(pin)

            val wallet = Wallet(
                name = options.walletName ?: network.name,
                network = network.id,
                isRecoveryPhraseConfirmed = options.isRestoreFlow || !mnemonic.isNullOrBlank(),
                isHardware = false
            )

            wallet.id = walletRepository.addWallet(wallet)

            pinData.let {
                walletRepository.addLoginCredentials(
                    LoginCredentials(
                        walletId = wallet.id,
                        credentialType = CredentialType.PIN,
                        pinData = pinData
                    )
                )
            }

            sessionManager.upgradeOnBoardingSessionToWallet(wallet)

            wallet
        }.doOnSubscribe {
            onProgress.value = true
        }.subscribe({
            onEvent.postValue(ConsumableEvent(it))
        }, {
            it.printStackTrace()
            onProgress.value = false
            onError.value = ConsumableEvent(it)
        })
    }

    fun checkRecoveryPhrase(network: Network , mnemonic: String, mnemonicPassword: String) {
        session.observable {
            it.loginWithMnemonic(network, mnemonic, mnemonicPassword)
        }.doOnSubscribe {
            onProgress.value = true
        }.doOnTerminate {
            onProgress.value = false
        }.subscribe({
            onEvent.postValue(ConsumableEvent(true))
        }, {
            onError.value = ConsumableEvent(it)
        })
    }

    // EMULATES DEVICE LOGIN
    fun loginWithDevice(options: OnboardingOptions){
        session.observable {

            // DON'T USE THIS MNEMONIC
            // DON'T MOVE ANY FUNDS, It's just here for development purposes and should be removed
            val network = options.network!!
            val mnemonic = "ADD MNEMONIC HERE"
            it.loginWithMnemonic(network, mnemonic, "")

            val wallet = Wallet(
                name = options.walletName ?: network.name,
                network = network.id,
                isRecoveryPhraseConfirmed = true,
                isHardware = true
            )

            wallet.id = walletRepository.addWallet(wallet)

            sessionManager.upgradeOnBoardingSessionToWallet(wallet)

            wallet
        }.doOnSubscribe {
            onProgress.value = true
        }.doOnTerminate {
            onProgress.value = false
        }.subscribe({
            onEvent.postValue(ConsumableEvent(it))
        }, {
            onError.value = ConsumableEvent(it)
        })
    }

    fun restoreWithPin(options: OnboardingOptions, pin: String) {
        session.observable {
            val network = options.network!!

            val pinData = it.setPin(pin)

            val wallet : Wallet

            if(restoreWallet == null){
                wallet  = restoreWallet
                    ?: Wallet(
                        name = options.walletName ?: options.network.name,
                        network = network.id,
                        isRecoveryPhraseConfirmed = options.isRestoreFlow,
                        isHardware = false
                    )

                wallet.id = walletRepository.addWallet(wallet)
            }else{
                wallet = restoreWallet

                wallet.name = options.walletName ?: restoreWallet.name
                wallet.isRecoveryPhraseConfirmed = true
                walletRepository.updateWalletSync(wallet)
            }

            walletRepository.addLoginCredentials(
                LoginCredentials(
                    walletId = wallet.id,
                    credentialType = CredentialType.PIN,
                    pinData = pinData
                )
            )

            sessionManager.upgradeOnBoardingSessionToWallet(wallet)

            wallet
        }.doOnSubscribe {
            onProgress.value = true
        }.subscribe({
            onEvent.postValue(ConsumableEvent(it))
        }, {
            onProgress.value = false
            onError.value = ConsumableEvent(it)
        })
    }
}