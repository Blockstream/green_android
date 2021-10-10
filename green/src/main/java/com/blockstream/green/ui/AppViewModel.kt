package com.blockstream.green.ui

import androidx.lifecycle.*
import com.blockstream.DeviceBrand
import com.blockstream.gdk.data.Device
import com.blockstream.green.data.AppEvent
import com.blockstream.green.ui.devices.DeviceInfoViewModel
import com.blockstream.green.utils.ConsumableEvent
import com.greenaddress.greenapi.HWWallet
import com.greenaddress.greenapi.HWWalletBridge
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import io.reactivex.rxjava3.disposables.CompositeDisposable


open class AppViewModel : ViewModel(), HWWalletBridge, LifecycleOwner {

    internal val disposables = CompositeDisposable()
    private var lifecycleRegistry: LifecycleRegistry? = null

    val onEvent = MutableLiveData<ConsumableEvent<AppEvent>>()
    val onProgress = MutableLiveData(false)
    val onError = MutableLiveData<ConsumableEvent<Throwable>>()

    val onDeviceInteractionEvent = MutableLiveData<ConsumableEvent<Device>>()

    var requestPinMatrixEmitter: SingleEmitter<String>? = null
    var requestPinPassphraseEmitter: SingleEmitter<String>? = null

    override fun interactionRequest(hw: HWWallet?) {
        hw?.let {
            onDeviceInteractionEvent.postValue(ConsumableEvent(it.device))
        }
    }

    override fun requestPinMatrix(deviceBrand: DeviceBrand?): Single<String> {
        onEvent.postValue(ConsumableEvent(AppFragment.DeviceRequestEvent.RequestPinMatrix))

        return Single.create<String> { emitter ->
            requestPinMatrixEmitter = emitter
        }.subscribeOn(AndroidSchedulers.mainThread())
    }

    override fun requestPassphrase(deviceBrand: DeviceBrand?): Single<String> {
        onEvent.postValue(ConsumableEvent(AppFragment.DeviceRequestEvent.RequestPassphrase))

        return Single.create<String> { emitter ->
            requestPinPassphraseEmitter = emitter
        }.subscribeOn(AndroidSchedulers.mainThread())
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