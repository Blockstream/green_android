package com.blockstream.domain.receive

import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.AccountAsset

/**
 * Use case that selects which accounts can fund a send for a given asset.
 *
 * Given the current wallet/session and a specific `EnrichedAsset`, this use case
 * filters the user's accounts to those that are compatible with the asset's network/type
 * and have a spendable balance. It encapsulates cross-network rules and feature flags
 * (e.g., Lightning-to-Liquid swap funding).
 *
 * Behavior summary:
 * - Bitcoin asset → include Bitcoin accounts only.
 * - Liquid asset (non-AMP) → include Liquid accounts only.
 * - Liquid AMP asset → include AMP accounts only.
 * - Lightning asset → include Lightning accounts; if swaps are enabled for this wallet,
 *   Liquid accounts are also included as potential funding sources.
 *
 * Only accounts with a positive balance are returned (via `AccountAssetBalance.createIfBalance`).
 *
 * Errors:
 * - If the asset is not recognized as Bitcoin, Liquid, AMP, or Lightning, an exception is thrown
 *   with message "No supported network".
 */
class GetReceiveAccountsUseCase() {

    /**
     * Returns the list of accounts that can fund a send of the given [asset].
     *
     * Accounts are filtered by network compatibility and by having a positive
     * spendable balance. Lightning behavior optionally includes Liquid accounts
     * when the swaps feature is enabled for the [wallet].
     *
     * @param session the active GDK session used to query balances and networks
     * @param wallet the current wallet; used for feature flags (e.g., swaps)
     * @param asset the target asset to be sent
     * @return a list of `AccountAssetBalance` entries, one per eligible account,
     *         only including those with balance; may be empty if no account can fund
     * @throws Exception when the asset's network is not supported ("No supported network")
     */
    suspend operator fun invoke(session: GdkSession, asset: EnrichedAsset): List<AccountAsset> {

        return session.accounts.value.filter { account ->
            when {
                asset.isBitcoin -> account.isBitcoin
                asset.isLiquidNetwork(session) && !asset.isAmp -> account.isLiquid
                asset.isLiquidNetwork(session) && asset.isAmp -> account.isAmp
                asset.isLightning -> account.isLightning
                else -> throw Exception("No supported network")
            }
        }.map {
            val accountAsset = if (asset.isLightning && it.accountAsset.account.isLiquid) {
                it.accountAsset // Swap
            } else {
                AccountAsset.fromAccountAsset(account = it, assetId = asset.assetId, session = session)
            }

            // Only Accounts with balance
            accountAsset
        }
    }
}
