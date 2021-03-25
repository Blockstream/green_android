package com.blockstream.green.ui.devices

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.observable
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mu.KLogging

class DeviceInfoViewModel @AssistedInject constructor(
    val sessionManager: SessionManager,
    val deviceManager: DeviceManager,
    @Assisted val device: Device
) : AppViewModel() {

    val deviceInfo = MutableLiveData<String>()
    val isJade = MutableLiveData(true)

    val status = MutableLiveData("")

//    val deviceState = MutableLiveData(DeviceState.DISCONNECTED)

    enum class DeviceState {
        UNAUTHORIZED, CONNECTED, DISCONNECTED
    }

    init {

//        device.usbDevice?.let {
//            if (!deviceManager.hasPermissions(it)) {
//                deviceState.value = DeviceState.UNAUTHORIZED
//            }
//        }
//
//        device.onlineSubject.subscribeBy(
//            onNext = {
//                if (!it) {
//                    onEvent.postValue(ConsumableEvent(DeviceState.DISCONNECTED))
//                    deviceState.postValue(DeviceState.DISCONNECTED)
//                }
//            }
//        ).addTo(disposables)
    }

//    private fun getDeviceInfo(){
//        deviceManager.getDeviceConnection(device)?.let {
//            it.observable {
//                it.getDeviceInfo()!!
//            }.doOnSubscribe {
//                onProgress.postValue(true)
//            }.doOnTerminate {
//                onProgress.postValue(false)
//            }.subscribeBy(
//                onError = {
//                    onError.postValue(ConsumableEvent(it))
//                },
//                onSuccess = {
//                    deviceInfo.postValue(it.toString())
//                }
//            )
//        }
//    }

    fun connect() {
//        hardwareWallet.observable {
//            it.connect(sessionManager.getJadeSession())
//        }.doOnSubscribe {
//            onProgress.postValue(true)
//        }.doOnTerminate {
//            onProgress.postValue(false)
//        }.subscribe({
////            getDeviceInfo()
////            start()
////            onEvent.value = ConsumableEvent(device)
//        }, {
//            onError.value = ConsumableEvent(it)
//        })
    }

    fun start() {
        logger.info { "Starting" }

        sessionManager.getJadeSession().observable {
//            greenWallet.networks.bitcoinGreen
//            it.connect(Networks)
        }

    }


    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            device: Device
        ): DeviceInfoViewModel
    }

    companion object : KLogging() {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            device: Device
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return assistedFactory.create(device) as T
            }
        }
    }
}