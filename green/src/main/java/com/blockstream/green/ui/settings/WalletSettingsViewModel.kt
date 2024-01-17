package com.blockstream.green.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.database.LoginCredentials
import com.blockstream.common.extensions.biometricsPinData
import com.blockstream.common.extensions.createLoginCredentials
import com.blockstream.common.extensions.lightningMnemonic
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.Settings
import com.blockstream.common.gdk.data.SettingsNotification
import com.blockstream.common.gdk.data.TwoFactorConfig
import com.blockstream.common.gdk.data.TwoFactorMethodConfig
import com.blockstream.common.gdk.data.TwoFactorReset
import com.blockstream.common.gdk.params.CsvParams
import com.blockstream.common.gdk.params.EncryptWithPinParams
import com.blockstream.common.gdk.params.Limits
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.randomChars
import com.blockstream.green.data.TwoFactorMethod
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import javax.crypto.Cipher

@KoinViewModel
open class WalletSettingsViewModel constructor(
    @InjectedParam wallet: GreenWallet
) : AbstractWalletViewModel(wallet) {
    private var _networkSettingsLiveData = mutableMapOf<Network, MutableLiveData<Settings>>()

    fun networkSettingsLiveData(network: Network) =
        _networkSettingsLiveData.getOrPut(network) { MutableLiveData<Settings>() }

    fun networkSettings(network: Network) = networkSettingsLiveData(network).value

    val prominentNetworkSettings get() = networkSettingsLiveData(session.defaultNetwork)

    private var _networkTwoFactorConfigLiveData =
        mutableMapOf<Network, MutableLiveData<TwoFactorConfig>>()

    fun networkTwoFactorConfigLiveData(network: Network) =
        _networkTwoFactorConfigLiveData.getOrPut(network) { MutableLiveData<TwoFactorConfig>() }

    fun networkTwoFactorConfig(network: Network) = networkTwoFactorConfigLiveData(network).value

    val biometricsLiveData = MutableLiveData<LoginCredentials?>()

    val lightningShortcutLiveData = MutableLiveData<LoginCredentials?>()

    private val _archivedAccountsLiveData: MutableLiveData<Int> = MutableLiveData(0)
    val archivedAccountsLiveData: LiveData<Int> get() = _archivedAccountsLiveData
    val archivedAccounts: Int get() = _archivedAccountsLiveData.value ?: 0

    init {
        session.activeSessions.forEach { network ->
            session
                .settings(network)
                .onEach {
                    networkSettingsLiveData(network).value = it
                }.launchIn(viewModelScope.coroutineScope)
        }

        session.accounts.value.find {
            it.isMultisig
        }

        database.getLoginCredentialsFlow(wallet.id).onEach {
            biometricsLiveData.postValue(it.biometricsPinData)
            lightningShortcutLiveData.postValue(it.lightningMnemonic)
        }.launchIn(viewModelScope.coroutineScope)

        session
            .allAccounts
            .onEach { accounts ->
                _archivedAccountsLiveData.value = accounts.count { it.hidden }
            }.launchIn(viewModelScope.coroutineScope)

        updateTwoFactorConfig()
        // session.updateWatchOnlyUsername()
    }

    fun updateTwoFactorConfig() {
        if (!session.isWatchOnly) {
            viewModelScope.coroutineScope.launch(context = Dispatchers.IO + logException(countly)) {
                session.activeSessions.filter { !it.isElectrum }.forEach {
                    networkTwoFactorConfigLiveData(it).postValue(session.getTwoFactorConfig(it))
                }
            }
        }
    }

    fun updateTwoFactorConfig(network: Network) {
        if (!session.isWatchOnly) {
            viewModelScope.coroutineScope.launch(context = Dispatchers.IO + logException(countly)) {
                networkTwoFactorConfigLiveData(network).postValue(session.getTwoFactorConfig(network))
            }
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
        isSmsBackup: Boolean,
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
                        data = data,
                        isSmsBackup = isSmsBackup
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
            postSideEffect(SideEffects.Success())
            if(isSmsBackup){
                postSideEffect(SideEffects.Snackbar("id_2fa_call_is_now_enabled"))
            }
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
            postSideEffect(SideEffects.Success())
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
                .twoFactorReset(network, email, isDispute)
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
            database.replaceLoginCredential(
                createLoginCredentials(
                    walletId = wallet.id,
                    network = encryptWithPin.network.id,
                    credentialType = CredentialType.PIN_PINDATA,
                    pinData = encryptWithPin.pinData
                )
            )

            // We only allow one credential type PIN / Password
            // Password comes from v2 and should be deleted when a user tries to change his
            // password to a pin
            database.deleteLoginCredentials(wallet.id, CredentialType.PASSWORD_PINDATA)
        }, onSuccess = {
            postSideEffect(SideEffects.Snackbar("id_you_have_successfully_changed"))
            postSideEffect(SideEffects.Success())
        })
    }

    override fun saveGlobalSettings(newSettings: Settings, onSuccess: (() -> Unit)?) {
        super.saveGlobalSettings(newSettings){
            postSideEffect(SideEffects.Success())
        }
    }

    fun saveNetworkSettings(network: Network, newSettings: Settings) {
        doUserAction({
            session.changeSettings(network, newSettings)
            session.updateSettings(network)
        }, onSuccess = {
            postSideEffect(SideEffects.Success())
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
            postSideEffect(SideEffects.Success())
        })
    }

    fun setCsvTime(network: Network, csvTime: Int, twoFactorResolver: DialogTwoFactorResolver) {
        doUserAction({
            session.setCsvTime(network, CsvParams(csvTime)).resolve(twoFactorResolver = twoFactorResolver)
            session.updateSettings(network)
        }, onSuccess = {
            postSideEffect(SideEffects.Success())
        })
    }

    fun enableBiometrics(cipher: Cipher) {
        doUserAction({
            val pin = randomChars(15)
            val credentials = session.getCredentials()
            val encryptWithPin =
                session.encryptWithPin(null, EncryptWithPinParams(pin, credentials))

            val encryptedData = greenKeystore.encryptData(cipher, pin.toByteArray())

            database.replaceLoginCredential(
                createLoginCredentials(
                    walletId = wallet.id,
                    network = encryptWithPin.network.id,
                    credentialType = CredentialType.BIOMETRICS_PINDATA,
                    pinData = encryptWithPin.pinData,
                    encryptedData = encryptedData
                )
            )
        }, onSuccess = {

        })
    }

    fun removeBiometrics() {
        applicationScope.launch(context = logException(countly)) {
            database.deleteLoginCredentials(wallet.id, CredentialType.BIOMETRICS_PINDATA)
        }
    }
}