package com.blockstream.green.ui.onboarding

import com.blockstream.gdk.data.Network
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.database.CredentialType
import com.blockstream.green.database.LoginCredentials
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.ConsumableEvent


open class OnboardingViewModel(
    val sessionManager: SessionManager,
    val walletRepository: WalletRepository,
    private val restoreWallet: Wallet?
) : AppViewModel() {
    val session = sessionManager.getOnBoardingSession(restoreWallet)

    fun createNewWallet(options: OnboardingOptions, pin: String, mnemonic: String?) {

        session.observable {
            val network = options.network!!
            val loginData = it.createNewWallet(network, mnemonic)

            var pinData = it.setPin(pin)

            val wallet = Wallet(
                walletHashId = loginData.walletHashId,
                name = generateWalletName(network, options.walletName),
                network = network.id,
                isRecoveryPhraseConfirmed = options.isRestoreFlow || !mnemonic.isNullOrBlank(),
                isHardware = false
            )

            wallet.id = walletRepository.addWallet(wallet)

            pinData.let {
                walletRepository.addLoginCredentialsSync(
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

    private fun generateWalletName(network:Network, userInputName: String?) : String{
        return userInputName ?: run {
            val wallets = walletRepository.getWalletsForNetworkSync(network)

            return@run if(wallets.isNotEmpty()){
                "${network.productName} #${wallets.size + 1}"
            }else{
                network.productName
            }
        }
    }

    fun checkRecoveryPhrase(network: Network , mnemonic: String, mnemonicPassword: String) {
        session.observable {
            val loginData = it.loginWithMnemonic(network, mnemonic, mnemonicPassword)

            if(restoreWallet == null) {
                // Check if wallet already exists
                it.walletHashId?.let { walletHashId ->
                    if (walletRepository.walletsExistsSync(walletHashId, false)) {
                        throw Exception("id_wallet_already_exists")
                    }
                }
            }else{
                // check if walletHashId is the same
                if(restoreWallet.walletHashId.isNotBlank() && restoreWallet.walletHashId != it.walletHashId){
                    throw Exception("id_the_recovery_phrase_youve_entered_doesnt_match")
                }
            }

            it
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

    fun restoreWithPin(options: OnboardingOptions, pin: String) {
        session.observable {
            val network = options.network!!

            val pinData = it.setPin(pin)

            val wallet : Wallet

            if(restoreWallet == null){
                wallet  = restoreWallet
                    ?: Wallet(
                        walletHashId = it.walletHashId ?: "",
                        name = generateWalletName(network, options.walletName),
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

            walletRepository.addLoginCredentialsSync(
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