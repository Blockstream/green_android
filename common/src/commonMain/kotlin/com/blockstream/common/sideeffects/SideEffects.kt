package com.blockstream.common.sideeffects

import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.data.Redact
import com.blockstream.common.events.Event
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.Device
import com.blockstream.common.navigation.NavigateDestination
import kotlinx.coroutines.CompletableDeferred


class SideEffects : SideEffect {
    open class SideEffectEvent(override val event: Event) : SideEffectWithEvent
    data class OpenBrowser(val url: String) : SideEffect
    data class OpenMenu(val id: Int = 0) : SideEffect
    data class OpenDialog(val id: Int = 0) : SideEffect
    data class Snackbar(val text: String) : SideEffect
    data class ErrorSnackbar(val error: Throwable, val errorReport: ErrorReport? = null) :
        SideEffect
    data class Dialog(val title: String? = null, val message: String) : SideEffect
    data class ErrorDialog(val error: Throwable, val errorReport: ErrorReport? = null) : SideEffect
    data class OpenDenominationDialog(val denominatedValue: DenominatedValue): SideEffect
    data class Success(val data: Any? = null) : SideEffect
    data class Mnemonic(val mnemonic: String) : SideEffect, Redact
    data class Navigate(val data: Any? = null) : SideEffect
    data class NavigateTo(val destination: NavigateDestination) : SideEffect
    data class NavigateBack(val error: Throwable? = null, val errorReport: ErrorReport? = null, val title: String? = null) :
        SideEffect
    object NavigateToRoot : SideEffect
    data class Logout(val reason: LogoutReason) : SideEffect
    object WalletDelete : SideEffect
    data class CopyToClipboard(val value: String, val message: String?, val label: String? = null) : SideEffect
    data class AccountArchived(val account: Account) : SideEffect
    data class AccountUnarchived(val account: Account) : SideEffect
    data class UrlWarning(val urls: List<String>): SideEffect
    object DeviceRequestPassphrase: SideEffect
    object DeviceRequestPin: SideEffect
    class DeviceInteraction(val device: Device, val message: String?, val completable: CompletableDeferred<Boolean>?):
        SideEffect
    object Dismiss : SideEffect
    data class Share(val text: String? = null) : SideEffect
}