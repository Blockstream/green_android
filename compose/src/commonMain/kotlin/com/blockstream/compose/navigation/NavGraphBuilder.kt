package com.blockstream.compose.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import com.blockstream.compose.looks.transaction.TransactionConfirmLook
import com.blockstream.compose.models.jade.JadeQrOperation
import com.blockstream.compose.models.settings.WalletSettingsSection
import com.blockstream.compose.models.sheets.NoteType
import com.blockstream.compose.navigation.bottomsheet.bottomSheet
import com.blockstream.data.AddressInputType
import com.blockstream.data.SupportType
import com.blockstream.data.data.DenominatedValue
import com.blockstream.data.data.Denomination
import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.EnrichedAssetList
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.LnUrlAuthRequestDataSerializable
import com.blockstream.data.data.LnUrlWithdrawRequestSerializable
import com.blockstream.data.data.MenuEntryList
import com.blockstream.data.data.PopTo
import com.blockstream.data.data.Promo
import com.blockstream.data.data.SetupArgs
import com.blockstream.data.data.SupportData
import com.blockstream.data.data.TwoFactorMethod
import com.blockstream.data.data.TwoFactorSetupAction
import com.blockstream.data.devices.DeviceModel
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.AccountAssetBalanceList
import com.blockstream.data.gdk.data.AccountAssetList
import com.blockstream.data.gdk.data.AssetBalance
import com.blockstream.data.gdk.data.AssetBalanceList
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.data.Transaction
import com.blockstream.data.gdk.data.TwoFactorReset
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.reflect.typeOf

@OptIn(ExperimentalEncodingApi::class)
val AppTypeMap = mapOf(
    typeOf<GreenWallet>() to CustomNavType.create<GreenWallet>(),
    typeOf<GreenWallet?>() to CustomNavType.create<GreenWallet>(),
    typeOf<Account>() to CustomNavType.create<Account>(),
    typeOf<Transaction>() to CustomNavType.create<Transaction>(),
    typeOf<AccountAsset>() to CustomNavType.create<AccountAsset>(),
    typeOf<AccountAsset?>() to CustomNavType.create<AccountAsset>(),
    typeOf<SupportData>() to CustomNavType.create<SupportData>(),
    typeOf<SetupArgs>() to CustomNavType.create<SetupArgs>(),
    typeOf<SetupArgs?>() to CustomNavType.create<SetupArgs>(),
    typeOf<Network>() to CustomNavType.create<Network>(),
    typeOf<Network?>() to CustomNavType.create<Network>(),
    typeOf<AssetBalance>() to CustomNavType.create<AssetBalance>(),
    typeOf<AssetBalance?>() to CustomNavType.create<AssetBalance>(),
    typeOf<Denomination>() to CustomNavType.create<Denomination>(),
    typeOf<Denomination?>() to CustomNavType.create<Denomination>(),
    typeOf<DenominatedValue>() to CustomNavType.create<DenominatedValue>(),
    typeOf<Promo>() to CustomNavType.create<Promo>(),
    typeOf<TransactionConfirmLook?>() to CustomNavType.create<TransactionConfirmLook>(),
    typeOf<JadeQrOperation>() to CustomNavType.create<JadeQrOperation>(),
    typeOf<TwoFactorReset?>() to CustomNavType.create<TwoFactorReset>(),
    typeOf<LnUrlAuthRequestDataSerializable>() to CustomNavType.create<LnUrlAuthRequestDataSerializable>(),
    typeOf<LnUrlWithdrawRequestSerializable>() to CustomNavType.create<LnUrlWithdrawRequestSerializable>(),
    typeOf<PopTo?>() to CustomNavType.create<PopTo>(),
    typeOf<WalletSettingsSection>() to CustomNavType.create<WalletSettingsSection>(),
    typeOf<AddressInputType?>() to CustomNavType.create<AddressInputType>(),
    typeOf<DeviceModel?>() to CustomNavType.create<DeviceModel>(),
    typeOf<DeviceModel>() to CustomNavType.create<DeviceModel>(),
    typeOf<SupportType>() to CustomNavType.create<SupportType>(),
    typeOf<TwoFactorMethod>() to CustomNavType.create<TwoFactorMethod>(),
    typeOf<TwoFactorSetupAction>() to CustomNavType.create<TwoFactorSetupAction>(),
    typeOf<NoteType>() to CustomNavType.create<NoteType>(),
    typeOf<AccountAssetBalanceList>() to CustomNavType.create<AccountAssetBalanceList>(),
    typeOf<AssetBalanceList>() to CustomNavType.create<AssetBalanceList>(),
    typeOf<MenuEntryList>() to CustomNavType.create<MenuEntryList>(),
    typeOf<com.blockstream.data.meld.data.QuotesResponse>() to CustomNavType.create<com.blockstream.data.meld.data.QuotesResponse>(),
    typeOf<EnrichedAsset>() to CustomNavType.create<EnrichedAsset>(),
    typeOf<EnrichedAssetList>() to CustomNavType.create<EnrichedAssetList>(),
    typeOf<AccountAssetList>() to CustomNavType.create<AccountAssetList>()
)

inline fun <reified T : Any> NavGraphBuilder.appComposable(noinline content: @Composable (AnimatedContentScope.(NavBackStackEntry) -> Unit)) {
    composable<T>(
        typeMap = AppTypeMap,
    ) {
        // Inject LocalNavBackStackEntry to every Screen
        CompositionLocalProvider(
            LocalNavBackStackEntry provides it
        ) {
            content(this, it)
        }
    }
}

public inline fun <reified T : Any> NavGraphBuilder.appDialog(
    deepLinks: List<NavDeepLink> = emptyList(),
    dialogProperties: DialogProperties = DialogProperties(),
    noinline content: @Composable (NavBackStackEntry) -> Unit
) {
    dialog<T>(
        typeMap = AppTypeMap,
        deepLinks = deepLinks,
        dialogProperties = dialogProperties,
        content = content
    )
}

inline fun <reified T : Any> NavGraphBuilder.appBottomSheet(
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline content: @Composable (backstackEntry: NavBackStackEntry) -> Unit
) {
    bottomSheet<T>(
        deepLinks = deepLinks,
        typeMap = AppTypeMap,
        content = content
    )
}
