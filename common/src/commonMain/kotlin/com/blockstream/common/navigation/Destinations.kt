package com.blockstream.common.navigation

import com.blockstream.common.AddressInputType
import com.blockstream.common.SupportType
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.EnrichedAssetList
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.LnUrlAuthRequestDataSerializable
import com.blockstream.common.data.LnUrlWithdrawRequestSerializable
import com.blockstream.common.data.MenuEntryList
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.data.SupportData
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.TwoFactorSetupAction
import com.blockstream.common.devices.DeviceModel
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountAssetBalanceList
import com.blockstream.common.gdk.data.AccountAssetList
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.common.gdk.data.AssetBalanceList
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.looks.transaction.TransactionConfirmLook
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.jade.JadeQrOperation
import com.blockstream.common.models.settings.WalletSettingsSection
import com.blockstream.common.models.sheets.NoteType
import com.blockstream.green.data.meld.data.QuotesResponse
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.Route
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
sealed class NavigateDestination(
    override val uniqueId: String = Uuid.random().toHexString(),
    override val unique: Boolean = false,
    override val makeItRoot: Boolean = false,
    override val isBottomNavigation: Boolean = false,
) : Event, Route

sealed class NavigateDestinations : NavigateDestination() {
    @Serializable
    data class WalletOverview constructor(
        val greenWallet: GreenWallet,
        val showWalletOnboarding: Boolean = false,
        val isBottomNav: Boolean = false
    ) : NavigateDestination(unique = true, makeItRoot = true, isBottomNavigation = isBottomNav)

    @Serializable
    data class Transact(val greenWallet: GreenWallet) : NavigateDestination(unique = true, isBottomNavigation = true)

    @Serializable
    data class Security(val greenWallet: GreenWallet) : NavigateDestination(unique = true, isBottomNavigation = true)

    @Serializable
    data object Home : NavigateDestination(unique = true, makeItRoot = true)

    @Serializable
    data object GetStarted : NavigateDestination(unique = true)

    @Serializable
    data object About : NavigateDestination(unique = true)

    @Serializable
    data object AppSettings : NavigateDestination(unique = true)

    @Serializable
    data class Analytics(val isActionRequired: Boolean = false) : NavigateDestination()

    @Serializable
    data object SetupNewWallet : NavigateDestination()

    @Serializable
    data object UseHardwareDevice : NavigateDestination(unique = true)


    @Serializable
    data class WatchOnlySinglesig(val setupArgs: SetupArgs) : NavigateDestination(unique = true)

    @Serializable
    data class WatchOnlyMultisig(val setupArgs: SetupArgs) : NavigateDestination(unique = true)

    @Serializable
    data class WatchOnlyCredentialsSettings(val greenWallet: GreenWallet, val network: Network) : NavigateDestination()

    @Serializable
    data class WalletSettings(
        val greenWallet: GreenWallet,
        val section: WalletSettingsSection = WalletSettingsSection.General,
        val network: Network? = null,
    ) : NavigateDestination(isBottomNavigation = section == WalletSettingsSection.General)

    @Serializable
    data class EnterRecoveryPhrase(val setupArgs: SetupArgs) : NavigateDestination()

    @Serializable
    data class RecoveryIntro(val setupArgs: SetupArgs) : NavigateDestination()

    @Serializable
    data class RecoveryWords(val setupArgs: SetupArgs) : NavigateDestination()

    @Serializable
    data class RecoveryCheck(val setupArgs: SetupArgs) : NavigateDestination()

    @Serializable
    data class RecoverySuccess(val greenWallet: GreenWallet) : NavigateDestination()

    @Serializable
    data class SetPin(val setupArgs: SetupArgs) : NavigateDestination()

    @Serializable
    data class RecoveryPhrase(val setupArgs: SetupArgs) : NavigateDestination()

    @Serializable
    data class AddAccount2of3(val setupArgs: SetupArgs) : NavigateDestination(unique = true)

    @Serializable
    data class ReviewAddAccount(val setupArgs: SetupArgs) : NavigateDestination()

    @Serializable
    data class Xpub(val setupArgs: SetupArgs) : NavigateDestination(unique = true)

    @Serializable
    data class DeviceList(val isJade: Boolean) : NavigateDestination()

    @Serializable
    data class DeviceInfo(val deviceId: String) : NavigateDestination()

    @Serializable
    data object NewJadeConnected : NavigateDestination()

    @Serializable
    data class JadeGenuineCheck(val greenWalletOrNull: GreenWallet? = null, val deviceId: String? = null) : NavigateDestination()

