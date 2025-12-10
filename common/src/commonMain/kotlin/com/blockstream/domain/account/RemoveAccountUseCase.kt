package com.blockstream.domain.account

import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.database.Database
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.managers.WalletSettingsManager

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
