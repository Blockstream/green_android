package com.blockstream.domain.swap

import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.jade.Loggable

/**
 * Retrieves a list of accounts that are eligible for swapping given a source account.
 *
 * Swap Compatibility Rules:
 * 1. **Asset Type**: Only the primary policy assets (L-BTC on Liquid and BTC on Bitcoin) are eligible.
 * 2. **Network Isolation**: Source and destination must reside on different networks (e.g., BTC to Liquid).
 * 3. **Lightning Routing**: Directly swapping between Bitcoin (on-chain) and Lightning is currently
 *    **NOT supported**. Users must bridge through a Liquid account (Bitcoin <-> Liquid <-> Lightning).
 */
class GetSwappableAccountsUseCase() {
    /**
     * Filters the user's accounts to find those compatible with the specified [swapFrom] account.
     *
     * @param session the current [GdkSession] providing access to all user accounts
     * @param swapFrom the source [AccountAssetBalance] to compare against, or null to return all
     *        accounts that are generally eligible for swapping.
     * @return a list of [AccountAssetBalance] representing accounts that can be used for the
     *         other side of the swap.
     */
    suspend operator fun invoke(
        session: GdkSession,
        swapFrom: AccountAssetBalance?
    ): List<AccountAssetBalance> {
        return session.accountAsset.value.filter {
            it.asset.isPolicyAsset(session)
        }.filter {
            // Except BTC <> LN
            when {
                swapFrom == null -> true
                swapFrom.account.network.isSameNetwork(it.account.network) -> false
                (swapFrom.account.isBitcoin && it.account.isLightning) -> false
                (swapFrom.account.isLightning && it.account.isBitcoin) -> false
                else -> true
            }
        }.map {
            AccountAssetBalance.create(accountAsset = it, session = session)
        }
    }

    companion object : Loggable()
}
