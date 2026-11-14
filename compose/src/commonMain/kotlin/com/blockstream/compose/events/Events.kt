package com.blockstream.compose.events

import com.blockstream.compose.navigation.NavigateDestination
import com.blockstream.compose.sideeffects.OpenBrowserType
import com.blockstream.compose.sideeffects.SideEffect
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.Urls
import com.blockstream.data.crypto.PlatformCipher
import com.blockstream.data.data.DenominatedValue
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.LogoutReason
import com.blockstream.data.data.Promo
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.Network

object Events {
    open class EventSideEffect(override val sideEffect: SideEffect) : EventWithSideEffect
    open class OpenBrowser(val url: String, val type: OpenBrowserType = OpenBrowserType.IN_APP) : EventWithSideEffect {
        override val sideEffect
            get() = SideEffects.OpenBrowser(url, type)
    }

    object OpenStatusPage : OpenBrowser(Urls.STATUS_PAGE, type = OpenBrowserType.OPEN_SYSTEM)

    open class NavigateTo(val destination: NavigateDestination) : EventWithSideEffect {
        override val sideEffect
            get() = SideEffects.NavigateTo(destination)
    }

    object NavigateBackUserAction : Event
    object NavigateBack : EventSideEffect(SideEffects.NavigateBack())
    class AckSystemMessage(val network: Network, val message: String) : Event
    object DismissSystemMessage : Event
    object DismissWalletBackupAlert : Event
    object ReconnectFailedNetworks : Event
    data class Transaction(val transaction: com.blockstream.data.gdk.data.Transaction) : Event
    data class HandleUserInput(val data: String, val isQr: Boolean = false) : Event
    object Continue : Event
    object SwapSetup : Event
    object SwapEnable : Event
    object SwapEntry : Event
    data class SwapInitiate(val from: Network, val to: Network) : Event
    data class SwapReceive(val from: Network, val to: Network) : Event
    data class SwapToggle(val from: Network, val to: Network) : Event
    data class PromoImpression(val promo: Promo) : Event
    data class PromoDismiss(val promo: Promo) : Event
    data class PromoAction(val promo: Promo) : Event
    object BannerDismiss : Event
    object BannerAction : Event
    object SelectDenomination : Event
    data class SetDenominatedValue(val denominatedValue: DenominatedValue) : Event
    data class RenameWallet(val wallet: GreenWallet, val name: String) : Event
    data class DeleteWallet(val wallet: GreenWallet) : Event
    data class Logout(val reason: LogoutReason) : Event
    data class DeviceRequestResponse(val data: String?) : Event
    data class RenameAccount(val account: Account, val name: String) : Event
    data class ArchiveAccount(val account: Account) : Event
    data class SetAccountAsset(val accountAsset: AccountAsset) : Event
    data class SetBarcodeScannerResult(val scannedText: String) : Event
    data class ProvideCipher(
        val platformCipher: PlatformCipher? = null,
        val exception: Exception? = null
    ) : Event

    data class SelectTwoFactorMethod(val method: String?) : Event
    data class ResolveTwoFactorCode(val code: String?) : Event
    object NotificationPermissionGiven : Event
    object BluetoothPermissionGiven : Event

    // Devices
    data class RespondToFirmwareUpgrade(val index: Int? = null) : Event
    data class SelectEnviroment(val isTestnet: Boolean, val customNetwork: Network?) : Event
}
