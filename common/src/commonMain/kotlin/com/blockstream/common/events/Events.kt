package com.blockstream.common.events

import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.navigation.NavigateDestination
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects

class Events : Event {
    open class EventSideEffect(override val sideEffect: SideEffect) : EventWithSideEffect
    open class OpenBrowser(val url: String) : EventWithSideEffect {
        override val sideEffect
            get() = SideEffects.OpenBrowser(url)
    }
    open class NavigateTo(val destination : NavigateDestination) : EventWithSideEffect {
        override val sideEffect
            get() = SideEffects.NavigateTo(destination)
    }
    object SetupNewWallet : NavigateTo(NavigateDestinations.SetupNewWallet)
    object About : NavigateTo(NavigateDestinations.About)
    object AppSettings : NavigateTo(NavigateDestinations.AppSettings)
    object Continue : Event
    object BannerDismiss : Event
    object BannerAction : Event
    object SelectDenomination : Event
    data class SetDenomination(val denominatedValue: DenominatedValue) : Event
    data class RenameWallet(val wallet: GreenWallet, val name: String) : Event
    data class DeleteWallet(val wallet: GreenWallet) : Event
    data class Logout(val reason: LogoutReason) : Event
    class DeviceRequestResponse(val data: String?): Event
    class ArchiveAccount(val account: Account): Event
    class UnArchiveAccount(val account: Account): Event

}