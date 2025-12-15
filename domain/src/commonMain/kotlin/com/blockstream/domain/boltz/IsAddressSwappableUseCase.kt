package com.blockstream.domain.boltz

import com.blockstream.data.extensions.tryCatch
import lwk.Bolt11Invoice

/**
 * Use case that validates whether a Lightning invoice can be used for a swap.
 *
 * Swaps currently require a BOLT11 invoice with an explicit amount. Amount-less
 * invoices are rejected because a precise amount is necessary to construct the swap.
 */
class IsAddressSwappableUseCase() {

    suspend operator fun invoke(
        address: String,
    ): Boolean {
        /**
         * Check if the provided address `address` is BOLT11 (invoice) and includes an amount.
         *
         * - Returns true if parsing succeeds and the invoice specifies `amountMilliSatoshis`.
         * - Returns false if parsing fails or the invoice has no amount.
         */
        // Don't allow amount-less invoices
        return tryCatch { Bolt11Invoice(address.replace("lightning:", "")).amountMilliSatoshis() != null } ?: false
    }
}
