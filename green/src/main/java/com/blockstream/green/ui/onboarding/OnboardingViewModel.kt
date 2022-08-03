package com.blockstream.green.ui.onboarding

import com.blockstream.gdk.data.EncryptWithPin
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
import com.blockstream.green.gdk.title
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.ConsumableEvent
import mu.KLogging

open class OnboardingViewModel constructor(
    val sessionManager: SessionManager,
    val walletRepository: WalletRepository,
    countly: Countly,
    private val restoreWallet: Wallet?
) : AppViewModel(countly) {
    val session = sessionManager.getOnBoardingSession(restoreWallet)

    private fun withPinData(options: OnboardingOptions) =
        options.walletName?.contains(SkipPinData) != true

    fun createNewWallet(
        options: OnboardingOptions,
        pin: String,
        mnemonic: String
    ) {

        doUserAction({
            val loginData = session.loginWithMnemonic(
                isTestnet = options.isTestnet == true,
                loginCredentialsParams = LoginCredentialsParams(mnemonic = mnemonic),
                initializeSession = true,
                isSmartDiscovery = false,
                isCreate = true,
                isRestore = false
            )

            var encryptWithPin: EncryptWithPin? = null

            if (withPinData(options)) {
                val credentials = session.getCredentials()
                encryptWithPin =
                    session.encryptWithPin(null, EncryptWithPinParams(pin, credentials))
            }

            // Archive all accounts on the newly created wallet
            session.accounts.forEach { account ->
                session.updateAccount(
                    account = account,
                    isHidden = true,
                    resetAccountName = account.type.title()
                )
            }

            val wallet = Wallet(
                walletHashId = loginData.walletHashId,
                name = generateWalletName(userInputName = options.walletName),
                activeNetwork = session.activeAccountOrNull?.networkId
                    ?: session.defaultNetwork.network,
                activeAccount = session.activeAccountOrNull?.pointer ?: 0,
                isRecoveryPhraseConfirmed = true, // options.isRestoreFlow || !mnemonic.isNullOrBlank(),
                isHardware = false,
                isTestnet = session.defaultNetwork.isTestnet
            )

            wallet.id = walletRepository.insertWallet(wallet)

            encryptWithPin?.let {
                walletRepository.insertOrReplaceLoginCredentials(
                    LoginCredentials(
                        walletId = wallet.id,
                        network = it.network.id,
                        credentialType = CredentialType.PIN,
                        pinData = it.pinData
                    )
                )
            }

            sessionManager.upgradeOnBoardingSessionToWallet(wallet)

            countly.createWallet(session)

            wallet
        }, postAction = {
            onProgress.value = it == null
        }, onSuccess = {
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(it)))
        })
    }

    fun createNewWatchOnlyWallet(
        appKeystore: AppKeystore,
        options: OnboardingOptions,
        username: String,
        password: String,
        savePassword: Boolean
    ) {

        doUserAction({
            val network = options.network!!

            val loginData = session.loginWatchOnly(network, username, password)

            val wallet = Wallet(
                walletHashId = loginData.networkHashId, // Use networkHashId as the watch-only is linked to a specific network
                name = generateWalletName(userInputName = null),
                activeNetwork = session.activeAccountOrNull?.networkId ?: session.defaultNetwork.id,
                activeAccount = session.activeAccountOrNull?.pointer ?: 0,
                isRecoveryPhraseConfirmed = true,
                watchOnlyUsername = username,
                isTestnet = network.isTestnet
            )

            wallet.id = walletRepository.insertWallet(wallet)

            if (savePassword) {
                val encryptedData = appKeystore.encryptData(password.toByteArray())
                walletRepository.insertOrReplaceLoginCredentials(
                    LoginCredentials(
                        walletId = wallet.id,
                        network = network.id,
                        credentialType = CredentialType.KEYSTORE,
                        encryptedData = encryptedData
                    )
                )
            }

            sessionManager.upgradeOnBoardingSessionToWallet(wallet)
            countly.importWallet(session)

            wallet
        }, postAction = {
            onProgress.value = it == null
        }, onSuccess = {
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(it)))
        })
    }

    private suspend fun generateWalletName(userInputName: String?): String {
        return generateWalletName(
            wallets = walletRepository.getSoftwareWallets(),
            userInputName = userInputName
        )
    }

    private fun generateWalletName(
        wallets: List<Wallet>,
        userInputName: String?
    ): String {
        return userInputName?.replace(SkipPinData, "")?.trim() ?: run {
            return@run "My Wallet ${((wallets.lastOrNull()?.id ?: 0) + 1).takeIf { it > 1 } ?: ""}".trim()
        }
    }

    fun checkRecoveryPhrase(
        isTestnet: Boolean,
        mnemonic: String,
        password: String?,
        successEvent: AppEvent
    ) {
        doUserAction({
            session.loginWithMnemonic(
                isTestnet = isTestnet,
                loginCredentialsParams = LoginCredentialsParams(
                    mnemonic = mnemonic,
                    password = password
                ),
                initNetworks = listOf(session.prominentNetwork(isTestnet)),
                initializeSession = false,
                isSmartDiscovery = false,
                isCreate = true,
                isRestore = false
            ).also {
                if (restoreWallet == null) {
                    // Check if wallet already exists
                    it.walletHashId.let { walletHashId ->
                        walletRepository.getWalletWithHashId(walletHashId, false)
                            ?.let { wallet ->
                                throw Exception("id_wallet_already_restored:${wallet.name}")
                            }
                    }
                } else {
                    // check if walletHashId is the same (also use networkHashId for backwards compatibility)
                    if (restoreWallet.walletHashId.isNotBlank() && (restoreWallet.walletHashId != it.walletHashId && restoreWallet.walletHashId != it.networkHashId)) {
                        throw Exception("id_the_recovery_phrase_doesnt")
                    }
                }
            }
        }, onSuccess = {
            onEvent.postValue(ConsumableEvent(successEvent))
        })
    }

    fun restoreWallet(
        options: OnboardingOptions,
        pin: String,
        mnemonic: String,
        password: String?
    ) {

        doUserAction({
            session.loginWithMnemonic(
                isTestnet = options.isTestnet == true,
                loginCredentialsParams = LoginCredentialsParams(
                    mnemonic = mnemonic,
                    password = password
                ),
                initializeSession = true,
                isSmartDiscovery = false,
                isCreate = false,
                isRestore = true
            )

            var encryptWithPin: EncryptWithPin? = null

            if (withPinData(options)) {
                val credentials = session.getCredentials()
                encryptWithPin =
                    session.encryptWithPin(null, EncryptWithPinParams(pin, credentials))
            }

            val wallet: Wallet

            if (restoreWallet == null) {
                wallet = Wallet(
                    walletHashId = session.walletHashId ?: "",
                    name = generateWalletName(
                        userInputName = options.walletName
                    ),
                    activeNetwork = session.activeAccountOrNull?.networkId
                        ?: session.defaultNetwork.id,
                    activeAccount = session.activeAccountOrNull?.pointer ?: 0,
                    isRecoveryPhraseConfirmed = options.isRestoreFlow,
                    isHardware = false,
                    isTestnet = options.isTestnet == true,
                )

                wallet.id = walletRepository.insertWallet(wallet)
            } else {
                wallet = restoreWallet

                wallet.name = options.walletName ?: restoreWallet.name
                walletRepository.updateWallet(wallet)
            }

            encryptWithPin?.also {
                walletRepository.insertOrReplaceLoginCredentials(
                    LoginCredentials(
                        walletId = wallet.id,
                        network = it.network.id,
                        credentialType = CredentialType.PIN,
                        pinData = it.pinData
                    )
                )
            }

            sessionManager.upgradeOnBoardingSessionToWallet(wallet)

            countly.importWallet(session)

            wallet
        }, postAction = {
            onProgress.value = it == null
        }, onSuccess = {
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(it)))
        })
    }

    companion object : KLogging() {
        const val SkipPinData = "_skip_pin_data_"
    }
}
