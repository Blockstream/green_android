package com.blockstream.green.ui.devices

import androidx.lifecycle.MutableLiveData
import com.blockstream.common.gdk.device.DeviceInterface
import com.blockstream.common.managers.DeviceManager
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.ConsumableEvent
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceConnectionManager
import com.blockstream.green.utils.QATester
import com.blockstream.green.utils.isDevelopmentOrDebug
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

    val devices = MutableLiveData(listOf<DeviceInterface>())
    val hasBleConnectivity = true // deviceBrand == null || deviceBrand.hasBleConnectivity

    var onSuccess: (() -> Unit)? = null

    override val device: Device? = null

    override val deviceConnectionManagerOrNull: DeviceConnectionManager? = null

    val isDevelopment = isDevelopmentOrDebug

    init {
        deviceManager.devices.map { devices ->
            devices.filter { it.isJade == isJade }
        }.onEach {
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