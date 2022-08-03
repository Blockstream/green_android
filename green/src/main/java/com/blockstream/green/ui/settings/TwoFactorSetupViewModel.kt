package com.blockstream.green.ui.settings

import android.graphics.Bitmap
import androidx.lifecycle.*
import com.blockstream.gdk.GdkBridge
import com.blockstream.gdk.data.Network
import com.blockstream.green.ApplicationScope
import com.blockstream.green.data.Countly
import com.blockstream.green.data.TwoFactorMethod
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.createQrBitmap

import com.blockstream.green.extensions.isEmailValid
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

class TwoFactorSetupViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    appKeystore: AppKeystore,
    gdkBridge: GdkBridge,
    applicationScope: ApplicationScope,
    @Assisted wallet: Wallet,
    @Assisted val network: Network,
    @Assisted val method: TwoFactorMethod,
    @Assisted val action: TwoFactorSetupAction
) : WalletSettingsViewModel(sessionManager, walletRepository, countly, appKeystore, gdkBridge, applicationScope, wallet) {

    var authenticatorUrl: String? = null
    val country = MutableLiveData("")
    val phoneNumber = MutableLiveData("")
    val email = MutableLiveData("")
    var authenticatorCode = MutableLiveData("")
    var authenticatorQRBitmap = MutableLiveData<Bitmap?>()

    val isValid: LiveData<Boolean> by lazy {
        MediatorLiveData<Boolean>().apply {
            val block = { _: Any? ->
                value = if(method == TwoFactorMethod.EMAIL){
                    email.value.isEmailValid()
                }else if(method == TwoFactorMethod.SMS || method == TwoFactorMethod.PHONE || method == TwoFactorMethod.TELEGRAM){
                    !country.value.isNullOrBlank() && (phoneNumber.value?.trim()?.length ?: 0) > 7
                }else{
                    true
                }
            }

            addSource(country, block)
            addSource(phoneNumber, block)
            addSource(email, block)
        }
    }

    init {
        if(method == TwoFactorMethod.AUTHENTICATOR){
            doUserAction({
                session.getTwoFactorConfig(network)
            }, preAction = null, postAction = null, onSuccess = {
                authenticatorUrl = it.gauth.data
                authenticatorQRBitmap.postValue(createQrBitmap(it.gauth.data))
                authenticatorCode.value = it.gauth.data.split("=").getOrNull(1)
            })
        }
    }

    fun getPhoneNumberValue() = "${country.value ?: ""}${phoneNumber.value ?: ""}"
    fun getEmailValue() = email.value ?: ""

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
            network: Network,
            method: TwoFactorMethod,
            action: TwoFactorSetupAction
        ): TwoFactorSetupViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            network: Network,
            method: TwoFactorMethod,
            action: TwoFactorSetupAction
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, network, method, action) as T
            }
        }
    }

}