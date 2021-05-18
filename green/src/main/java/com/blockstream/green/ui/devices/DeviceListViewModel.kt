package com.blockstream.green.ui.devices

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceBrand
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.ConsumableEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.kotlin.addTo

class DeviceListViewModel @AssistedInject constructor(
    val deviceManager: DeviceManager,
    @Assisted deviceBrand: DeviceBrand
) : AppViewModel() {

    val devices = MutableLiveData(listOf<Device>())
    val bleAdapterState = deviceManager.bleAdapterState
    val hasBleConnectivity = deviceBrand.hasBleConnectivity

    init {
        deviceManager
            .getDevices()
            .map { devices ->
                devices.filter { it.deviceBrand == deviceBrand }
            }
            .subscribe(devices::postValue)
            .addTo(disposables)
    }

    fun askForPermissionOrBond(device: Device){
        device.usbDevice?.let {
            deviceManager.askForPermissions(it){
                onEvent.postValue(ConsumableEvent(device))
            }
        }

        device.bleDevice?.let {
            deviceManager.bondDevice(device, {
                onEvent.postValue(ConsumableEvent(device))
            }, {
                onError.postValue(ConsumableEvent(it))
            })
        }
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            deviceBrand: DeviceBrand
        ): DeviceListViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            deviceBrand: DeviceBrand
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return assistedFactory.create(deviceBrand) as T
            }
        }
    }
}