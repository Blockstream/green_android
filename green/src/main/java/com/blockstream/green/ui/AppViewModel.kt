package com.blockstream.green.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.blockstream.DeviceBrand
import com.blockstream.gdk.data.Device
import com.blockstream.green.utils.ConsumableEvent
import com.greenaddress.greenapi.HWWallet
import com.greenaddress.greenapi.HWWalletBridge
import io.reactivex.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable

open class AppViewModel : ViewModel(), HWWalletBridge {
    internal val disposables = CompositeDisposable()

    val onEvent = MutableLiveData<ConsumableEvent<Any>>()
    val onProgress = MutableLiveData(false)
    val onError = MutableLiveData<ConsumableEvent<Throwable>>()

    val onDeviceInteractionEvent = MutableLiveData<ConsumableEvent<Device>>()

    override fun interactionRequest(hw: HWWallet?) {
        hw?.let {
            onDeviceInteractionEvent.postValue(ConsumableEvent(it.device))
        }
    }

    // The following two methods are not needed
    // it will be remove in the next iteration on simplifying
    // hardware wallet interfaces
    override fun requestPinMatrix(deviceBrand: DeviceBrand?): Single<String> {
        TODO("Not yet implemented")
    }

    override fun requestPassphrase(deviceBrand: DeviceBrand?): Single<String> {
        TODO("Not yet implemented")
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}