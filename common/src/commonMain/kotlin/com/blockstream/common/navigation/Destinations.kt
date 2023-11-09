package com.blockstream.common.navigation

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.gdk.data.Asset
import com.blockstream.common.gdk.data.Network

interface NavigateDestination
sealed class NavigateDestinations : NavigateDestination {
    data class WalletOverview(val greenWallet: GreenWallet) : NavigateDestination
    data class NewWallet(val args: SetupArgs) : NavigateDestination
    object NewWatchOnlyWallet : NavigateDestination
    data class RestoreWallet(val args: SetupArgs) : NavigateDestination

    data class RecoveryWords(val args: SetupArgs) : NavigateDestination

    data class RecoveryCheck(val args: SetupArgs) : NavigateDestination

    data class SetPin(val args: SetupArgs) : NavigateDestination

    data class AddAccount(val args: SetupArgs) : NavigateDestination

    data class RecoveryPhrase(val args: SetupArgs) : NavigateDestination

    data class AddAccount2of3(val asset: Asset, val network: Network) : NavigateDestination

    object ExportLightningKey : NavigateDestination

    class NewRecovery(val setupArgs: SetupArgs) : NavigateDestination
    class ExistingRecovery(val setupArgs: SetupArgs) : NavigateDestination
    class Xpub(val setupArgs: SetupArgs) : NavigateDestination
}
