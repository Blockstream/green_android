package com.blockstream.green.ui.login

import android.util.Base64
import androidx.lifecycle.*
import com.blockstream.gdk.data.TorEvent
import com.blockstream.gdk.params.LoginCredentialsParams
import com.blockstream.green.ApplicationScope
import com.blockstream.green.data.AppEvent
import com.blockstream.green.data.Countly
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.LoginCredentials
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceResolver
import com.blockstream.green.extensions.logException
import com.blockstream.green.extensions.string
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.isNotAuthorized
import com.blockstream.green.lifecycle.PendingLiveData
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.ConsumableEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.crypto.Cipher

class LoginViewModel @AssistedInject constructor(
    private var appKeystore: AppKeystore,
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    private val applicationScope: ApplicationScope,
    private val settingsManager: SettingsManager,
    @Assisted wallet: Wallet,
    @Assisted val device: Device?
) : AbstractWalletViewModel(sessionManager, walletRepository, countly, wallet) {

    sealed class LoginEvent: AppEvent {
        data class LaunchBiometrics(val loginCredentials: LoginCredentials) : LoginEvent()
        object LoginDevice : LoginEvent()
        object AskBip39Passphrase : LoginEvent()
    }

    val onErrorMessage = MutableLiveData<ConsumableEvent<Throwable>>()

    var biometricsCredentials: MutableLiveData<LoginCredentials> = MutableLiveData()
    var keystoreCredentials: MutableLiveData<LoginCredentials> = MutableLiveData()
    var pinCredentials: PendingLiveData<LoginCredentials> = PendingLiveData()
    var passwordCredentials: PendingLiveData<LoginCredentials> = PendingLiveData()

    var password = MutableLiveData("")
    var watchOnlyPassword = MutableLiveData("")

    val torEvent: MutableLiveData<TorEvent> = MutableLiveData()

    val applicationSettingsLiveData = settingsManager.getApplicationSettingsLiveData()

    var bip39Passphrase = MutableLiveData("")
    private val isBip39Login
        get() = bip39Passphrase.string().trim().isNotBlank()

    val isWatchOnlyLoginEnabled: LiveData<Boolean> by lazy {
        MediatorLiveData<Boolean>().apply {
            val block = { _: Any? ->
                val isInitial = (keystoreCredentials.value != null && initialAction.value == false)
                value = !watchOnlyPassword.value.isNullOrBlank() && !onProgress.value!! || isInitial
            }
            addSource(watchOnlyPassword, block)
            addSource(onProgress, block)
            addSource(keystoreCredentials, block)
            addSource(initialAction, block)
        }
    }

    /**
     * {@code initialAction} describes if the initial action is been taken.
     *
     * Also is used to prevent multiple actions to be initiated automatically .eg BiometricsPrompt
     *
     * For example initialAction is used in Watch-only login to display only the username and the login button.
     * If login fails, the password field is then shown.
     *
     */
    var initialAction = MutableLiveData(session.isConnected)

    var loginCredentialsInitialized = false

    val isEmergencyRecoveryPhrase = MutableLiveData(false)

    init {
        if(session.isConnected){
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(wallet)))
        }else{
            if (wallet.askForBip39Passphrase) {
                onEvent.postValue(ConsumableEvent(LoginEvent.AskBip39Passphrase))
            }

            device?.let {
                onEvent.postValue(ConsumableEvent(LoginEvent.LoginDevice))
            }

            // Beware as this will fire new values if eg. you change a login credential
            walletRepository.getWalletLoginCredentialsFlow(wallet.id).filterNotNull().onEach {
                    loginCredentialsInitialized = true
                    biometricsCredentials.value = it.biometrics
                    keystoreCredentials.value = it.keystore
                    pinCredentials.value = it.pin
                    passwordCredentials.value = it.password

                    if(initialAction.value == false && !wallet.askForBip39Passphrase){
                        it.biometrics?.let { biometricsCredentials ->
                            onEvent.postValue(ConsumableEvent(
                                LoginEvent.LaunchBiometrics(
                                    biometricsCredentials
                                )
                            ))
                            initialAction.postValue(true)
                        }
                    }
                }.launchIn(viewModelScope)

            sessionManager.torProxyProgress.onEach {
                torEvent.value = it
            }.launchIn(viewModelScope)
        }
    }

    fun setBip39Passphrase(passphrase: String?, alwaysAsk: Boolean){
        bip39Passphrase.value = passphrase?.trim()
        viewModelScope.launch(context = logException(countly)) {
            wallet.askForBip39Passphrase = alwaysAsk
            walletRepository.updateWallet(wallet)
        }

        if(initialAction.value == false){
            biometricsCredentials.value?.let { biometricsCredentials ->
                onEvent.postValue(ConsumableEvent(LoginEvent.LaunchBiometrics(biometricsCredentials)))
                initialAction.postValue(true)
            }
        }
    }

    private fun emergencyRecoveryPhrase(pin: String, loginCredentials: LoginCredentials) {
        doUserAction({
            session.emergencyRestoreOfRecoveryPhrase(
                wallet = wallet,
                pin = pin,
                loginCredentials = loginCredentials
            ).also {
                session.disconnect()
            }
        }, onSuccess = {credentials ->
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(credentials)))
            isEmergencyRecoveryPhrase.postValue(false)
        }, onError = {

            if (it.isNotAuthorized()) {
                loginCredentials.counter += 1

                viewModelScope.launch {
                    if (loginCredentials.counter < 3) {
                        walletRepository.updateLoginCredentials(loginCredentials)
                    } else {
                        walletRepository.deleteLoginCredentials(loginCredentials)
                    }
                }
            } else {
                onErrorMessage.postValue(ConsumableEvent(it))
            }

            onError.postValue(ConsumableEvent(it))
            countly.failedWalletLogin(session, it)
        })
    }

    fun loginWithDevice() {
        if(device == null) return

        login {
            val network = session.networks.getNetworkById(wallet.activeNetwork)
            session.loginWithDevice(
                network = network,
                device = device,
                hardwareWalletResolver = DeviceResolver(device.hwWallet, this),
                hwWalletBridge = this
            )
        }
    }

    fun loginWithPin(pin: String, loginCredentials: LoginCredentials) {
        if(isEmergencyRecoveryPhrase.value == true){
            emergencyRecoveryPhrase(pin, loginCredentials)
            return
        }

        // If Wallet Hash ID is empty (from migration) do a one-time wallet restore to make a real account discovery
        val isRestore = wallet.walletHashId.isEmpty()

        login(loginCredentials) {
            // if bip39 passphrase, don't initialize the session as we need to re-connect || initializeSession = bip39Passphrase.isNullOrBlank())
            session.loginWithPin(
                wallet = wallet,
                pin = pin,
                loginCredentials = loginCredentials,
                isRestore = isRestore,
                initializeSession = !isBip39Login
            )
        }
    }

    fun loginWatchOnlyWithKeyStore(loginCredentials: LoginCredentials) {
        loginCredentials.encryptedData?.let { encryptedData ->
            login(loginCredentials, isWatchOnly = true, updateWatchOnlyPassword = false) {

                initialAction.postValue(true)

                val password = String(appKeystore.decryptData(encryptedData))
                session.loginWatchOnly(wallet, wallet.watchOnlyUsername!!, password)
            }
        }
    }

    fun loginWithBiometrics(cipher: Cipher, loginCredentials: LoginCredentials) {
        loginCredentials.encryptedData?.also { encryptedData ->
            val pin = String(appKeystore.decryptData(cipher, encryptedData))
            loginWithPin(pin, loginCredentials)
        }
    }

    fun loginWithBiometricsV3(cipher: Cipher, loginCredentials: LoginCredentials) {
        loginCredentials.encryptedData?.also { encryptedData ->
            val decrypted = appKeystore.decryptData(cipher, encryptedData)
            // Migrated from v3
            val pin = Base64.encodeToString(decrypted, Base64.NO_WRAP).substring(0, 15)
            loginWithPin(pin, loginCredentials)
        }
    }

    fun watchOnlyLogin() {
        login(null, isWatchOnly = true, updateWatchOnlyPassword = true) {
            session.loginWatchOnly(
                wallet,
                wallet.watchOnlyUsername!!,
                watchOnlyPassword.value ?: ""
            )
        }
    }

    private fun login(
        loginCredentials: LoginCredentials? = null,
        isWatchOnly: Boolean = false,
        updateWatchOnlyPassword: Boolean = false,
        logInMethod: (GdkSession) -> Unit
    ) {

        doUserAction({
            logInMethod.invoke(session)

            // Migrate - add walletHashId
            if (wallet.walletHashId != session.walletHashId) {
                wallet.walletHashId = session.walletHashId ?: ""
                if (!wallet.isEphemeral) {
                    walletRepository.updateWallet(wallet)
                }
            }

            // Change active account if necessary (account archived)
            if (wallet.activeNetwork != (session.activeAccountOrNull?.networkId ?: "")||
                wallet.activeAccount != (session.activeAccountOrNull?.pointer ?: 0)
            ) {
                wallet.activeNetwork = session.activeAccountOrNull?.networkId ?: session.defaultNetwork.id
                wallet.activeAccount = session.activeAccountOrNull?.pointer ?: 0

                if(!wallet.isEphemeral) {
                    walletRepository.updateWallet(wallet)
                }
            }

            // Reset counter
            loginCredentials?.also{
                it.counter = 0
                walletRepository.updateLoginCredentials(it)
            }

            // Update watch-only password if needed
            if(updateWatchOnlyPassword){
                keystoreCredentials.value?.let {
                    it.encryptedData = appKeystore.encryptData(watchOnlyPassword.value!!.toByteArray())
                    walletRepository.updateLoginCredentials(it)
                }
            }

            if(isBip39Login){
                val network = session.defaultNetwork

                val mnemonic = session.getCredentials().mnemonic

                // Disconnect as no longer needed
                session.disconnectAsync()

                val walletName = wallet.name

                val ephemeralWallet = Wallet.createEphemeralWallet(
                    ephemeralId = sessionManager.getNextEphemeralId(),
                    name = walletName,
                    networkId = network.id,
                    isHardware = false
                )

                // Create an ephemeral session
                val ephemeralSession = sessionManager.getWalletSession(ephemeralWallet)

                // Set Ephemeral wallet
                ephemeralSession.ephemeralWallet = ephemeralWallet

                val loginData = ephemeralSession.loginWithMnemonic(
                    isTestnet = wallet.isTestnet,
                    loginCredentialsParams = LoginCredentialsParams(
                        mnemonic = mnemonic,
                        bip39Passphrase = bip39Passphrase.string().trim()
                    ),
                    initializeSession = true,
                    isSmartDiscovery = true,
                    isCreate = false,
                    isRestore = false
                )

                // Check if there is already a BIP39 ephemeral wallet
                sessionManager.getEphemeralWalletSession(loginData.walletHashId)?.let {
                    // Disconnect the no longer needed session
                    ephemeralSession.disconnectAsync()

                    // Return the previous connected BIP39 ephemeral session
                    it.ephemeralWallet!! to it
                } ?: run {
                    ephemeralWallet.walletHashId = loginData.walletHashId

                    // Return the ephemeral wallet
                    ephemeralWallet to ephemeralSession
                }
            }else{
                // Return the wallet
                wallet to session
            }
        }, postAction = {
            onProgress.value = it == null
        }, onSuccess = { pair ->
            countly.loginWallet(
                wallet = pair.first,
                session = pair.second,
                loginCredentials = loginCredentials
            )
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(pair.first)))
        }, onError = {
            if(it.isNotAuthorized()){
                loginCredentials?.also{ loginCredentials ->
                    loginCredentials.counter += 1

                    viewModelScope.launch {
                        if (loginCredentials.counter < 3) {
                            walletRepository.updateLoginCredentials(loginCredentials)
                        } else {
                            walletRepository.deleteLoginCredentials(loginCredentials)
                        }
                    }
                }

                if(isWatchOnly){
                    onErrorMessage.postValue(ConsumableEvent(it))
                }

            }else if(isBip39Login && it.message == "id_login_failed"){
                // On Multisig & BIP39 Passphrase login, instead of registering a new wallet, show error "Wallet not found"
                // Jade users restoring can still login
                onErrorMessage.postValue(ConsumableEvent(Exception("id_wallet_not_found")))
            }else{
                onErrorMessage.postValue(ConsumableEvent(it))
            }

            onError.postValue(ConsumableEvent(it))
            countly.failedWalletLogin(session, it)
        })
    }

    fun deleteLoginCredentials(loginCredentials: LoginCredentials){
        applicationScope.launch(context = logException(countly)) {
            walletRepository.deleteLoginCredentials(loginCredentials)
        }
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
            device: Device?
        ): LoginViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            device: Device?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, device) as T
            }
        }
    }
}