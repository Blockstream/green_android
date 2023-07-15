package com.blockstream.common.navigation

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.SetupArgs

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
}
