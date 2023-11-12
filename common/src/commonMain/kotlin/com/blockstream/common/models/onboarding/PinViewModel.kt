package com.blockstream.common.models.onboarding

import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Event
import com.blockstream.common.extensions.createLoginCredentials
import com.blockstream.common.extensions.title
import com.blockstream.common.gdk.data.EncryptWithPin
import com.blockstream.common.gdk.params.EncryptWithPinParams
import com.blockstream.common.gdk.params.LoginCredentialsParams
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.generateWalletName
import com.rickclephas.kmm.viewmodel.MutableStateFlow
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class PinViewModelAbstract(
    val setupArgs: SetupArgs
) : GreenViewModel(greenWalletOrNull = setupArgs.greenWallet) {
    override fun screenName(): String = "OnBoardPin"

    override fun segmentation(): HashMap<String, Any>? = setupArgs.let { countly.onBoardingSegmentation(setupArgs = it) }

    @NativeCoroutinesState
    abstract val navigationLock: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val rocketAnimation: StateFlow<Boolean>
}

class PinViewModel constructor(
    setupArgs: SetupArgs
) : PinViewModelAbstract(setupArgs) {
    @NativeCoroutinesState
    override val navigationLock: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, false)

    @NativeCoroutinesState
    override val rocketAnimation: MutableStateFlow<Boolean> =
        MutableStateFlow(viewModelScope, false)

    class LocalEvents {
        class SetPin(val pin: String) : Event
    }

    override val isLoginRequired: Boolean
        get() = false

    init {
        if (setupArgs.isRestoreFlow) {
            checkRecoveryPhrase(
                isTestnet = setupArgs.isTestnet == true,
                mnemonic = setupArgs.mnemonic,
                password = setupArgs.password
            )
        }

        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.SetPin) {

            if (event.pin.length == 6) {
                if (setupArgs.isRestoreFlow) {
                    restoreWallet(
                        setupArgs = setupArgs,
                        pin = event.pin,
                    )
                } else {
                    createNewWallet(
                        setupArgs = setupArgs, pin = event.pin
                    )
                }
            } else {
                postSideEffect(SideEffects.ErrorDialog(Exception("PIN should be 6 digits")))
            }

        }
    }

    private fun checkRecoveryPhrase(isTestnet: Boolean, mnemonic: String, password: String?) {
        onProgressDescription.value = "id_recovery_phrase_check"
        doAsync({
            session.loginWithMnemonic(
                isTestnet = isTestnet,
                loginCredentialsParams = LoginCredentialsParams(
                    mnemonic = mnemonic, password = password
                ),
                initNetworks = listOf(session.prominentNetwork(isTestnet)),
                initializeSession = false,
                isSmartDiscovery = false,
                isCreate = true,
                isRestore = false
            ).also {
                if (greenWalletOrNull == null) {
                    // Check if wallet already exists
                    it.walletHashId.also { walletHashId ->
                        database.getWalletWithXpubHashId(
                            xPubHashId = walletHashId, isTestnet = isTestnet, isHardware = false
                        )?.also { wallet ->
                            throw Exception("id_wallet_already_restored_s|${wallet.name}")
                        }
                    }
                } else {
                    // check if walletHashId is the same (also use networkHashId for backwards compatibility)
                    if (greenWallet.xPubHashId.isNotBlank() && (greenWallet.xPubHashId != it.walletHashId && greenWallet.xPubHashId != it.networkHashId)) {
                        throw Exception("id_the_recovery_phrase_doesnt")
                    }
                }
            }

        }, onSuccess = {

        }, onError = {
            if (it.message?.startsWith("id_wallet_already_restored") == true || it.message?.startsWith("id_the_recovery_phrase_doesnt") == true) {
                postSideEffect(SideEffects.NavigateBack(it))
            } else if (it.message == "id_login_failed") {
                postSideEffect(SideEffects.NavigateBack(Exception("id_no_multisig_shield_wallet")))
            } else if (it.message?.lowercase()?.contains("decrypt_mnemonic") == true || it.message?.lowercase()?.contains("invalid checksum") == true) {
                postSideEffect(SideEffects.NavigateBack(Exception("id_error_passphrases_do_not_match")))
            } else {
                postSideEffect(SideEffects.ErrorDialog(it))
            }
        })
    }

    private fun createNewWallet(setupArgs: SetupArgs, pin: String) {

        doAsync({
            val loginData = session.loginWithMnemonic(
                isTestnet = setupArgs.isTestnet == true,
                loginCredentialsParams = LoginCredentialsParams(mnemonic = setupArgs.mnemonic),
                initializeSession = true,
                isSmartDiscovery = false,
                isCreate = true,
                isRestore = false
            )

            val credentials = session.getCredentials()
            val encryptWithPin =
                session.encryptWithPin(null, EncryptWithPinParams(pin, credentials))

            // Archive all accounts on the newly created wallet
            session.accounts.value.forEach { account ->
                session.updateAccount(
                    account = account, isHidden = true, resetAccountName = account.type.title()
                )
            }

            val wallet = GreenWallet.createWallet(
                name = generateWalletName(settingsManager),
                xPubHashId = loginData.walletHashId,
                activeNetwork = session.activeAccount.value?.networkId
                    ?: session.defaultNetwork.network,
                activeAccount = session.activeAccount.value?.pointer ?: 0,
                isHardware = false,
                isTestnet = session.defaultNetwork.isTestnet
            ).also {
                database.insertWallet(it)
            }

            encryptWithPin.let {
                database.replaceLoginCredential(
                    createLoginCredentials(
                        walletId = wallet.id,
                        network = it.network.id,
                        credentialType = CredentialType.PIN_PINDATA,
                        pinData = it.pinData
                    )
                )
            }

            sessionManager.upgradeOnBoardingSessionToWallet(wallet)

            countly.createWallet(session)

            wallet
        }, preAction = {
            onProgress.value = true
            navigationLock.value = true
        }, postAction = {
            onProgress.value = it == null
            navigationLock.value = false
        }, onSuccess = {
            postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WalletOverview(it)))
        })
    }

    private fun restoreWallet(
        setupArgs: SetupArgs,
        pin: String,
    ) {
        onProgressDescription.value = "id_restoring_your_wallet"
        doAsync({
            session.loginWithMnemonic(
                isTestnet = setupArgs.isTestnet == true,
                loginCredentialsParams = LoginCredentialsParams(
                    mnemonic = setupArgs.mnemonic, password = setupArgs.password
                ),
                initializeSession = true,
                isSmartDiscovery = false,
                isCreate = false,
                isRestore = true
            )

            val encryptWithPin: EncryptWithPin = session.getCredentials().let { credentials ->
                session.encryptWithPin(
                    network = null,
                    encryptWithPinParams = EncryptWithPinParams(
                        pin = pin,
                        credentials = credentials
                    )
                )
            }

            val wallet: GreenWallet

            if (greenWalletOrNull == null) {
                wallet = GreenWallet.createWallet(
                    name = generateWalletName(settingsManager),
                    xPubHashId = session.xPubHashId ?: "",
                    activeNetwork = session.activeAccount.value?.networkId
                        ?: session.defaultNetwork.id,
                    activeAccount = session.activeAccount.value?.pointer ?: 0,
                    isTestnet = setupArgs.isTestnet == true,
                )

                database.insertWallet(wallet)

                if (session.hasLightning) {

                    session.lightningSdk.appGreenlightCredentials?.also { credentials ->
                        val encryptedData =
                            greenKeystore.encryptData(credentials.toJson().encodeToByteArray())

                        val loginCredentials = createLoginCredentials(
                            walletId = wallet.id,
                            network = session.lightning!!.id,
                            credentialType = CredentialType.KEYSTORE_GREENLIGHT_CREDENTIALS,
                            encryptedData = encryptedData
                        )

                        database.replaceLoginCredential(loginCredentials)
                    }
                }

            } else {
                wallet = greenWalletOrNull
            }

            database.replaceLoginCredential(
                createLoginCredentials(
                    walletId = wallet.id,
                    network = encryptWithPin.network.id,
                    credentialType = CredentialType.PIN_PINDATA,
                    pinData = encryptWithPin.pinData
                )
            )

            sessionManager.upgradeOnBoardingSessionToWallet(wallet)

            countly.importWallet(session)

            wallet
        }, preAction = {
            onProgress.value = true
            navigationLock.value = true
            rocketAnimation.value = true
        }, postAction = {
            onProgress.value = it == null
            rocketAnimation.value = it == null
            navigationLock.value = false
        }, onSuccess = {
            postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WalletOverview(it)))
        })
    }

    companion object : Loggable()
}

class PinViewModelPreview(setupArgs: SetupArgs) : PinViewModelAbstract(setupArgs) {

    override val navigationLock: MutableStateFlow<Boolean>
        get() = MutableStateFlow(viewModelScope, false)
    override val rocketAnimation: MutableStateFlow<Boolean>
        get() = MutableStateFlow(viewModelScope, false)

    companion object {
        fun preview() = PinViewModelPreview(SetupArgs(mnemonic = "neutral inherit learn"))
    }
}