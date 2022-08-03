package com.blockstream.green.ui.devices

import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.DeviceBrand
import com.blockstream.green.data.Countly
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.ConsumableEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.kotlin.addTo

class DeviceListViewModel @AssistedInject constructor(
    countly: Countly,
    val deviceManager: DeviceManager,
    @Assisted deviceBrand: DeviceBrand?
) : AppViewModel(countly) {

    val devices = MutableLiveData(listOf<Device>())
    val bleAdapterState = deviceManager.bleAdapterState
    val hasBleConnectivity = deviceBrand == null || deviceBrand.hasBleConnectivity

    var onSuccess: (() -> Unit)? = null

    val canEnableBluetooth = MutableLiveData(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)

    init {
        deviceManager
            .getDevices()
            .map { devices ->
                deviceBrand?.let { devices.filter { it.deviceBrand == deviceBrand } } ?: devices
            }
            .subscribe(devices::postValue)
            .addTo(disposables)
    }

    fun askForPermissionOrBond(device: Device) {
        onSuccess = {
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(device)))
        }

        device.askForPermissionOrBond(onSuccess!!) {
            onError.postValue(ConsumableEvent(it))
        }
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            deviceBrand: DeviceBrand?
        ): DeviceListViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            deviceBrand: DeviceBrand?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(deviceBrand) as T
            }
        }
    }
}