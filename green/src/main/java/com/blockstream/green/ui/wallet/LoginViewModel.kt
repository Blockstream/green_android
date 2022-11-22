package com.blockstream.green.ui.wallet

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
import com.blockstream.green.gdk.*
import com.blockstream.green.lifecycle.PendingLiveData
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.logException
import com.blockstream.green.utils.string
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.launch
import javax.crypto.Cipher

class LoginViewModel @AssistedInject constructor(
    private var appKeystore: AppKeystore,
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    private val applicationScope: ApplicationScope,
    @Assisted wallet: Wallet,
    @Assisted val device: Device?
) : AbstractWalletViewModel(sessionManager, walletRepository, countly, wallet) {

    sealed class LoginEvent: AppEvent {
        data class LaunchBiometrics(val loginCredentials: LoginCredentials) : WalletEvent()
        object LoginDevice : WalletEvent()
        object AskBip39Passphrase : WalletEvent()
    }

    val onErrorMessage = MutableLiveData<ConsumableEvent<Throwable>>()

    var biometricsCredentials: MutableLiveData<LoginCredentials> = MutableLiveData()
    var keystoreCredentials: MutableLiveData<LoginCredentials> = MutableLiveData()
    var pinCredentials: PendingLiveData<LoginCredentials> = PendingLiveData()
    var passwordCredentials: PendingLiveData<LoginCredentials> = PendingLiveData()

    var password = MutableLiveData("")
    var watchOnlyPassword = MutableLiveData("")

    val torEvent: MutableLiveData<TorEvent> = MutableLiveData()

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
            walletRepository
                .getWalletLoginCredentialsObservable(wallet.id)
                .async()
                .subscribeBy(
                    onError = {
                        it.printStackTrace()
                    },
                    onNext = {
                        loginCredentialsInitialized = true
                        biometricsCredentials.value = it.biometrics
                        keystoreCredentials.value = it.keystore
                        pinCredentials.value = it.pin
                        passwordCredentials.value = it.password

                        if(initialAction.value == false && !wallet.askForBip39Passphrase){
                            it.biometrics?.let { biometricsCredentials ->
                                onEvent.postValue(ConsumableEvent(LoginEvent.LaunchBiometrics(biometricsCredentials)))
                                initialAction.postValue(true)
                            }
                        }
                    }
                ).addTo(disposables)

            session.getTorStatusObservable()
                .async()
                .subscribe {
                    torEvent.value = it
                }.addTo(disposables)
        }
    }

    fun setBip39Passphrase(passphrase: String?, alwaysAsk: Boolean){
        bip39Passphrase.value = passphrase?.trim()
        viewModelScope.launch(context = logException(countly)) {
            wallet.askForBip39Passphrase = alwaysAsk
            walletRepository.updateWalletSuspend(wallet)
        }

        if(initialAction.value == false){
            biometricsCredentials.value?.let { biometricsCredentials ->
                onEvent.postValue(ConsumableEvent(LoginEvent.LaunchBiometrics(biometricsCredentials)))
                initialAction.postValue(true)
            }
        }
    }

    private fun emergencyRecoveryPhrase(pin: String, loginCredentials: LoginCredentials) {
        Single.just(session)
            .subscribeOn(Schedulers.io())
            .map {
                it.emergencyRestoreOfRecoveryPhrase(
                    wallet = wallet,
                    pin = pin,
                    loginCredentials = loginCredentials
                ).also {
                    session.disconnect()
                }
            }
            .doOnError {
                it.printStackTrace()
                // isNotAuthorized only catches multisig/singlesig login errors, watchonly are not caught
                // and the counter is not incremented
                if (it.isNotAuthorized()) {
                    loginCredentials.counter += 1

                    if (loginCredentials.counter < 3) {
                        walletRepository.updateLoginCredentialsSync(loginCredentials)
                    } else {
                        walletRepository.deleteLoginCredentialsSync(loginCredentials)
                    }
                } else {
                    onErrorMessage.postValue(ConsumableEvent(it))
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                onProgress.postValue(true)
            }
            .doOnTerminate {
                onProgress.postValue(false)
            }
            .subscribeBy(
                onError = {
                    onError.postValue(ConsumableEvent(it))
                    countly.failedWalletLogin(session, it)
                },
                onSuccess = { credentials ->
                    onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(credentials)))
                    isEmergencyRecoveryPhrase.postValue(false)
                }
            )
    }

    fun loginWithPin(pin: String, loginCredentials: LoginCredentials) {
        if(isEmergencyRecoveryPhrase.value == true){
            emergencyRecoveryPhrase(pin, loginCredentials)
            return
        }

        login(loginCredentials) {
            // if bip39 passphrase, don't initialize the session as we need to re-connect || initializeSession = bip39Passphrase.isNullOrBlank())
            session.loginWithPin(wallet, pin, loginCredentials.pinData!!, initializeSession = !isBip39Login)
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
        if(isEmergencyRecoveryPhrase.value == true){
            val pin = String(appKeystore.decryptData(cipher, loginCredentials.encryptedData!!))
            emergencyRecoveryPhrase(pin, loginCredentials)
            return
        }

        loginCredentials.encryptedData?.let { encryptedData ->
            login(loginCredentials) {
                val pin = String(appKeystore.decryptData(cipher, encryptedData))
                // if bip39 passphrase, don't initialize the session as we need to re-connect
                session.loginWithPin(wallet, pin, loginCredentials.pinData!!, initializeSession = !isBip39Login)
            }
        }
    }

    fun loginWithBiometricsV3(cipher: Cipher, loginCredentials: LoginCredentials) {
        if(isEmergencyRecoveryPhrase.value == true){
            val decrypted = appKeystore.decryptData(cipher, loginCredentials.encryptedData!!)
            // Migrated from v3
            val pin = Base64.encodeToString(decrypted, Base64.NO_WRAP).substring(0, 15)
            emergencyRecoveryPhrase(pin, loginCredentials)
            return
        }

        loginCredentials.encryptedData?.let { encryptedData ->
            login(loginCredentials) {
                val decrypted = appKeystore.decryptData(cipher, encryptedData)
                // Migrated from v3
                val pin = Base64.encodeToString(decrypted, Base64.NO_WRAP).substring(0, 15)
                // if bip39 passphrase, don't initialize the session as we need to re-connect
                session.loginWithPin(wallet, pin, loginCredentials.pinData!!, initializeSession = !isBip39Login)
            }
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
        loginCredentials: LoginCredentials?,
        isWatchOnly: Boolean = false,
        updateWatchOnlyPassword: Boolean = false,
        mapper: (GreenSession) -> Unit
    ) {
        Single.just(session)
            .subscribeOn(Schedulers.io())
            .map(mapper)
            .map {

                // Migrate - add walletHashId
                if(wallet.walletHashId.isBlank()){
                    wallet.walletHashId = session.walletHashId ?: ""
                    walletRepository.updateWalletSync(wallet)
                }

                // Change active account if necessary (account archived)
                if(wallet.activeAccount != session.activeAccount){
                    wallet.activeAccount = session.activeAccount
                    walletRepository.updateWalletSync(wallet)
                }

                // Reset counter
                loginCredentials?.also{
                    it.counter = 0
                    walletRepository.updateLoginCredentialsSync(it)
                }

                // Update watchonly password if needed
                if(updateWatchOnlyPassword){
                    keystoreCredentials.value?.let {
                        it.encryptedData = appKeystore.encryptData(watchOnlyPassword.value!!.toByteArray())
                        walletRepository.updateLoginCredentialsSync(it)
                    }
                }

                if(isBip39Login){
                    val network = session.network

                    val mnemonic = session.getCredentials().mnemonic

                    // Disconnect as no longer needed
                    session.disconnectAsync()

                    val walletName = wallet.name

                    val ephemeralWallet = Wallet.createEphemeralWallet(ephemeralId = sessionManager.getNextEphemeralId(), network = network, name = walletName, isHardware = false)

                    // Create an ephemeral session
                    val ephemeralSession = sessionManager.getWalletSession(ephemeralWallet)

                    // Set Ephemeral wallet
                    ephemeralSession.ephemeralWallet = ephemeralWallet

                    val loginData = ephemeralSession.loginWithMnemonic(network, LoginCredentialsParams(mnemonic = mnemonic, bip39Passphrase = bip39Passphrase.string().trim()), initializeSession = true)

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
            }
            .doOnError {
                // isNotAuthorized only catches multisig/singlesig login errors, watchonly are not caught
                // and the counter is not incremented
                if(it.isNotAuthorized()){
                    loginCredentials?.also{ loginCredentials ->
                        loginCredentials.counter += 1

                        if(loginCredentials.counter < 3){
                            walletRepository.updateLoginCredentialsSync(loginCredentials)
                        }else{
                             walletRepository.deleteLoginCredentialsSync(loginCredentials)
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
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                onProgress.postValue(true)
            }
            .subscribeBy(
                onError = {
                    onError.postValue(ConsumableEvent(it))
                    // change inProgress only on error to avoid glitching the UI cause on success we continue to next screen
                    onProgress.postValue(false)
                    countly.failedWalletLogin(session, it)
                },
                onSuccess = { pair ->
                    countly.loginWallet(
                        wallet = pair.first,
                        session = pair.second,
                        loginCredentials = loginCredentials
                    )
                    onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(pair.first)))
                }
            )
    }

    fun loginWithDevice() {
        if(device == null) return

        session.observable {
            it.loginWithDevice(it.networks.getNetworkById(wallet.network),
                registerUser = true,
                device = device,
                hardwareWalletResolver = DeviceResolver(session, this)
            )

            // Change active account if necessary (account archived or the newly created SegWit)
            if(wallet.activeAccount != session.activeAccount){
                wallet.activeAccount = session.activeAccount
            }

        }
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe {
            onProgress.postValue(true)
        }
        .subscribeBy(
            onError = {
                onErrorMessage.postValue(ConsumableEvent(it))
                // change inProgress only on error to avoid glitching the UI cause on success we continue to next screen
                onProgress.postValue(false)
                countly.failedWalletLogin(session, it)
            },
            onSuccess = {
                countly.loginWallet(wallet = wallet, session = session)
                onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(wallet)))
            }
        )
    }

    fun deleteLoginCredentials(loginCredentials: LoginCredentials){
        applicationScope.launch(context = logException(countly)) {
            walletRepository.deleteLoginCredentialsSuspend(loginCredentials)
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