    @Serializable
    data class DeviceScan(
        val greenWallet: GreenWallet,
        val isWatchOnlyUpgrade: Boolean = false,
        val isWatchOnlyDeviceConnect: Boolean = false
    ) : NavigateDestination()

    @Serializable
    data class JadeFirmwareUpdate(val deviceId: String) : NavigateDestination()

    @Serializable
    data class Login(
        val greenWallet: GreenWallet,
        val autoLoginWallet: Boolean = true,
        val deviceId: String? = null,
        val isWatchOnlyUpgrade: Boolean = false
    ) : NavigateDestination(unique = true)

    @Serializable
    data class Support(
        val type: SupportType,
        val supportData: SupportData,
        val greenWalletOrNull: GreenWallet? = null
    ) : NavigateDestination()

    @Serializable
    data class RenameWallet(val greenWallet: GreenWallet) : NavigateDestination()

    @Serializable
    data class DeleteWallet(val greenWallet: GreenWallet) : NavigateDestination()

    @Serializable
    data class AssetsAccounts(
        val greenWallet: GreenWallet,
        val assetsAccounts: AccountAssetBalanceList
    ) : NavigateDestination()

    @Serializable
    data class Accounts(
        val greenWallet: GreenWallet,
        val accounts: AccountAssetBalanceList,
        val title: String? = null,
        val message: String? = null,
        val withAsset: Boolean = true,
        val withAssetIcon: Boolean = true,
        val withArrow: Boolean = false,
        val withAction: Boolean = true
    ) : NavigateDestination()

    @Serializable
    data class SecurityLevel(val greenWallet: GreenWallet) : NavigateDestination()

    @Serializable
    data class Assets constructor(val greenWallet: GreenWallet, val assets: AssetBalanceList) : NavigateDestination() {
        companion object {
            @NativeCoroutines
            suspend fun create(viewModel: GreenViewModel): Assets {
                return Assets(
                    greenWallet = viewModel.greenWallet,
                    assets = withContext(context = Dispatchers.IO) {
                        (listOfNotNull(
                            EnrichedAsset.createOrNull(
                                session = viewModel.session,
                                viewModel.session.bitcoin?.policyAsset
                            ),
                            EnrichedAsset.createOrNull(
                                session = viewModel.session,
                                viewModel.session.lightning?.policyAsset
                            ).takeIf { viewModel.session.hasLightning },
                            EnrichedAsset.createOrNull(
                                session = viewModel.session,
                                viewModel.session.liquid?.policyAsset
                            ),
                        ) + (viewModel.session.enrichedAssets.value.takeIf { viewModel.session.liquid != null }
                            ?.filter { !it.isAmp || viewModel.session.hasAmpAccount }?.map {
                                EnrichedAsset.create(session = viewModel.session, assetId = it.assetId)
                            } ?: listOf()) + listOfNotNull(
                            EnrichedAsset.createAnyAsset(session = viewModel.session, isAmp = false)
                                .takeIf { viewModel.session.hasAmpAccount },
                            EnrichedAsset.createAnyAsset(session = viewModel.session, isAmp = true)
                                .takeIf { !viewModel.session.isHwWatchOnly }
                        ).sortedWith(viewModel.session::sortEnrichedAssets)).let { list ->
                            list.map {
                                AssetBalance.create(it)
                            }
                        }
                    }.let { AssetBalanceList(it) })
            }
        }
    }

    @Serializable
    data class Bip39Passphrase(val greenWallet: GreenWallet, val passphrase: String) : NavigateDestination()

    @Serializable
    data class EnableTwoFactor(val greenWallet: GreenWallet, val network: Network) : NavigateDestination()

    @Serializable
    data class ArchivedAccounts(val greenWallet: GreenWallet, val navigateToRoot: Boolean = false) : NavigateDestination()

    @Serializable
    data class WalletAssets(val greenWallet: GreenWallet) : NavigateDestination(unique = true)

    @Serializable
    data object Environment : NavigateDestination()

    @Serializable
    data class AccountOverview(val greenWallet: GreenWallet, val accountAsset: AccountAsset) : NavigateDestination(unique = true)

    @Serializable
    data class ChooseAccountType(
        val greenWallet: GreenWallet,
        val assetBalance: AssetBalance? = null,
        val allowAssetSelection: Boolean = true,
        val popTo: PopTo? = null
    ) : NavigateDestination(unique = true)

    @Serializable
    data class WatchOnly(val greenWallet: GreenWallet) : NavigateDestination()

