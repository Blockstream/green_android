package com.blockstream.common.models.onboarding

import com.blockstream.common.events.Event
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects

abstract class SetupNewWalletViewModelAbstract() : GreenViewModel() {
    override fun screenName(): String = "SetupNewWallet"
}

class SetupNewWalletViewModel : SetupNewWalletViewModelAbstract() {

    class LocalEvents {
        object ClickOnThisDevice : Event
        object ClickOnHardwareWallet : Event
        object WatchOnly : Event
    }


    init {
        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ClickOnThisDevice -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.AddWallet))
                countly.addWallet()
            }

            is LocalEvents.ClickOnHardwareWallet -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.UseHardwareDevice))
                countly.hardwareWallet()
            }

            is LocalEvents.WatchOnly -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WatchOnlyPolicy))
                countly.watchOnlyWallet()
            }
        }
    }
}

class SetupNewWalletViewModelPreview() : SetupNewWalletViewModelAbstract() {

    companion object {
        fun preview() = SetupNewWalletViewModelPreview()
    }
}