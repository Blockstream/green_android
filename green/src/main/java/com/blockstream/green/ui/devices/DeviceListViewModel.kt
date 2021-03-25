package com.blockstream.green.ui.devices

import androidx.lifecycle.MutableLiveData
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.kotlin.addTo
import javax.inject.Inject

@HiltViewModel
class DeviceListViewModel @Inject constructor(val deviceManager: DeviceManager) : AppViewModel() {
    val devices = MutableLiveData(listOf<Device>())

    init {
        deviceManager
            .getDevices()
            .subscribe(devices::postValue)
            .addTo(disposables)
    }

    fun askForPermissionOrBond(device: Device){
        device.usbDevice?.let {
            deviceManager.askForPermissions(it){
                onEvent.postValue(ConsumableEvent(device))
            }
        }
    }
}