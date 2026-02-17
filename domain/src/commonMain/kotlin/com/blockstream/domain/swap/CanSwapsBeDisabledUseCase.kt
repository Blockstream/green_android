package com.blockstream.domain.swap

import com.blockstream.data.data.GreenWallet
import com.blockstream.data.database.Database

/**
 * Checks whether swap functionality can be safely disabled for a given wallet.
 * Returns `true` only when no pending (in-flight) Boltz swaps exist, preventing
 * the user from disabling swaps while transactions are still being processed.
 */
class CanSwapsBeDisabledUseCase(private val database: Database) {
    suspend operator fun invoke(wallet: GreenWallet): Boolean {
        return !database.hasPendingSwaps(xPubHashId = wallet.xPubHashId)
    }
}
