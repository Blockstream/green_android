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
import com.blockstream.common.AddressInputType
import com.blockstream.common.SupportType
import com.blockstream.common.data.*
import com.blockstream.common.devices.DeviceModel
import com.blockstream.common.gdk.data.*
import com.blockstream.common.looks.transaction.TransactionConfirmLook
import com.blockstream.common.models.jade.JadeQrOperation
import com.blockstream.common.models.settings.WalletSettingsSection
import com.blockstream.common.models.sheets.NoteType
import com.blockstream.common.navigation.PopTo
import com.blockstream.green.data.meld.data.QuotesResponse
import com.blockstream.ui.navigation.LocalNavBackStackEntry
import com.blockstream.ui.navigation.bottomsheet.bottomSheet
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
    typeOf<QuotesResponse>() to CustomNavType.create<QuotesResponse>(),
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
