package com.blockstream.compose.models.onboarding.hardware

import com.blockstream.common.Urls
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects

abstract class UseHardwareDeviceViewModelAbstract() : GreenViewModel() {
    override fun screenName(): String = "UseHardwareDevice"

}

class UseHardwareDeviceViewModel : UseHardwareDeviceViewModelAbstract() {

    class LocalEvents {
        object ConnectJade : Event
        object ConnectDifferentHardwareDevice : Event
        object JadeStore : Events.OpenBrowser(Urls.JADE_STORE)
    }

    init {
        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ConnectJade -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.DeviceList(isJade = true)))
            }

            is LocalEvents.ConnectDifferentHardwareDevice -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.DeviceList(isJade = false)))
            }
        }
    }
}

class UseHardwareDeviceViewModelPreview() : UseHardwareDeviceViewModelAbstract() {

    companion object {
        fun preview() = UseHardwareDeviceViewModelPreview()
    }
}