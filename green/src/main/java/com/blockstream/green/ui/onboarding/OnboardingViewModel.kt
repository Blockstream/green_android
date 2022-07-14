package com.blockstream.green.ui.onboarding

import com.blockstream.gdk.data.Network
import com.blockstream.gdk.data.PinData
import com.blockstream.gdk.params.EncryptWithPinParams
import com.blockstream.gdk.params.LoginCredentialsParams
import com.blockstream.green.data.AppEvent
import com.blockstream.green.data.Countly
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.database.CredentialType
import com.blockstream.green.database.LoginCredentials
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.ConsumableEvent
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import mu.KLogging


open class OnboardingViewModel constructor(
    val sessionManager: SessionManager,
    val walletRepository: WalletRepository,
    val countly: Countly,
    private val restoreWallet: Wallet?
) : AppViewModel() {
    val session = sessionManager.getOnBoardingSession(restoreWallet)

    private fun withPinData(options: OnboardingOptions) = options.walletName?.contains(SkipPinData) != true

    fun createNewWallet(options: OnboardingOptions, pin: String, mnemonic: String) {

        session.observable {
            val network = options.network!!
            val loginData = it.createNewWallet(network = network, mnemonic = mnemonic)

            var pinData : PinData? = null

            if(withPinData(options)){
                val credentials = it.getCredentials()
                pinData = it.encryptWithPin(EncryptWithPinParams(pin, credentials)).pinData
            }

            val wallet = Wallet(
                walletHashId = loginData.walletHashId,
                name = generateWalletNameSync(network = network, userInputName = options.walletName),
                network = network.id,
                isRecoveryPhraseConfirmed = true, // options.isRestoreFlow || !mnemonic.isNullOrBlank(),
                isHardware = false
            )

            wallet.id = walletRepository.addWallet(wallet)

            pinData?.let {
                walletRepository.addLoginCredentialsSync(
                    LoginCredentials(
                        walletId = wallet.id,
                        credentialType = CredentialType.PIN,
                        pinData = pinData
                    )
                )
            }

            sessionManager.upgradeOnBoardingSessionToWallet(wallet)

            countly.createWallet(session)

            wallet
        }.doOnSubscribe {
            onProgress.value = true
        }.subscribe({
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(it)))
        }, {
            it.printStackTrace()
            onProgress.value = false
            onError.value = ConsumableEvent(it)
        })
    }

    fun createNewWatchOnlyWallet(appKeystore: AppKeystore, options: OnboardingOptions, username: String, password: String, savePassword: Boolean) {
        session.observable {
            val network = options.network!!

            val loginData =
                it.loginWatchOnly(network, username, password)

            val wallet = Wallet(
                walletHashId = loginData.walletHashId,
                name = generateWalletNameSync(network = network, userInputName = null),
                network = session.network.id,
                isRecoveryPhraseConfirmed = true,
                watchOnlyUsername = username
            )

            wallet.id = walletRepository.addWallet(wallet)

            if (savePassword) {
                val encryptedData = appKeystore.encryptData(password.toByteArray())
                walletRepository.addLoginCredentialsSync(
                    LoginCredentials(
                        walletId = wallet.id,
                        credentialType = CredentialType.KEYSTORE,
                        encryptedData = encryptedData
                    )
                )
            }

            sessionManager.upgradeOnBoardingSessionToWallet(wallet)
            countly.restoreWatchOnlyWallet(session)
            wallet
        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onError = {
                onError.value = ConsumableEvent(it)
            },
            onSuccess = {
                onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(it)))
            }
        ).addTo(disposables)
    }

    fun generateWalletNameSync(network: Network, userInputName: String?): String {
        return generateWalletName(
            wallets = walletRepository.getWalletsForNetworkSync(network),
            network = network,
            userInputName = userInputName
        )
    }

    suspend fun generateWalletNameSuspend(network: Network, userInputName: String?): String {
        return generateWalletName(
            wallets = walletRepository.getWalletsForNetworkSuspend(network),
            network = network,
            userInputName = userInputName
        )
    }

    private fun generateWalletName(
        wallets: List<Wallet>,
        network: Network,
        userInputName: String?
    ): String {
        return userInputName?.replace(SkipPinData, "")?.trim() ?: run {

            return@run (if(wallets.isNotEmpty()){
                "${network.productName} #${wallets.size + 1}"
            }else{
                network.productName
            })
        }
    }

    fun checkRecoveryPhrase(network: Network , mnemonic: String, mnemonicPassword: String, successEvent: AppEvent) {
        session.observable {
            it.loginWithMnemonic(network, LoginCredentialsParams(mnemonic = mnemonic, password = mnemonicPassword))

            if(restoreWallet == null) {
                // Check if wallet already exists
                it.walletHashId?.let { walletHashId ->
                    if (walletRepository.walletsExistsSync(walletHashId, false)) {
                        throw Exception("id_wallet_already_restored")
                    }
                }
            }else{
                // check if walletHashId is the same
                if(restoreWallet.walletHashId.isNotBlank() && restoreWallet.walletHashId != it.walletHashId){
                    throw Exception("id_the_recovery_phrase_doesnt")
                }
            }

            it
        }.doOnSubscribe {
            onProgress.value = true
        }.doOnTerminate {
            onProgress.value = false
        }.subscribe({
            onEvent.postValue(ConsumableEvent(successEvent))
        }, {
            onError.value = ConsumableEvent(it)
        })
    }

    fun restoreWithPin(options: OnboardingOptions, pin: String) {
        session.observable {
            val network = options.network!!

            var pinData : PinData? = null

            if(withPinData(options)){
                val credentials = it.getCredentials()
                pinData = it.encryptWithPin(EncryptWithPinParams(pin, credentials)).pinData
            }

            val wallet : Wallet

            if(restoreWallet == null){
                wallet  = restoreWallet
                    ?: Wallet(
                        walletHashId = it.walletHashId ?: "",
                        name = generateWalletNameSync(network = network, userInputName = options.walletName),
                        network = network.id,
                        isRecoveryPhraseConfirmed = options.isRestoreFlow,
                        isHardware = false
                    )

                wallet.id = walletRepository.addWallet(wallet)
            }else{
                wallet = restoreWallet

                wallet.name = options.walletName ?: restoreWallet.name
                walletRepository.updateWalletSync(wallet)
            }

            pinData?.let {
                walletRepository.addLoginCredentialsSync(
                    LoginCredentials(
                        walletId = wallet.id,
                        credentialType = CredentialType.PIN,
                        pinData = pinData
                    )
                )
            }

            sessionManager.upgradeOnBoardingSessionToWallet(wallet)

            countly.restoreWallet(session)

            wallet
        }.doOnSubscribe {
            onProgress.value = true
        }.subscribe({
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(it)))
        }, {
            onProgress.value = false
            onError.value = ConsumableEvent(it)
        })
    }

    companion object: KLogging(){
        const val SkipPinData = "_skip_pin_data_"
    }
}
