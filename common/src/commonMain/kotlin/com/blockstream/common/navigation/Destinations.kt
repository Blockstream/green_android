package com.blockstream.common.navigation

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.SetupArgs

interface NavigateDestination
sealed class NavigateDestinations : NavigateDestination {
    object About : NavigateDestination
    object AppSettings : NavigateDestination
    object SetupNewWallet : NavigateDestination
    object AddWallet : NavigateDestination
    object UseHardwareDevice : NavigateDestination
    data class RecoveryIntro(val args: SetupArgs) : NavigateDestination
    object NewWatchOnlyWallet : NavigateDestination
    data class EnterRecoveryPhrase(val args: SetupArgs) : NavigateDestination

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
    data class Bip39Passphrase(val greenWallet: GreenWallet, val passphrase: String) : NavigateDestination

}
