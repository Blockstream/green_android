package com.blockstream.domain.swap

import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAsset

/**
 * Validates if a transaction qualifies as a Liquid-to-Lightning swap.
 *
 * This check ensures that:
 * 1. The destination asset is Lightning.
 * 2. The source account is on the Liquid network.
 * 3. Swaps are enabled for the wallet.
 * 4. The destination address is a valid BOLT11 invoice with an amount.
 */
class IsLiquidToLightningSwapUseCase(
    private val isSwapsEnabledUseCase: IsSwapsEnabledUseCase,
    private val isInvoiceSwappableUseCase: IsInvoiceSwappableUseCase
) {

    /**
     * Returns true if the provided parameters constitute a valid Liquid-to-Lightning swap.
     *
     * @param wallet the active [GreenWallet]
     * @param asset the destination [EnrichedAsset]
     * @param address the destination address/invoice string
     * @param accountAsset the source [AccountAsset]
     * @param session the current [GdkSession]
     * @return true if all conditions for a Liquid-to-Lightning swap are met
     */
    suspend operator fun invoke(
        wallet: GreenWallet,
        asset: EnrichedAsset,
        address: String,
        accountAsset: AccountAsset,
        session: GdkSession,
    ): Boolean {
        return asset.isLightning && accountAsset.account.isLiquid && isSwapsEnabledUseCase(wallet = wallet) && isInvoiceSwappableUseCase(
            address = address,
            session = session
        )
    }
}