    @Serializable
    data class ChangePin(val greenWallet: GreenWallet, val isRecoveryConfirmation: Boolean = false) : NavigateDestination()

    @Serializable
    data class SystemMessage(val greenWallet: GreenWallet, val network: Network, val message: String) : NavigateDestination()

    @Serializable
    data class TwoFactorReset(
        val greenWallet: GreenWallet,
        val network: Network,
        val twoFactorReset: com.blockstream.common.gdk.data.TwoFactorReset? = null
    ) : NavigateDestination()

    @Serializable
    data class TwoFactorAuthentication(val greenWallet: GreenWallet, val network: Network? = null) : NavigateDestination()

    @Serializable
    data class RenameAccount(val greenWallet: GreenWallet, val account: Account) : NavigateDestination()

    @Serializable
    data class LightningNode(val greenWallet: GreenWallet) : NavigateDestination()

    @Serializable
    data class SwapFees(
        val serviceFee: String,
        val networkFee: String,
        val totalFees: String,
        val totalFeesFiat: String?
    ) : NavigateDestination()

    @Serializable
    data class TransactionDetails(val greenWallet: GreenWallet, val transaction: com.blockstream.common.gdk.data.Transaction) :
        NavigateDestination()

    @Serializable
    data class AssetAccountList(val greenWallet: GreenWallet, val assetId: String) :
        NavigateDestination()

    @Serializable
    data class AssetAccountDetails(val greenWallet: GreenWallet, val accountAsset: AccountAsset) :
        NavigateDestination()

    @Serializable
    data class AccountDescriptor(val greenWallet: GreenWallet, val accountAsset: AccountAsset) :
        NavigateDestination()

    @Serializable
    data class Camera(
        val isDecodeContinuous: Boolean = false,
        val parentScreenName: String? = null,
        val setupArgs: SetupArgs? = null
    ) : NavigateDestination()

    @Serializable
    data class AssetDetails(
        val greenWallet: GreenWallet,
        val assetId: String,
        val accountAsset: AccountAsset? = null
    ) : NavigateDestination()

    @Serializable
    data class TwoFactorSetup(
        val greenWallet: GreenWallet,
        val method: TwoFactorMethod,
        val action: TwoFactorSetupAction,
        val network: Network,
        val isSmsBackup: Boolean = false
    ) : NavigateDestination()

    @Serializable
    data class Receive constructor(val greenWallet: GreenWallet, val accountAsset: AccountAsset) : NavigateDestination()

    @Serializable
    data class ReceiveChooseAsset(
        val greenWallet: GreenWallet,
        val accountAsset: AccountAsset? = null
    ) : NavigateDestination()

    @Serializable
    data class ReceiveChooseAccount(
        val greenWallet: GreenWallet,
        val accounts: AccountAssetList
    ) : NavigateDestination()

    @Serializable
    data class SendChooseAsset(
        val greenWallet: GreenWallet,
        val address: String,
        val addressType: AddressInputType,
        val assets: EnrichedAssetList
    ) : NavigateDestination()

    @Serializable
    data class SendChooseAccount(
        val greenWallet: GreenWallet,
        val address: String,
        val addressType: AddressInputType,
        val asset: EnrichedAsset,
        val accounts: AccountAssetBalanceList
    ) : NavigateDestination()

    @Serializable
    data class Send(
        val greenWallet: GreenWallet,
        val address: String,
        val addressType: AddressInputType,
        val accountAsset: AccountAsset
    ) : NavigateDestination()

    @Serializable
    data class SendAddress(
        val greenWallet: GreenWallet,
        val address: String? = null,
        val addressType: AddressInputType? = null,
        val accountAsset: AccountAsset? = null
    ) : NavigateDestination()

    @Serializable
    data class Bump(
        val greenWallet: GreenWallet,
        val accountAsset: AccountAsset,
        val transaction: String
    ) : NavigateDestination()

    @Serializable
    data class SendConfirm(
        val greenWallet: GreenWallet,
        val accountAsset: AccountAsset,
        val denomination: com.blockstream.common.data.Denomination? = null
    ) : NavigateDestination()

    @Serializable
    data class RecoverFunds(
        val greenWallet: GreenWallet,
        val amount: Long = 0,
        val isSendAll: Boolean = false,
        val address: String? = null,
    ) : NavigateDestination()

    @Serializable
    data class Addresses(val greenWallet: GreenWallet, val accountAsset: AccountAsset) : NavigateDestination()

    @Serializable
    data class Sweep(
        val greenWallet: GreenWallet,
        val privateKey: String? = null,
        val accountAsset: AccountAsset? = null
    ) : NavigateDestination()

