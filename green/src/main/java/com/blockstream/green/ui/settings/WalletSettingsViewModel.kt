package com.blockstream.green.ui.settings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.GreenWallet
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
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.async
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.logException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.launch
import javax.crypto.Cipher

open class WalletSettingsViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    val appKeystore: AppKeystore,
    val greenWallet: GreenWallet,
    val applicationScope: ApplicationScope,
    @Assisted wallet: Wallet
) : AbstractWalletViewModel(sessionManager, walletRepository, countly, wallet) {
    val settingsLiveData = MutableLiveData<Settings>()
    val twoFactorConfigLiveData = MutableLiveData<TwoFactorConfig>()
    val watchOnlyUsernameLiveData = MutableLiveData("")
    val biometricsLiveData = MutableLiveData<LoginCredentials>()

    var zeroSubaccount : SubAccount? = null

    init {
        session
            .getSettingsObservable()
            .async()
            .subscribe(settingsLiveData::postValue)
            .addTo(disposables)

        session
            .getSubAccountsObservable()
            .async()
            .subscribe{
                zeroSubaccount = it.firstOrNull()
            }
            .addTo(disposables)

        walletRepository
            .getWalletLoginCredentialsObservable(wallet.id)
            .async()
            .subscribe {
                biometricsLiveData.postValue(it.biometrics)
            }
            .addTo(disposables)

        updateTwoFactorConfig()
        updateWatchOnlyUsername()
    }

    fun updateTwoFactorConfig(){
        if(!session.isElectrum && !session.isWatchOnly){
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
        if(!session.isWatchOnly) {
            session.observable {
                it.getWatchOnlyUsername()
            }.subscribeBy(
                onError = {
                    onError.postValue(ConsumableEvent(Exception("id_username_not_available")))
                },
                onSuccess = {
                    watchOnlyUsernameLiveData.value = it
                }
            )
        }
    }

    fun setWatchOnly(username: String, password: String){
        session.observable {
            it.setWatchOnly(
                username,
                password
            )
        }.subscribeBy(
            onError = {
                onError.value = ConsumableEvent(it)
            },
            onSuccess = {
                updateWatchOnlyUsername()
            }
        )
    }

    fun setLimits(limits: Limits, twoFactorResolver: DialogTwoFactorResolver){
        session.observable {
            session.twofactorChangeLimits(limits).result<Limits>(twoFactorResolver = twoFactorResolver)
        }.subscribeBy(
            onError = {
                it.printStackTrace()
                onError.value = ConsumableEvent(it)
            },
            onSuccess = {
                updateTwoFactorConfig()
            }
        )
    }

    fun sendNlocktimes(){
        session.observable {
            session.sendNlocktimes()
        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onError = {
                onError.value = ConsumableEvent(it)
            },
            onSuccess = {

            }
        )
    }

    fun enable2FA(method: TwoFactorMethod, data: String, action: TwoFactorSetupAction, twoFactorResolver: DialogTwoFactorResolver){
        session.observable {
            session
                .changeSettingsTwoFactor(method.gdkType, TwoFactorMethodConfig(confirmed = true, enabled = action != TwoFactorSetupAction.SETUP_EMAIL, data = data))
                .resolve(twoFactorResolver = twoFactorResolver)

            // Enable legacy recovery emails
            if(action == TwoFactorSetupAction.SETUP_EMAIL){
                settingsLiveData.value?.copy(
                    notifications = SettingsNotification(
                        emailIncoming = true,
                        emailOutgoing = true
                    )
                )?.also { newSettings ->
                    it.changeSettings(newSettings).resolve()
                    it.updateSettings()
                }
            }
        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onError = {
                onError.value = ConsumableEvent(it)
            },
            onSuccess = {
                updateTwoFactorConfig()
                onEvent.postValue(ConsumableEvent(GdkEvent.Success))
            }
        )
    }

    fun disable2FA(method: TwoFactorMethod, twoFactorResolver: DialogTwoFactorResolver){
        session.observable {
            session
                .changeSettingsTwoFactor(method.gdkType, TwoFactorMethodConfig(enabled = false))
                .resolve(twoFactorResolver = twoFactorResolver)
        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onError = {
                onError.value = ConsumableEvent(it)
            },
            onSuccess = {
                updateTwoFactorConfig()
                onEvent.postValue(ConsumableEvent(GdkEvent.Success))
            }
        )
    }

    fun reset2FA(email: String, isDispute: Boolean, twoFactorResolver: DialogTwoFactorResolver){
        session.observable {
            session
                .twofactorReset(email, isDispute)
                .result<TwoFactorReset>(twoFactorResolver = twoFactorResolver)
        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onError = {
                onError.value = ConsumableEvent(it)
            },
            onSuccess = {
                logout(LogoutReason.USER_ACTION)
            }
        )
    }

    fun undoReset2FA(email: String, twoFactorResolver: DialogTwoFactorResolver){
        session.observable {
            session
                .twofactorUndoReset(email)
                .resolve(twoFactorResolver = twoFactorResolver)
        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onError = {
                onError.value = ConsumableEvent(it)
            },
            onSuccess = {
                logout(LogoutReason.USER_ACTION)
            }
        )
    }

    fun cancel2FA(twoFactorResolver: DialogTwoFactorResolver){
        session.observable {
            session
                .twofactorCancelReset()
                .resolve(twoFactorResolver = twoFactorResolver)
        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onError = {
                onError.value = ConsumableEvent(it)
            },
            onSuccess = {
                logout(LogoutReason.USER_ACTION)
            }
        )
    }

    fun changePin(newPin: String){
        session.observable {
            val credentials = it.getCredentials()
            val pinData = it.encryptWithPin(EncryptWithPinParams(newPin, credentials)).pinData

            // Replace PinData
            walletRepository.addLoginCredentialsSync(
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
                onEvent.postValue(ConsumableEvent(GdkEvent.Success))
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
                onEvent.postValue(ConsumableEvent(GdkEvent.Success))
            }
        )
    }

    fun setCsvTime(csvTime: Int, twoFactorResolver: DialogTwoFactorResolver){
        session.observable {
            it.setCsvTime(csvTime).resolve(twoFactorResolver = twoFactorResolver)

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
                onEvent.postValue(ConsumableEvent(GdkEvent.Success))
            }
        )
    }

    fun enableBiometrics(cipher: Cipher) {
        session.observable {
            val pin = greenWallet.randomChars(15)
            val credentials = it.getCredentials()
            val pinData = it.encryptWithPin(EncryptWithPinParams(pin, credentials)).pinData

            val encryptedData = appKeystore.encryptData(cipher, pin.toByteArray())

            walletRepository.addLoginCredentialsSync(
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
        applicationScope.launch(context = logException(countly)) {
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
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet) as T
            }
        }
    }

}