package com.blockstream.compose.navigation

import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.database.Database
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.managers.SessionManager

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