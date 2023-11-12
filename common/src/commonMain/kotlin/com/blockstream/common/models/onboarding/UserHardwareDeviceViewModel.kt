package com.blockstream.common.models.onboarding

import com.blockstream.common.Urls
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects

abstract class UseHardwareDeviceViewModelAbstract() : GreenViewModel() {
    override fun screenName(): String = "UseHardwareDevice"

}

class UseHardwareDeviceViewModel : UseHardwareDeviceViewModelAbstract() {

    class LocalEvents {
        object ConnectJade: Event
        object ConnectDifferentHardwareDevice: Event
        object JadeStore : Events.OpenBrowser(Urls.PRIVACY_POLICY)
    }

    init {
        bootstrap()
    }

    override fun handleEvent(event: Event) {
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