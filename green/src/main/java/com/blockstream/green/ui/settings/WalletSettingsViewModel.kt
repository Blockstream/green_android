package com.blockstream.green.ui.settings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.Settings
import com.blockstream.gdk.data.TwoFactorConfig
import com.blockstream.gdk.data.TwoFactorMethodConfig
import com.blockstream.gdk.params.Limits
import com.blockstream.green.R
import com.blockstream.green.database.CredentialType
import com.blockstream.green.database.LoginCredentials
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.ui.wallet.WalletViewModel
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.ConsumableEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import javax.crypto.Cipher

class WalletSettingsViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    val appKeystore: AppKeystore,
    val greenWallet: GreenWallet,
    @Assisted wallet: Wallet
) : WalletViewModel(sessionManager, walletRepository, wallet) {

    val onErrorStringRes = MutableLiveData<ConsumableEvent<Int>>()

    val settingsLiveData = MutableLiveData<Settings>()
    val twoFactorConfigLiveData = MutableLiveData<TwoFactorConfig>()
    val watchOnlyUsernameLiveData = MutableLiveData("")
    val biometricsLiveData = MutableLiveData<LoginCredentials>()

    init {
        session
            .getSettingsObservable()
            .subscribe(settingsLiveData::setValue)
            .addTo(disposables)

        walletRepository
            .getWalletLoginCredentialsObservable(wallet.id)
            .subscribe {
                biometricsLiveData.postValue(it.biometrics)
            }
            .addTo(disposables)

        updateTwoFactorConfig()
        updateWatchOnlyUsername()
    }

    fun updateTwoFactorConfig(){
        if(!session.isElectrum){
            session.observable {
                it.getTwoFactorConfig()
            }.subscribeBy(
                onSuccess = {
                    twoFactorConfigLiveData.value = it
                },
                onError = {
                    it.printStackTrace()
                }
            )
        }
    }

    fun updateWatchOnlyUsername(){
        session.observable {
            it.getWatchOnlyUsername()
        }.subscribeBy(
            onSuccess = {
                watchOnlyUsernameLiveData.value = it
            }
        )
    }

    fun setWatchOnly(username: String, password: String){
        session.observable {
            it.setWatchOnly(
                username,
                password
            )
        }.subscribeBy(
            onError = {
                onErrorStringRes.value = ConsumableEvent(R.string.id_username_not_available)
            },
            onSuccess = {
                updateWatchOnlyUsername()
            }
        )
    }

    fun setLimits(limits: JsonElement, twoFactorResolver: DialogTwoFactorResolver){
        session.observable {
            session.twofactorChangeLimits(limits).result<Limits>(twoFactorResolver = twoFactorResolver)
        }.subscribeBy(
            onError = {
                onError.value = ConsumableEvent(it)
            },
            onSuccess = {
                updateTwoFactorConfig()
            }
        )
    }

    fun disable2FA(method: String, twoFactorResolver: DialogTwoFactorResolver){
        session.observable {
            session
                .changeSettingsTwoFactor(method, TwoFactorMethodConfig(enabled = false))
                .resolve(twoFactorResolver = twoFactorResolver)
        }.subscribeBy(
            onError = {
                onError.value = ConsumableEvent(it)
            },
            onSuccess = {
                updateTwoFactorConfig()
            }
        )
    }

    fun changePin(newPin: String){
        session.observable {
            val pinData = it.setPin(newPin)

            // Replace PinData
            walletRepository.addLoginCredentials(
                LoginCredentials(
                    walletId = wallet.id,
                    credentialType = CredentialType.PIN,
                    pinData = pinData
                )
            )

            // We only allow one credential type PIN / Password
            // Password comes from v2 and should be deleted when a user tries to change his
            // password to a pin
            walletRepository.deleteLoginCredentialsSync(wallet.id, CredentialType.PASSWORD)

        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onError = {
                onError.postValue(ConsumableEvent(it))
            },
            onSuccess = {
                onEvent.postValue(ConsumableEvent(true))
            }
        )
    }

    fun saveSettings(newSettings: Settings){
        session.observable {
            it.changeSettings(newSettings).resolve()
            it.updateSettings()
        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onError = {
                onError.postValue(ConsumableEvent(it))
            },
            onSuccess = {
                updateWatchOnlyUsername()
                onEvent.postValue(ConsumableEvent(true))
            }
        )
    }

    fun enableBiometrics(cipher: Cipher) {
        session.observable {
            val pin = greenWallet.randomChars(15)
            val pinData = it.setPin(pin)

            val encryptedData = appKeystore.encryptData(cipher, pin.toByteArray())

            walletRepository.addLoginCredentials(
                LoginCredentials(
                    walletId = wallet.id,
                    credentialType = CredentialType.BIOMETRICS,
                    pinData = pinData,
                    encryptedData = encryptedData
                )
            )

            true
        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onError = {
                onError.postValue(ConsumableEvent(it))
            },
            onSuccess = {

            }
        )
    }

    fun removeBiometrics(){
        GlobalScope.launch {
            walletRepository.deleteLoginCredentialsSuspend(wallet.id, CredentialType.BIOMETRICS)
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
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet) as T
            }
        }
    }

}