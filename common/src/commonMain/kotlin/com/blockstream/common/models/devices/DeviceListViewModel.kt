package com.blockstream.common.models.devices

import com.blockstream.common.gdk.device.DeviceInterface
import com.blockstream.common.managers.DeviceManager
import com.blockstream.common.models.GreenViewModel
import com.rickclephas.kmm.viewmodel.stateIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

abstract class DeviceListViewModelAbstract() : GreenViewModel() {
    override fun screenName(): String = "RecoveryIntro"

    @NativeCoroutinesState
    abstract val devices: StateFlow<List<DeviceInterface>>

}

class DeviceListViewModel constructor(deviceManager: DeviceManager, val showJade: Boolean = true) :
    DeviceListViewModelAbstract() {

    override val devices: StateFlow<List<DeviceInterface>> = deviceManager.devices.map { devices ->
        devices.filter { it.isJade == showJade }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())
}


class DeviceListViewModelPreview() : DeviceListViewModelAbstract() {
    override val devices: MutableStateFlow<List<DeviceInterface>> = MutableStateFlow(listOf())

    companion object {
        fun preview() = DeviceListViewModelPreview()
    }
}