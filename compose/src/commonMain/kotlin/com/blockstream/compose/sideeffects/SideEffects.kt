package com.blockstream.compose.sideeffects

import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.LogoutReason
import com.blockstream.data.data.PopTo
import com.blockstream.data.data.Redact
import com.blockstream.data.data.SupportData
import com.blockstream.data.data.TwoFactorResolverData
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.compose.events.Event
import com.blockstream.compose.navigation.NavigateDestination
import com.blockstream.compose.utils.StringHolder
import com.blockstream.jade.firmware.FirmwareUpgradeRequest
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CompletableDeferred
import okio.Path
import org.jetbrains.compose.resources.DrawableResource

object SideEffects {
    open class SideEffectEvent(override val event: Event) : SideEffectWithEvent
    data class OpenBrowser(val url: String, val type: OpenBrowserType = OpenBrowserType.IN_APP) : SideEffect
    data class OpenMenu(val id: Int = 0) : SideEffect
    data class OpenDialog(val id: Int = 0) : SideEffect
    data class Snackbar(val text: StringHolder) : SideEffect
    data class ErrorSnackbar(val error: Throwable, val supportData: SupportData? = null) :
        SideEffect

    data class Dialog(val title: StringHolder? = null, val message: StringHolder, val icon: DrawableResource? = null) : SideEffect
    data class ErrorDialog(val error: Throwable, val supportData: SupportData? = null) : SideEffect
    data class OpenFeeBottomSheet(
        val greenWallet: GreenWallet,
        val accountAsset: AccountAsset?,
        val params: CreateTransactionParams?,
        val useBreezFees: Boolean = false
    ) : SideEffect

    data class Success(val data: Any? = null) : SideEffect
    data class Mnemonic(val mnemonic: String) : SideEffect, Redact
    data class Navigate(val data: Any? = null) : SideEffect
    data class NavigateTo(val destination: NavigateDestination) : SideEffect
    data class NavigateBack constructor(
        val title: StringHolder? = null,
        val message: StringHolder? = null,
        val error: Throwable? = null,
        val supportData: SupportData? = null,
    ) : SideEffect

    data object NavigateAfterSendTransaction : SideEffect
    data class NavigateToRoot(val popTo: PopTo? = null) : SideEffect
    object CloseDrawer : SideEffect
    data class TransactionSent(val data: ProcessedTransactionDetails) : SideEffect
    data class Logout constructor(val reason: LogoutReason) : SideEffect
    data object WalletDelete : SideEffect
    data class CopyToClipboard(
        val value: String,
        val message: String? = null,
        val label: String? = null,
        val isSensitive: Boolean = false
    ) : SideEffect

    data class AccountArchived(val account: Account) : SideEffect
    data class AccountUnarchived(val account: Account) : SideEffect
    data class AccountCreated(val accountAsset: AccountAsset) : SideEffect
    data object AppReview : SideEffect
    data class RequestDeviceInteraction(
        val deviceId: String?,
        val message: String?,
        val isMasterBlindingKeyRequest: Boolean,
        val completable: CompletableDeferred<Boolean>?
    ) : SideEffect

    data object Dismiss : SideEffect
    data class Share(val text: String? = null) : SideEffect
    data class ShareFile(val path: Path? = null, val file: PlatformFile? = null) : SideEffect
    data class TwoFactorResolver(val data: TwoFactorResolverData) : SideEffect
    data object OpenDenominationExchangeRate : SideEffect
    data object EnableBluetooth : SideEffect
    data object EnableLocationService : SideEffect
    data object AskForBluetoothPermissions : SideEffect

    data object BleRequireRebonding : SideEffect
    data object RequestBiometricsCipher : SideEffect

    // Devices
    data class AskForFirmwareUpgrade(val request: FirmwareUpgradeRequest) : SideEffect
}