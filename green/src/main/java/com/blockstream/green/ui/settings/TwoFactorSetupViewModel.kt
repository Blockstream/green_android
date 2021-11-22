package com.blockstream.green.ui.settings

import android.graphics.Bitmap
import android.util.Patterns
import androidx.lifecycle.*
import com.blockstream.gdk.GreenWallet
import com.blockstream.green.ApplicationScope
import com.blockstream.green.data.TwoFactorMethod
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.observable
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.createQrBitmap
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.kotlin.subscribeBy

class TwoFactorSetupViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    appKeystore: AppKeystore,
    greenWallet: GreenWallet,
    applicationScope: ApplicationScope,
    @Assisted wallet: Wallet,
    @Assisted val method: TwoFactorMethod,
    @Assisted val action: TwoFactorSetupAction
) : WalletSettingsViewModel(sessionManager, walletRepository, appKeystore, greenWallet, applicationScope, wallet) {

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
                    Patterns.EMAIL_ADDRESS.matcher(email.value ?: "").matches()
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
            session.observable {
                it.getTwoFactorConfig()
            }.subscribeBy(
                onSuccess = {
                    authenticatorUrl = it.gauth.data
                    authenticatorQRBitmap.postValue(createQrBitmap(it.gauth.data))
                    authenticatorCode.value = it.gauth.data.split("=").getOrNull(1)
                },
                onError = {
                    it.printStackTrace()
                    onError.postValue(ConsumableEvent(it))
                }
            )
        }
    }

    fun getPhoneNumberValue() = "${country.value ?: ""}${phoneNumber.value ?: ""}"
    fun getEmailValue() = email.value ?: ""

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
            method: TwoFactorMethod,
            action: TwoFactorSetupAction
        ): TwoFactorSetupViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            method: TwoFactorMethod,
            action: TwoFactorSetupAction
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, method, action) as T
            }
        }
    }

}