package com.blockstream.green.ui.receive

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.data.Address
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.createQrBitmap
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy

class ReceiveViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted wallet: Wallet,
) : AbstractWalletViewModel(sessionManager, walletRepository, countly, wallet){
    var address = MutableLiveData<Address>()
    var addressUri = MutableLiveData<String>()

    val requestAmount = MutableLiveData<String?>()
    var label = MutableLiveData<String?>()

    var addressQRBitmap = MutableLiveData<Bitmap?>()

    val isAddressUri = MutableLiveData(false)

    val deviceAddressValidationEvent = MutableLiveData<ConsumableEvent<Boolean?>>()

    // only show if we are on Liquid and we are using Ledger
    val showAssetWhitelistWarning = session.network.isLiquid && session.hwWallet?.device?.isLedger == true

    val canValidateAddressInDevice : Boolean by lazy {
        session.hwWallet?.device?.let { device ->
            device.isJade || (device.isLedger && session.isLiquid && !session.isSinglesig) || (device.isTrezor && !session.isLiquid && session.isSinglesig)
        } ?: false
    }

    init {
        generateAddress()
    }

    fun generateAddress() {

        session.observable {
            it.getReceiveAddress(session.activeAccount)
        }.doOnSubscribe {
            onProgress.postValue(true)
        }
        .doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onError = {
                onError.value = ConsumableEvent(it)
            },
            onSuccess = {
                address.value = it
                update()
            }
        ).addTo(disposables)
    }

    fun validateAddressInDevice() {
        address.value?.let { address ->
            deviceAddressValidationEvent.value = ConsumableEvent(null)

            session.hwWallet?.let { hwWallet ->
                session.observable(timeout = 30) {
                    val subAccount = session.getActiveSubAccount()
                    hwWallet.getGreenAddress(session.network, subAccount, address.userPath, address.subType ?: 0)
                }.subscribeBy(
                    onError = {
                        onError.value = ConsumableEvent(it)
                    },
                    onSuccess = {
                        if(it == address.address){
                            deviceAddressValidationEvent.value = ConsumableEvent(true)
                        }else{
                            deviceAddressValidationEvent.value = ConsumableEvent(false)
                        }
                    }
                ).addTo(disposables)
            }
        }
    }

    private fun update() {
        updateAddressUri()
        updateQR()
    }

    private fun updateAddressUri() {
        if (requestAmount.value != null || label.value != null) {
            isAddressUri.value = true

            // Use 2 different builders, we are restricted by spec
            // https://stackoverflow.com/questions/8534899/is-it-possible-to-use-uri-builder-and-not-have-the-part

            val scheme = Uri.Builder().also {
                if(session.isLiquid){
                    it.scheme("liquidnetwork")
                }else{
                    it.scheme("bitcoin")
                }

                it.opaquePart(address.value?.address)
            }.toString()

            val query = Uri.Builder().also {
                if (!requestAmount.value.isNullOrBlank()) {
                    it.appendQueryParameter("amount", requestAmount.value)
                }

                if (!label.value.isNullOrBlank()) {
                    it.appendQueryParameter("label", label.value)
                }

                if(session.isLiquid){
                    it.appendQueryParameter("assetid", session.network.policyAsset)
                }

            }.toString()

            addressUri.value = scheme + query
        } else {
            isAddressUri.value = false
            addressUri.value = address.value?.address
        }
    }

    private fun updateQR() {
        addressQRBitmap.postValue(createQrBitmap(addressUri.value ?: ""))
    }

    fun setRequestAmountAndLabel(amount: String?, lbl: String?) {
        requestAmount.value = amount
        label.value = lbl
        update()
    }

    fun clearRequestAmountAndLabel() {
        setRequestAmountAndLabel(null, null)
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet
        ): ReceiveViewModel
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
