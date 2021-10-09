package com.blockstream.green.ui

import androidx.arch.core.util.Function
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.blockstream.DeviceBrand
import com.blockstream.gdk.data.Device
import com.blockstream.green.utils.ConsumableEvent
import com.greenaddress.greenapi.HWWallet
import com.greenaddress.greenapi.HWWalletBridge
import io.reactivex.rxjava3.core.Single

import io.reactivex.rxjava3.disposables.CompositeDisposable
import androidx.lifecycle.LifecycleRegistry
import com.blockstream.green.data.AppEvent


open class AppViewModel : ViewModel(), HWWalletBridge, LifecycleOwner {

//    enum class Event {
//        NAVIGATE, RENAME_WALLET, DELETE_WALLET, RENAME_ACCOUNT, ACK_MESSAGE
//    }

    internal val disposables = CompositeDisposable()
    private var lifecycleRegistry: LifecycleRegistry? = null

    val onEvent = MutableLiveData<ConsumableEvent<AppEvent>>()
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
        lifecycleRegistry?.currentState = Lifecycle.State.DESTROYED
    }

    override fun getLifecycle(): Lifecycle {
        if(lifecycleRegistry == null) {
            lifecycleRegistry = LifecycleRegistry(this)
            lifecycleRegistry?.currentState = Lifecycle.State.STARTED
        }

        return lifecycleRegistry!!
    }

    val viewLifecycleOwner: LifecycleOwner
        get() = this
}