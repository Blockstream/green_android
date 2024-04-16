package com.blockstream.common.navigation

import com.blockstream.common.AddressInputType
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.LnUrlAuthRequestDataSerializable
import com.blockstream.common.data.LnUrlWithdrawRequestSerializable
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.TwoFactorSetupAction
import com.blockstream.common.events.Event
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.gdk.data.Network

interface NavigateDestination: Event
sealed class NavigateDestinations : NavigateDestination {
    object About : NavigateDestination
    object AppSettings : NavigateDestination
    object SetupNewWallet : NavigateDestination
    object AddWallet : NavigateDestination
    object UseHardwareDevice : NavigateDestination

    object NewWatchOnlyWallet : NavigateDestination
    object WalletSettings : NavigateDestination
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
    data class Accounts(val greenWallet: GreenWallet, val accounts: List<AccountAssetBalance>, val withAsset: Boolean) : NavigateDestination
    data class Bip39Passphrase(val passphrase: String) : NavigateDestination
    object EnableTwoFactor : NavigateDestination
    object ArchivedAccounts : NavigateDestination
    object Assets : NavigateDestination
    data class AccountOverview(val accountAsset: AccountAsset) : NavigateDestination
    data class ChooseAccountType(val greenWallet: GreenWallet) : NavigateDestination
    data class WatchOnly(val greenWallet: GreenWallet) : NavigateDestination
    data class ChangePin(val greenWallet: GreenWallet) : NavigateDestination
    data class SystemMessage(val network: Network, val message: String) : NavigateDestination
    data class TwoFactorReset(val network: Network, val twoFactorReset: com.blockstream.common.gdk.data.TwoFactorReset) : NavigateDestination
    data class TwoFactorAuthentication(val network: Network? = null) : NavigateDestination
    data class RenameAccount(val account: Account) : NavigateDestination
    object LightningNode : NavigateDestination
    data class TransactionDetails(val transaction: com.blockstream.common.gdk.data.Transaction) : NavigateDestination
    data class Camera(
        val isDecodeContinuous: Boolean = false,
        val parentScreenName: String? = null,
        val setupArgs: SetupArgs? = null
    ) : NavigateDestination
    data class AssetDetails(
        val assetId: String,
        val accountAsset: AccountAsset?
    ) : NavigateDestination
    data class TwoFactorSetup(
        val method: TwoFactorMethod,
        val action: TwoFactorSetupAction,
        val network: Network,
        val isSmsBackup: Boolean = false
    ) : NavigateDestination

    data class Send(
        val accountAsset: AccountAsset,
        val address: String? = null,
        val addressType: AddressInputType? = null
    ) : NavigateDestination

    data class Bump(
        val accountAsset: AccountAsset,
        val transaction: String
    ) : NavigateDestination

    data class SendConfirm(
        val accountAsset: AccountAsset,
        val denomination: Denomination?
    ) : NavigateDestination

    data class RecoverFunds(
        val satoshi: Long = 0,
        val isSendAll: Boolean = false,
        val address: String? = null,
    ) : NavigateDestination

    data class Receive(val accountAsset: AccountAsset) : NavigateDestination
    data class Sweep(
        val privateKey: String? = null,
        val accountAsset: AccountAsset? = null
    ) : NavigateDestination

    object AccountExchange : NavigateDestination

    data class LnUrlAuth(
        val lnUrlAuthRequest: LnUrlAuthRequestDataSerializable
    ) : NavigateDestination

    data class LnUrlWithdraw(
        val lnUrlWithdrawRequest: LnUrlWithdrawRequestSerializable
    ) : NavigateDestination

    data class Transaction(
        val transaction: com.blockstream.common.gdk.data.Transaction
    ) : NavigateDestination
}
