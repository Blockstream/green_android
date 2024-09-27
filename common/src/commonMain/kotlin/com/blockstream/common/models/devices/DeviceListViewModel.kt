package com.blockstream.common.models.devices

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_setup_guide
import com.blockstream.common.data.NavAction
import com.blockstream.common.data.NavData
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.getString

abstract class DeviceListViewModelAbstract(val isJade: Boolean) : AbstractDeviceViewModel() {
    override fun screenName(): String = "DeviceList"

    @NativeCoroutinesState
    abstract val devices: StateFlow<List<GreenDevice>>

}

class DeviceListViewModel(isJade: Boolean = true) :
    DeviceListViewModelAbstract(isJade = isJade) {
        
    class LocalEvents {
        data class SelectDevice(val device: GreenDevice): Event
        object ConnectViaQR: Events.NavigateTo(NavigateDestinations.JadeQR())
    }

    override val devices: StateFlow<List<GreenDevice>> = deviceManager.devices.map { devices ->
        devices.filter { it.deviceBrand.isJade == isJade }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    init {
        viewModelScope.launch {
            _navData.value =
                NavData(title = if (isJade) "Blockstream Jade" else "", actions = listOfNotNull(
                    NavAction(title = getString(Res.string.id_setup_guide), onClick = {
                        postEvent(NavigateDestinations.JadeGuide)
                    }).takeIf { isJade }
                ))
        }

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.SelectDevice) {
            val navigateTo =
                SideEffects.NavigateTo(NavigateDestinations.DeviceInfo(deviceId = event.device.connectionIdentifier))
            if (event.device.hasPermissions()) {
                postSideEffect(navigateTo)
            } else {
                askForPermissions(event.device, navigateTo)
            }
        }
    }
}


class DeviceListViewModelPreview : DeviceListViewModelAbstract(isJade = true) {
    override val devices: MutableStateFlow<List<GreenDevice>> = MutableStateFlow(listOf())

    companion object {

        fun preview() = DeviceListViewModelPreview()
    }
}