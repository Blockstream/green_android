package com.blockstream.compose.navigation

import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.database.Database
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.managers.SessionManager

class NavigateToWallet(
    private val sessionManager: SessionManager,
    private val database: Database,
) {
    suspend operator fun invoke(wallet: GreenWallet): NavigateDestination {
        val session: GdkSession = sessionManager.getWalletSessionOrCreate(wallet)

        return if (session.isConnected) {
            NavigateDestinations.WalletOverview(wallet)
        } else if (wallet.isHardware && !wallet.isWatchOnly && database.getLoginCredential(
                wallet.id, CredentialType.KEYSTORE_HW_WATCHONLY_CREDENTIALS
            ) == null
        ) {
            NavigateDestinations.DeviceScan(greenWallet = wallet)
        } else {
            NavigateDestinations.Login(greenWallet = wallet, autoLoginWallet = true)
        }
    }
}