package com.blockstream.domain.hardware

import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.WalletExtras
import com.blockstream.data.database.Database
import com.blockstream.utils.Loggable

/**
 * Turns hardware watch-only off for a wallet: removes the keystore credential that logs the
 * wallet in without its device, and records the choice on the wallet so the next device login
 * does not enable it again.
 */
class DisableHardwareWatchOnlyUseCase(
    private val database: Database
) : Loggable() {

    suspend operator fun invoke(greenWallet: GreenWallet) {
        check(!greenWallet.isEphemeral) { "Ephemeral wallets are not supported" }

        logger.d { "Removing HW Watch-only credentials" }

        database.deleteLoginCredentials(
            walletId = greenWallet.id,
            type = CredentialType.KEYSTORE_HW_WATCHONLY_CREDENTIALS
        )

        greenWallet.extras = (greenWallet.extras ?: WalletExtras()).copy(hwWatchOnlyEnabled = false)

        database.updateWallet(greenWallet)
    }
}
