package com.blockstream.compose.events

import com.blockstream.common.crypto.PlatformCipher
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.data.PopTo
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.Network
import com.blockstream.compose.navigation.NavigateDestination
import com.blockstream.compose.sideeffects.SideEffect
import com.blockstream.compose.sideeffects.SideEffects

object Events {
    open class EventSideEffect(override val sideEffect: SideEffect) : EventWithSideEffect
    open class OpenBrowser(val url: String) : EventWithSideEffect {
        override val sideEffect
            get() = SideEffects.OpenBrowser(url)
    }

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
    data class Transaction(val transaction: com.blockstream.common.gdk.data.Transaction) : Event
    data class ChooseAccountType(val isFirstAccount: Boolean = false, val popTo: PopTo? = null) : Event
    data class HandleUserInput(val data: String, val isQr: Boolean = false) : Event
    object Continue : Event
    object PromoImpression : Event
    object PromoDismiss : Event
    object PromoOpen : Event
    object PromoAction : Event
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
    data class RemoveAccount(val account: Account) : Event
    data class SetAccountAsset(val accountAsset: AccountAsset, val setAsActive: Boolean = false) : Event
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