package com.blockstream.common.events

import com.blockstream.common.crypto.PlatformCipher
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.navigation.NavigateDestination
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
    object NavigateBack: EventSideEffect(SideEffects.NavigateBack())
    class AckSystemMessage(val network: Network, val message: String) : Event
    object DismissSystemMessage : Event
    object ReconnectFailedNetworks : Event
    data class Transaction(val transaction: com.blockstream.common.gdk.data.Transaction): Event
    data class ChooseAccountType(val isFirstAccount: Boolean = false, val isReceive: Boolean = false) : Event
    data class HandleUserInput(val data: String, val isQr: Boolean = false) : Event
    object Continue : Event
    object BannerDismiss : Event
    object BannerAction : Event
    object SelectDenomination : Event
    data class SetDenominatedValue(val denominatedValue: DenominatedValue) : Event
    data class RenameWallet(val wallet: GreenWallet, val name: String) : Event
    data class DeleteWallet(val wallet: GreenWallet) : Event
    data class Logout(val reason: LogoutReason) : Event
    class DeviceRequestResponse(val data: String?): Event
    class RenameAccount(val account: Account, val name: String): Event
    class ArchiveAccount constructor(val account: Account): Event
    class UnArchiveAccount(val account: Account, val navigateToRoot: Boolean): Event
    class RemoveAccount(val account: Account): Event
    class SetAccountAsset(val accountAsset: AccountAsset, val setAsActive: Boolean = false): Event
    class SetBarcodeScannerResult(val scannedText : String): Event
    class SubmitErrorReport(val email : String, val message: String, val errorReport: ErrorReport): Event
    class ProvideCipher(
        val platformCipher: PlatformCipher? = null,
        val exception: Exception? = null
    ) : Event

    data class SelectTwoFactorMethod(val method: String?): Event
    object NotificationPermissionGiven: Event
}