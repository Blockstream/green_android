package com.blockstream.domain.send

import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.domain.swap.IsLiquidToLightningSwapUseCase

/**
 * Determines which accounts can fund a send for the given [EnrichedAsset].
 *
 * Given the current [GdkSession] and [GreenWallet], this use case filters the user's accounts by
 * network/type compatibility and by having a spendable balance. It also respects feature flags
 * (e.g., using Liquid accounts to fund Lightning via swaps when enabled).
 *
 * Behavior summary:
 * - Bitcoin asset → include Bitcoin accounts only.
 * - Liquid asset (non‑AMP) → include Liquid accounts only.
 * - Liquid AMP asset → include AMP accounts only.
 * - Lightning asset → include Lightning accounts; if swaps are enabled for this wallet, Liquid
 *   accounts are also included as potential funding sources.
 *
 * Balance filtering:
 * - Each eligible account is mapped to an `AccountAsset` for the target asset and then wrapped into
 *   `AccountAssetBalance`. Only entries with a positive balance are returned via
 *   `AccountAssetBalance.createIfBalance(...)`.
 * - In the Lightning + Liquid swap case, the account's existing `accountAsset` is reused to reflect
 *   swap funding rather than constructing a standard asset mapping.
 *
 * Errors:
 * - If the asset's network/type is not recognized as Bitcoin, Liquid, AMP, or Lightning, an
 *   `Exception("No supported network")` is thrown.
 *
 * Thread-safety: read‑only; safe to call from coroutines.
 */
class GetSendAccountsUseCase(
    private val isLiquidToLightningSwapUseCase: IsLiquidToLightningSwapUseCase
) {

    /**
     * Returns the list of accounts that can fund a send of the given [asset].
     *
     * Accounts are filtered by network compatibility and by having a positive spendable balance.
     * For Lightning, Liquid accounts are also considered when swaps are enabled for the [wallet].
     *
     * @param session the active GDK session used to query balances and networks
     * @param wallet the current wallet; used for feature flags (e.g., swaps)
     * @param asset the target asset to be sent
     * @return a list of `AccountAssetBalance` entries, one per eligible account, only including
     *         those with balance; may be empty if no account can fund
     * @throws Exception when the asset's network is not supported ("No supported network")
     */
    suspend operator fun invoke(
        session: GdkSession, wallet: GreenWallet, asset: EnrichedAsset, address: String
    ): List<AccountAssetBalance> {

        return session.accounts.value.filter { account ->
            when {
                // Same Policy Asset
                asset.assetId == account.network.policyAsset -> true

                asset.isLiquidNetwork(session) && !asset.isAmp -> account.isLiquid
                asset.isLiquidNetwork(session) && asset.isAmp -> account.isAmp

                isLiquidToLightningSwapUseCase(
                    wallet = wallet,
                    asset = asset,
                    address = address,
                    accountAsset = account.accountAsset,
                    session = session
                ) -> true

                else -> false
            }
        }.mapNotNull {

            val accountAsset = if (asset.isPolicyAsset(session) && asset.assetId != it.accountAsset.assetId) {
                it.accountAsset // Swap
            } else {
                AccountAsset.fromAccountAsset(account = it, assetId = asset.assetId, session = session)
            }

            // Only Accounts with balance
            AccountAssetBalance.createIfBalance(accountAsset = accountAsset, session = session)
        }
    }
}
