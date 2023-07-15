package com.blockstream.green.ui.devices

import android.os.Build
import androidx.lifecycle.MutableLiveData
import com.blockstream.common.gdk.device.DeviceBrand
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.ConsumableEvent
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceConnectionManager
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.utils.QATester
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class DeviceListViewModel constructor(
    deviceManager: DeviceManager,
    qaTester: QATester,
    @InjectedParam val isJade: Boolean
) : AbstractDeviceViewModel(deviceManager, qaTester, null) {

    val devices = MutableLiveData(listOf<Device>())
    val hasBleConnectivity = true // deviceBrand == null || deviceBrand.hasBleConnectivity

    var onSuccess: (() -> Unit)? = null

    val canEnableBluetooth = MutableLiveData(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)

    override val device: Device? = null

    override val deviceConnectionManagerOrNull: DeviceConnectionManager? = null

    init {
        deviceManager
            .devicesStateFlow.map { devices ->
                devices.filter {
                    (it.deviceBrand == DeviceBrand.Blockstream && isJade) || (it.deviceBrand != DeviceBrand.Blockstream && !isJade)
                }
            }
            .onEach {
                devices.value = it
            }.launchIn(viewModelScope.coroutineScope)
    }

    fun askForPermissionOrBond(device: Device) {
        device.askForPermissionOrBond(onSuccess = {
            postSideEffect(SideEffects.Navigate(device))
        }, onError = {error ->
            error?.also { onError.postValue(ConsumableEvent(it)) }
        })
    }
}