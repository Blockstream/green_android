package com.blockstream.domain.account

import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.database.Database
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.managers.WalletSettingsManager

class RemoveAccountUseCase(
    private val database: Database,
    private val walletSettingsManager: WalletSettingsManager
) {

    suspend operator fun invoke(session: GdkSession, wallet: GreenWallet, account: Account) {
        if (account.isLightning) {

            walletSettingsManager.setLightningEnabled(walletId = wallet.id, enabled = false)

            database.deleteLoginCredentials(wallet.id, CredentialType.LIGHTNING_MNEMONIC)

            session.removeAccount(account)
        }
    }
}