    @Serializable
    data class SignMessage(
        val greenWallet: GreenWallet,
        val accountAsset: AccountAsset,
        val address: String
    ) : NavigateDestination()

    @Serializable
    data class AccountExchange(val greenWallet: GreenWallet) : NavigateDestination()

    @Serializable
    data class OnOffRamps(val greenWallet: GreenWallet) : NavigateDestination()

    @Serializable
    data class Buy(val greenWallet: GreenWallet, val accountAsset: AccountAsset? = null) : NavigateDestination()

    @Serializable
    data class BuyQuotes(val greenWallet: GreenWallet, val quotes: QuotesResponse, val selectedServiceProvider: String?) :
        NavigateDestination()

    @Serializable
    data class LnUrlAuth(
        val greenWallet: GreenWallet,
        val lnUrlAuthRequest: LnUrlAuthRequestDataSerializable
    ) : NavigateDestination()

    @Serializable
    data class LnUrlWithdraw(
        val greenWallet: GreenWallet,
        val lnUrlWithdrawRequest: LnUrlWithdrawRequestSerializable
    ) : NavigateDestination()

    @Serializable
    data class Transaction(
        val greenWallet: GreenWallet,
        val transaction: com.blockstream.common.gdk.data.Transaction
    ) : NavigateDestination()

    @Serializable
    data class Redeposit(
        val greenWallet: GreenWallet,
        val accountAsset: AccountAsset,
        val isRedeposit2FA: Boolean
    ) : NavigateDestination()

    @Serializable
    data class JadeQR(
        val operation: JadeQrOperation,
        val greenWalletOrNull: GreenWallet? = null,
        val deviceModel: DeviceModel? = null,
    ) : NavigateDestination()

    @Serializable
    data class AskJadeUnlock(
        val isOnboarding: Boolean
    ) : NavigateDestination()

    @Serializable
    data object JadePinUnlock : NavigateDestination()

    @Serializable
    data class ImportPubKey(val deviceModel: DeviceModel) : NavigateDestination()

    @Serializable
    data class Qr(
        val greenWallet: GreenWallet,
        val title: String? = null,
        val subtitle: String? = null,
        val data: String
    ) : NavigateDestination()

    @Serializable
    data class ReEnable2FA(val greenWallet: GreenWallet) : NavigateDestination()

    @Serializable
    data class Countries(
        val greenWallet: GreenWallet,
        val title: String? = null,
        val subtitle: String? = null,
        val showDialCode: Boolean = true
    ) : NavigateDestination()

    @Serializable
    data class MeldCountries(
        val greenWallet: GreenWallet,
        val title: String? = null,
        val subtitle: String? = null
    ) : NavigateDestination()

    @Serializable
    data object JadeGuide : NavigateDestination()

    @Serializable
    data class ChooseAssetAccounts(val greenWallet: GreenWallet) : NavigateDestination()

    @Serializable
    data class Note(val greenWallet: GreenWallet, val note: String, val noteType: NoteType) : NavigateDestination()

    @Serializable
    data class Promo(val promo: com.blockstream.common.data.Promo, val greenWalletOrNull: GreenWallet? = null) : NavigateDestination()

    @Serializable
    data class DeviceInteraction(
        val greenWalletOrNull: GreenWallet? = null,
        val deviceId: String? = null,
        val transactionConfirmLook: TransactionConfirmLook? = null,
        val verifyAddress: String? = null,
        val isMasterBlindingKeyRequest: Boolean = false,
        val message: String? = null
    ) : NavigateDestination()

    @Serializable
    data class Denomination(val greenWallet: GreenWallet, val denominatedValue: DenominatedValue) : NavigateDestination()

    @Serializable
    data object RecoveryHelp : NavigateDestination()

    @Serializable
    data class Menu(
        val title: String,
        val subtitle: String? = null,
        val entries: MenuEntryList
    ) : NavigateDestination()

    @Serializable
    data class MainMenu(val isTestnet: Boolean) : NavigateDestination()

    @Serializable
    data class FeeRate(
        val greenWallet: GreenWallet,
        val accountAsset: AccountAsset? = null,
        val useBreezFees: Boolean
    ) : NavigateDestination()

    @Serializable
    data object DevicePin : NavigateDestination()

    @Serializable
    data object DevicePassphrase : NavigateDestination()

    @Serializable
    data class UrlWarning(val urls: List<String>) : NavigateDestination()

    @Serializable
    data object TorWarning : NavigateDestination()
}
