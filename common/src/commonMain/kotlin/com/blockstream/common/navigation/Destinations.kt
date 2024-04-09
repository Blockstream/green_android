package com.blockstream.common.navigation

import com.blockstream.common.TransactionSegmentation
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.TwoFactorSetupAction
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.gdk.data.Network

interface NavigateDestination
sealed class NavigateDestinations : NavigateDestination {
    object About : NavigateDestination
    object AppSettings : NavigateDestination
    object SetupNewWallet : NavigateDestination
    object AddWallet : NavigateDestination
    object UseHardwareDevice : NavigateDestination

    object NewWatchOnlyWallet : NavigateDestination
    data class EnterRecoveryPhrase(val args: SetupArgs) : NavigateDestination
    data class RecoveryIntro(val args: SetupArgs) : NavigateDestination
    data class RecoveryWords(val args: SetupArgs) : NavigateDestination

    data class RecoveryCheck(val args: SetupArgs) : NavigateDestination

    data class SetPin(val args: SetupArgs) : NavigateDestination

    data class AddAccount(val args: SetupArgs) : NavigateDestination

    data class RecoveryPhrase(val args: SetupArgs) : NavigateDestination

    data class AddAccount2of3(val setupArgs: SetupArgs) : NavigateDestination

    data class ReviewAddAccount(val setupArgs: SetupArgs) : NavigateDestination

    object ExportLightningKey : NavigateDestination

    data class NewRecovery(val setupArgs: SetupArgs) : NavigateDestination
    data class ExistingRecovery(val setupArgs: SetupArgs) : NavigateDestination
    data class Xpub(val setupArgs: SetupArgs) : NavigateDestination
    data class DeviceList(val isJade: Boolean) : NavigateDestination

    data class WalletLogin(val greenWallet: GreenWallet, val isLightningShortcut: Boolean = false) : NavigateDestination

    data class WalletOverview(val greenWallet: GreenWallet) : NavigateDestination

    data class DeviceScan(val greenWallet: GreenWallet) : NavigateDestination

    data class RenameWallet(val greenWallet: GreenWallet) : NavigateDestination

    data class DeleteWallet(val greenWallet: GreenWallet) : NavigateDestination
    data class AssetsAccounts(val greenWallet: GreenWallet, val assetsAccounts: List<AccountAssetBalance>) : NavigateDestination
    data class Bip39Passphrase(val greenWallet: GreenWallet, val passphrase: String) : NavigateDestination
    data class WalletSettings(val greenWallet: GreenWallet) : NavigateDestination
    data class ArchivedAccounts(val greenWallet: GreenWallet) : NavigateDestination
    data class WatchOnly(val greenWallet: GreenWallet) : NavigateDestination
    data class ChangePin(val greenWallet: GreenWallet) : NavigateDestination
    data class TwoFactorAuthentication(val greenWallet: GreenWallet) : NavigateDestination
    data class TwoFactorSetup(
        val greenWallet: GreenWallet,
        val method: TwoFactorMethod,
        val action: TwoFactorSetupAction,
        val network: Network
    ) : NavigateDestination

    data class Send(
        val greenWallet: GreenWallet,
        val accountAsset: AccountAsset,
        val address: String? = null,
    ) : NavigateDestination

    data class Bump(
        val greenWallet: GreenWallet,
        val accountAsset: AccountAsset,
        val transaction: String
    ) : NavigateDestination

    data class SendConfirm(
        val greenWallet: GreenWallet,
        val accountAsset: AccountAsset,
        val transactionSegmentation: TransactionSegmentation
    ) : NavigateDestination

    data class RecoverFunds(
        val greenWallet: GreenWallet,
        val satoshi: Long,
        val isSendAll: Boolean = false,
        val address: String? = null,
    ) : NavigateDestination

    data class Receive(val greenWallet: GreenWallet, val accountAsset: AccountAsset) : NavigateDestination
    data class Sweep(
        val greenWallet: GreenWallet,
        val privateKey: String? = null,
        val accountAsset: AccountAsset? = null
    ) : NavigateDestination
}
