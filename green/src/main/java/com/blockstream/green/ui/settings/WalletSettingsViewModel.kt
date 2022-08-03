package com.blockstream.green.ui.settings

import androidx.lifecycle.*
import com.blockstream.gdk.GdkBridge
import com.blockstream.gdk.data.*
import com.blockstream.gdk.params.EncryptWithPinParams
import com.blockstream.gdk.params.Limits
import com.blockstream.green.ApplicationScope
import com.blockstream.green.data.Countly
import com.blockstream.green.data.GdkEvent
import com.blockstream.green.data.TwoFactorMethod
import com.blockstream.green.database.CredentialType
import com.blockstream.green.database.LoginCredentials
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.extensions.logException
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.ConsumableEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.crypto.Cipher

open class WalletSettingsViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    val appKeystore: AppKeystore,
    val gdkBridge: GdkBridge,
    val applicationScope: ApplicationScope,
    @Assisted wallet: Wallet
) : AbstractWalletViewModel(sessionManager, walletRepository, countly, wallet) {
    private var _networkSettingsLiveData = mutableMapOf<Network, MutableLiveData<Settings>>()
    val networkSettingsLiveData
        get() = _networkSettingsLiveData

    fun networkSettingsLiveData(network: Network) =
        _networkSettingsLiveData.getOrPut(network) { MutableLiveData<Settings>() }

    fun networkSettings(network: Network) = networkSettingsLiveData(network).value

    val prominentNetworkSettings get() = networkSettingsLiveData(session.defaultNetwork)

    private var _networkTwoFactorConfigLiveData =
        mutableMapOf<Network, MutableLiveData<TwoFactorConfig>>()

    fun networkTwoFactorConfigLiveData(network: Network) =
        _networkTwoFactorConfigLiveData.getOrPut(network) { MutableLiveData<TwoFactorConfig>() }

    fun networkTwoFactorConfig(network: Network) = networkTwoFactorConfigLiveData(network).value

    private var _watchOnlyUsernameLiveData = mutableMapOf<Network, MutableLiveData<String>>()
    fun watchOnlyUsernameLiveData(network: Network) =
        _watchOnlyUsernameLiveData.getOrPut(network) { MutableLiveData<String>() }

    val biometricsLiveData = MutableLiveData<LoginCredentials>()

    private val _archivedAccountsLiveData: MutableLiveData<Int> = MutableLiveData(0)
    val archivedAccountsLiveData: LiveData<Int> get() = _archivedAccountsLiveData
    val archivedAccounts: Int get() = _archivedAccountsLiveData.value ?: 0

    var supportId: String? = null

    init {
        session.activeSessions.forEach { network ->
            session
                .settingsFlow(network)
                .onEach {
                    networkSettingsLiveData(network).value = it
                }.launchIn(viewModelScope)
        }

        session.accountsFlow.value.find {
            it.isMultisig
        }

        session.accountsFlow.onEach { accounts ->
            supportId = accounts.filter { it.isMultisig && it.pointer == 0L }
                .joinToString(",") { "${it.network.bip21Prefix}:${it.receivingId}" }
        }.launchIn(viewModelScope)

        walletRepository
            .getWalletLoginCredentialsFlow(wallet.id).filterNotNull()
            .onEach {
                biometricsLiveData.postValue(it.biometrics)
            }
            .launchIn(viewModelScope)

        session
            .allAccountsFlow
            .onEach { accounts ->
                _archivedAccountsLiveData.value = accounts.count { it.hidden }
            }.launchIn(lifecycleScope)

        updateTwoFactorConfig()
        updateWatchOnlyUsername()
    }

    fun updateTwoFactorConfig() {
        if (!session.isWatchOnly) {
            viewModelScope.launch(context = Dispatchers.IO + logException(countly)) {
                session.activeSessions.filter { !it.isElectrum }.forEach {
                    networkTwoFactorConfigLiveData(it).postValue(session.getTwoFactorConfig(it))
                }
            }
        }
    }

    fun updateTwoFactorConfig(network: Network) {
        if (!session.isWatchOnly) {
            viewModelScope.launch(context = Dispatchers.IO + logException(countly)) {
                networkTwoFactorConfigLiveData(network).postValue(session.getTwoFactorConfig(network))
            }
        }
    }

    fun updateWatchOnlyUsername() {
        if (!session.isWatchOnly) {
            doUserAction({
                session.activeSessions.filter { !it.isElectrum }.toList().map { network ->
                    network to session.getWatchOnlyUsername(network)
                }.toMap()
            }, onError = {
                onError.postValue(ConsumableEvent(Exception("id_username_not_available")))
            }, onSuccess = {
                it.forEach {
                    watchOnlyUsernameLiveData(it.key).postValue(it.value)
                }
            })
        }
    }

    fun setWatchOnly(network: Network, username: String, password: String) {
        doUserAction({
            session.setWatchOnly(
                network,
                username,
                password
            )
        }, onSuccess = {
            updateWatchOnlyUsername()
        })
    }

    fun setLimits(network: Network, limits: Limits, twoFactorResolver: DialogTwoFactorResolver) {
        doUserAction({
            session.twofactorChangeLimits(network, limits)
                .result<Limits>(twoFactorResolver = twoFactorResolver)
        }, onSuccess = {
            updateTwoFactorConfig()
        })
    }

    fun sendNlocktimes(network: Network) {
        doUserAction({
            session.sendNlocktimes(network)
        }, onSuccess = {

        })
    }

    fun enable2FA(
        network: Network,
        method: TwoFactorMethod,
        data: String,
        action: TwoFactorSetupAction,
        twoFactorResolver: DialogTwoFactorResolver
    ) {
        doUserAction({
            session
                .changeSettingsTwoFactor(
                    network,
                    method.gdkType,
                    TwoFactorMethodConfig(
                        confirmed = true,
                        enabled = action != TwoFactorSetupAction.SETUP_EMAIL,
                        data = data
                    )
                )
                .resolve(twoFactorResolver = twoFactorResolver)

            // Enable legacy recovery emails
            if (action == TwoFactorSetupAction.SETUP_EMAIL) {
                networkSettingsLiveData(network).value?.copy(
                    notifications = SettingsNotification(
                        emailIncoming = true,
                        emailOutgoing = true
                    )
                )?.also { newSettings ->
                    session.changeSettings(network, newSettings)
                    session.updateSettings(network)
                }
            }
        }, onSuccess = {
            updateTwoFactorConfig()
            onEvent.postValue(ConsumableEvent(GdkEvent.Success))
        })
    }

    fun disable2FA(
        network: Network,
        method: TwoFactorMethod,
        twoFactorResolver: DialogTwoFactorResolver
    ) {
        doUserAction({
            session
                .changeSettingsTwoFactor(
                    network,
                    method.gdkType,
                    TwoFactorMethodConfig(enabled = false)
                )
                .resolve(twoFactorResolver = twoFactorResolver)
        }, onSuccess = {
            updateTwoFactorConfig()
            onEvent.postValue(ConsumableEvent(GdkEvent.Success))
        })
    }

    fun reset2FA(
        network: Network,
        email: String,
        isDispute: Boolean,
        twoFactorResolver: DialogTwoFactorResolver
    ) {
        doUserAction({
            session
                .twofactorReset(network, email, isDispute)
                .result<TwoFactorReset>(twoFactorResolver = twoFactorResolver)
        }, onSuccess = {
            logout(LogoutReason.USER_ACTION)
        })
    }

    fun undoReset2FA(network: Network, email: String, twoFactorResolver: DialogTwoFactorResolver) {
        doUserAction({
            session
                .twofactorUndoReset(network, email)
                .resolve(twoFactorResolver = twoFactorResolver)
        }, onSuccess = {
            logout(LogoutReason.USER_ACTION)
        })
    }

    fun cancel2FA(network: Network, twoFactorResolver: DialogTwoFactorResolver) {
        doUserAction({
            session
                .twofactorCancelReset(network)
                .resolve(twoFactorResolver = twoFactorResolver)
        }, onSuccess = {
            logout(LogoutReason.USER_ACTION)
        })
    }

    fun changePin(newPin: String) {
        doUserAction({
            val credentials = session.getCredentials()
            val encryptWithPin =
                session.encryptWithPin(null, EncryptWithPinParams(newPin, credentials))

            // Replace PinData
            walletRepository.insertOrReplaceLoginCredentials(
                LoginCredentials(
                    walletId = wallet.id,
                    network = encryptWithPin.network.id,
                    credentialType = CredentialType.PIN,
                    pinData = encryptWithPin.pinData
                )
            )

            // We only allow one credential type PIN / Password
            // Password comes from v2 and should be deleted when a user tries to change his
            // password to a pin
            walletRepository.deleteLoginCredentials(wallet.id, CredentialType.PASSWORD)
        }, onSuccess = {
            onEvent.postValue(ConsumableEvent(GdkEvent.Success))
        })
    }

    fun saveGlobalSettings(newSettings: Settings) {
        doUserAction({
            session.changeGlobalSettings(newSettings)
            session.updateSettings()
        }, onSuccess = {
            updateWatchOnlyUsername()
            onEvent.postValue(ConsumableEvent(GdkEvent.Success))
        })
    }

    fun saveNetworkSettings(network: Network, newSettings: Settings) {
        doUserAction({
            session.changeSettings(network, newSettings)
            session.updateSettings(network)
        }, onSuccess = {
            updateWatchOnlyUsername()
            onEvent.postValue(ConsumableEvent(GdkEvent.Success))
        })
    }

    fun savePGP(pgp: String?) {
        doUserAction({
            pgp?.trim().also { pgpTrimmed ->
                session.activeMultisig.forEach {
                    networkSettingsLiveData(it).value?.also { settings ->
                        session.changeSettings(it, settings.copy(pgp = pgpTrimmed))
                        session.updateSettings(it)
                    }
                }
            }
        }, onSuccess = {
            updateWatchOnlyUsername()
            onEvent.postValue(ConsumableEvent(GdkEvent.Success))
        })
    }

    fun setCsvTime(network: Network, csvTime: Int, twoFactorResolver: DialogTwoFactorResolver) {
        doUserAction({
            session.setCsvTime(network, csvTime).resolve(twoFactorResolver = twoFactorResolver)
            session.updateSettings(network)
        }, onSuccess = {
            onEvent.postValue(ConsumableEvent(GdkEvent.Success))
        })
    }

    fun enableBiometrics(cipher: Cipher) {
        doUserAction({
            val pin = gdkBridge.randomChars(15)
            val credentials = session.getCredentials()
            val encryptWithPin =
                session.encryptWithPin(null, EncryptWithPinParams(pin, credentials))

            val encryptedData = appKeystore.encryptData(cipher, pin.toByteArray())

            walletRepository.insertOrReplaceLoginCredentials(
                LoginCredentials(
                    walletId = wallet.id,
                    network = encryptWithPin.network.id,
                    credentialType = CredentialType.BIOMETRICS,
                    pinData = encryptWithPin.pinData,
                    encryptedData = encryptedData
                )
            )
        }, onSuccess = {

        })
    }

    fun removeBiometrics() {
        applicationScope.launch(context = logException(countly)) {
            walletRepository.deleteLoginCredentials(wallet.id, CredentialType.BIOMETRICS)
        }
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet
        ): WalletSettingsViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet) as T
            }
        }
    }

}