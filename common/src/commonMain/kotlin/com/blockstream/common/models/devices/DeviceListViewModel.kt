package com.blockstream.common.models.devices

import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.managers.DeviceManager
import com.blockstream.common.models.GreenViewModel
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

abstract class DeviceListViewModelAbstract() : GreenViewModel() {
    override fun screenName(): String = "RecoveryIntro"

    @NativeCoroutinesState
    abstract val devices: StateFlow<List<GreenDevice>>

}

class DeviceListViewModel constructor(deviceManager: DeviceManager, val showJade: Boolean = true) :
    DeviceListViewModelAbstract() {

    override val devices: StateFlow<List<GreenDevice>> = deviceManager.devices.map { devices ->
        devices.filter { it.deviceBrand.isJade == showJade }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())
}


class DeviceListViewModelPreview() : DeviceListViewModelAbstract() {
    override val devices: MutableStateFlow<List<GreenDevice>> = MutableStateFlow(listOf())

    companion object {
        fun preview() = DeviceListViewModelPreview()
    }
}