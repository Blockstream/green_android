package com.blockstream.green.ui.settings

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.gdk.data.Network
import com.blockstream.green.data.TwoFactorMethod
import com.blockstream.green.extensions.isEmailValid
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.createQrBitmap
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class TwoFactorSetupViewModel constructor(
    @InjectedParam wallet: GreenWallet,
    @InjectedParam val network: Network,
    @InjectedParam val method: TwoFactorMethod,
    @InjectedParam val action: TwoFactorSetupAction
) : WalletSettingsViewModel(wallet) {

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
}