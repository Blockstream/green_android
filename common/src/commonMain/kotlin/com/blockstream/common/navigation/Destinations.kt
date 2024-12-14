package com.blockstream.common.navigation

import com.blockstream.common.AddressInputType
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.LnUrlAuthRequestDataSerializable
import com.blockstream.common.data.LnUrlWithdrawRequestSerializable
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.TwoFactorSetupAction
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.events.Event
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.looks.transaction.TransactionConfirmLook
import com.blockstream.common.models.jade.JadeQrOperation
import com.blockstream.common.models.settings.WalletSettingsSection
import com.blockstream.common.models.sheets.NoteType

interface NavigateDestination: Event
sealed class NavigateDestinations : NavigateDestination {
    object About : NavigateDestination
    object AppSettings : NavigateDestination
    object SetupNewWallet : NavigateDestination
    object AddWallet : NavigateDestination
    object UseHardwareDevice : NavigateDestination

    object WatchOnlyPolicy : NavigateDestination
    data class WatchOnlyNetwork(val setupArgs: SetupArgs) : NavigateDestination
    data class WatchOnlyCredentials(val setupArgs: SetupArgs) : NavigateDestination
    data class WatchOnlyCredentialsSettings(val network: Network) : NavigateDestination
    data class WalletSettings(val section: WalletSettingsSection = WalletSettingsSection.General, val network: Network? = null) : NavigateDestination
    data class EnterRecoveryPhrase(val setupArgs: SetupArgs) : NavigateDestination
    data class RecoveryIntro(val setupArgs: SetupArgs) : NavigateDestination
    data class RecoveryWords(val setupArgs: SetupArgs) : NavigateDestination

    data class RecoveryCheck(val setupArgs: SetupArgs) : NavigateDestination

    data class SetPin(val setupArgs: SetupArgs) : NavigateDestination

    data class AddAccount(val setupArgs: SetupArgs) : NavigateDestination

    data class RecoveryPhrase(val setupArgs: SetupArgs) : NavigateDestination

    data class AddAccount2of3(val setupArgs: SetupArgs) : NavigateDestination

    data class ReviewAddAccount(val setupArgs: SetupArgs) : NavigateDestination

    data class Xpub(val setupArgs: SetupArgs) : NavigateDestination
    data class DeviceList(val isJade: Boolean) : NavigateDestination
    data class DeviceInfo(val deviceId: String) : NavigateDestination
    data object NewJadeConnected : NavigateDestination
    data class JadeGenuineCheck(val deviceId: String? = null) : NavigateDestination
    data class DeviceScan(val greenWallet: GreenWallet) : NavigateDestination
    data class JadeFirmwareUpdate(val deviceId: String) : NavigateDestination

    data class Login(val greenWallet: GreenWallet, val isLightningShortcut: Boolean = false, val deviceId: String? = null) : NavigateDestination

    data class WalletOverview(val greenWallet: GreenWallet) : NavigateDestination


    data class RenameWallet(val greenWallet: GreenWallet) : NavigateDestination
    data class DeleteWallet(val greenWallet: GreenWallet) : NavigateDestination
    data class AssetsAccounts(val assetsAccounts: List<AccountAssetBalance>) : NavigateDestination
    data class Accounts(val accounts: List<AccountAssetBalance>, val withAsset: Boolean) : NavigateDestination
    data class Assets(val assets: List<AssetBalance>? = null) : NavigateDestination
    data class Bip39Passphrase(val passphrase: String) : NavigateDestination
    data class EnableTwoFactor(val network: Network) : NavigateDestination
    data class ArchivedAccounts(val navigateToRoot: Boolean = false) : NavigateDestination
    object WalletAssets : NavigateDestination
    data class AccountOverview(val accountAsset: AccountAsset) : NavigateDestination
    data class ChooseAccountType(
        val assetBalance: AssetBalance? = null,
        val allowAssetSelection: Boolean = true,
        val popTo: PopTo? = null
    ) : NavigateDestination
    object WatchOnly : NavigateDestination
    object ChangePin : NavigateDestination
    data class SystemMessage(val network: Network, val message: String) : NavigateDestination
    data class TwoFactorReset(val network: Network) : NavigateDestination
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
        val accountAsset: AccountAsset? = null
    ) : NavigateDestination
    data class TwoFactorSetup(
        val method: TwoFactorMethod,
        val action: TwoFactorSetupAction,
        val network: Network,
        val isSmsBackup: Boolean = false
    ) : NavigateDestination

    data class Send(
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
        val amount: Long = 0,
        val isSendAll: Boolean = false,
        val address: String? = null,
    ) : NavigateDestination

    data class Receive(val accountAsset: AccountAsset) : NavigateDestination
    data class Addresses(val accountAsset: AccountAsset) : NavigateDestination
    data class Sweep(
        val privateKey: String? = null,
        val accountAsset: AccountAsset? = null
    ) : NavigateDestination

    data class SignMessage(
        val accountAsset: AccountAsset,
        val address: String
    ) : NavigateDestination

    object AccountExchange : NavigateDestination

    object OnOffRamps : NavigateDestination

    data class LnUrlAuth(
        val lnUrlAuthRequest: LnUrlAuthRequestDataSerializable
    ) : NavigateDestination

    data class LnUrlWithdraw(
        val lnUrlWithdrawRequest: LnUrlWithdrawRequestSerializable
    ) : NavigateDestination

    data class Transaction(
        val transaction: com.blockstream.common.gdk.data.Transaction
    ) : NavigateDestination

    data class Redeposit(
        val accountAsset: AccountAsset,
        val isRedeposit2FA: Boolean
    ) : NavigateDestination

    data class JadeQR constructor(
        val operation: JadeQrOperation,
        val deviceBrand: DeviceBrand? = null,
    ) : NavigateDestination

    data class AskJadeUnlock(
        val isOnboarding: Boolean
    ) : NavigateDestination


    object JadePinUnlock: NavigateDestination

    data class ImportPubKey(val deviceBrand: DeviceBrand): NavigateDestination

    data class Qr(
        val title: String? = null,
        val subtitle: String? = null,
        val data: String
    ) : NavigateDestination

    object ReEnable2FA: NavigateDestination

    object Countries: NavigateDestination
    object JadeGuide: NavigateDestination

    object ChooseAssetAccounts: NavigateDestination

    data class Note(val note: String, val noteType: NoteType) : NavigateDestination

    data class Promo(val promo: com.blockstream.common.data.Promo) : NavigateDestination

    data class DeviceInteraction(
        val transactionConfirmLook: TransactionConfirmLook? = null,
        val verifyAddress: String? = null,
        val isMasterBlindingKeyRequest: Boolean = false,
        val message: String? = null
    ) : NavigateDestination
}
