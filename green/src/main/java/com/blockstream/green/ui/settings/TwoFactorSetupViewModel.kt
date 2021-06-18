package com.blockstream.green.ui.settings

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.GreenWallet
import com.blockstream.green.data.TwoFactorMethod
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.async
import com.blockstream.green.gdk.observable
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.createQrBitmap
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy

class TwoFactorSetupViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    appKeystore: AppKeystore,
    greenWallet: GreenWallet,
    @Assisted wallet: Wallet,
    @Assisted val method: TwoFactorMethod
) : WalletSettingsViewModel(sessionManager, walletRepository, appKeystore, greenWallet, wallet) {

    var authenticatorUrl: String? = null
    val country = MutableLiveData("")
    val phoneNumber = MutableLiveData("")
    val email = MutableLiveData("")
    var authenticatorCode = MutableLiveData("")
    var authenticatorQRBitmap = MutableLiveData<Bitmap?>()

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
            method: TwoFactorMethod
        ): TwoFactorSetupViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            method: TwoFactorMethod
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, method) as T
            }
        }
    }

}