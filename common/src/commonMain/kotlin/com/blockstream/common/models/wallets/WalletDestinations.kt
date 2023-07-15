package com.blockstream.common.models.wallets

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.navigation.NavigateDestination

sealed class WalletDestinations(val wallet: GreenWallet): NavigateDestination {
    class WalletLogin(wallet: GreenWallet, val isLightningShortcut: Boolean = false) :
        WalletDestinations(wallet = wallet)

    class WalletOverview(wallet: GreenWallet) :
        WalletDestinations(wallet = wallet)

    class DeviceScan(wallet: GreenWallet) :
        WalletDestinations(wallet = wallet)
}