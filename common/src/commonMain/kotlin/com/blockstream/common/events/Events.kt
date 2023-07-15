package com.blockstream.common.events

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.navigation.LogoutReason
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects

class Events : Event {
    open class EventSideEffect(override val sideEffect: SideEffect) : EventWithSideEffect
    open class OpenBrowser(val url: String) : EventWithSideEffect {
        override val sideEffect
            get() = SideEffects.OpenBrowser(url)
    }
    object Continue : Event
    object BannerDismiss : Event
    object BannerAction : Event
    data class RenameWallet(val wallet: GreenWallet, val name: String) : Event
    data class DeleteWallet(val wallet: GreenWallet) : Event
    data class Logout(val reason: LogoutReason) : Event
    class DeviceRequestResponse(val data: String?): Event
}