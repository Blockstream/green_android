package com.blockstream.green.ui.wallet

import android.util.Base64
import androidx.lifecycle.*
import com.blockstream.gdk.HardwareWalletResolver
import com.blockstream.gdk.data.TORStatus
import com.blockstream.green.database.LoginCredentials
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.devices.Device
import com.blockstream.green.gdk.*
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.ConsumableEvent
import com.greenaddress.greenapi.HWWallet
import com.greenaddress.greenapi.HWWalletBridge
import com.greenaddress.greenbits.wallets.HardwareCodeResolver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.crypto.Cipher

class LoginViewModel @AssistedInject constructor(
    private var appKeystore: AppKeystore,
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    @Assisted wallet: Wallet,
    @Assisted val device: Device?
) : AbstractWalletViewModel(sessionManager, walletRepository, wallet) {

    val onErrorMessage = MutableLiveData<ConsumableEvent<Throwable>>()

    var biometricsCredentials: MutableLiveData<LoginCredentials> = MutableLiveData()
    var keystoreCredentials: MutableLiveData<LoginCredentials> = MutableLiveData()
    var pinCredentials: MutableLiveData<LoginCredentials> = MutableLiveData()
    var passwordCredentials: MutableLiveData<LoginCredentials> = MutableLiveData()

    var password = MutableLiveData("")
    var watchOnlyPassword = MutableLiveData("")

    val torStatus: MutableLiveData<TORStatus> = MutableLiveData()

    var actionLogin = MutableLiveData<Boolean>()

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
    var initialAction = MutableLiveData(false)

    var loginCredentialsInitialized = false

    init {
        if (session.isConnected) {
            actionLogin.value = true
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
                    biometricsCredentials.postValue(it.biometrics)
                    keystoreCredentials.postValue(it.keystore)
                    pinCredentials.postValue(it.pin)
                    passwordCredentials.postValue(it.password)
                }
            ).addTo(disposables)

        session.getTorStatusObservable()
            .async()
            .subscribe {
                torStatus.value = it
            }.addTo(disposables)

    }

    fun loginWithPin(pin: String, loginCredentials: LoginCredentials) {
        login(loginCredentials) {
            session.loginWithPin(wallet, pin, loginCredentials.pinData!!)
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
        loginCredentials.encryptedData?.let { encryptedData ->
            login(loginCredentials) {
                val pin = String(appKeystore.decryptData(cipher, encryptedData))
                session.loginWithPin(wallet, pin, loginCredentials.pinData!!)
            }
        }
    }

    fun loginWithBiometricsV3(cipher: Cipher, loginCredentials: LoginCredentials) {
        loginCredentials.encryptedData?.let { encryptedData ->
            login(loginCredentials) {
                val decrypted = appKeystore.decryptData(cipher, encryptedData)
                // Migrated from v3
                val pin = Base64.encodeToString(decrypted, Base64.NO_WRAP).substring(0, 15)
                session.loginWithPin(wallet, pin, loginCredentials.pinData!!)
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

                session
            }
            .doOnError {
                // isNotAuthorized only catches multisig login errors, singlesig or watchonly are not caught
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
                },
                onSuccess = {
                    actionLogin.postValue(true)
                }
            )
    }

    fun loginWithDevice(device: Device) {
        session.observable {
            it.loginWithDevice(it.networks.getNetworkById(wallet.network),
                registerUser = true,
                device = device,
                hardwareWalletResolver = HardwareCodeResolver(this, device.hwWallet)
            )
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
            },
            onSuccess = {
                actionLogin.postValue(true)
            }
        )
    }

    fun deleteLoginCredentials(loginCredentials: LoginCredentials){
        GlobalScope.launch {
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
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, device) as T
            }
        }
    }
}