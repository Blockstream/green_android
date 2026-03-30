package com.blockstream.domain.receive

import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAsset

/**
 * Selects which accounts can receive a given asset.
 *
 * Given the current session and a specific [EnrichedAsset], filters the user's
 * active accounts ([GdkSession.accounts]) to those whose network/type is
 * compatible with the asset. No balance check is applied — any compatible
 * account can receive.
 *
 * Network matching:
 * - Bitcoin asset → Bitcoin accounts only.
 * - Liquid asset (non-AMP) → Liquid accounts only.
 * - Liquid AMP asset → AMP accounts only.
 * - Lightning asset → Lightning accounts only.
 *
 * @throws Exception if the asset's network is not recognized ("No supported network").
 */
class GetReceiveAccountsUseCase {

    /**
     * Returns active accounts compatible with receiving the given [asset].
     *
     * @param session the active GDK session used to query accounts and networks.
     * @param asset the asset to receive.
     * @return a list of [AccountAsset] entries for eligible accounts; may be empty.
     * @throws Exception when the asset's network is not recognized ("No supported network").
     */
    operator fun invoke(session: GdkSession, asset: EnrichedAsset): List<AccountAsset> {
        return session.accounts.value.filter { account ->
            when {
                asset.isBitcoin -> account.isBitcoin
                asset.isLiquidNetwork(session) && !asset.isAmp -> account.isLiquid
                asset.isLiquidNetwork(session) && asset.isAmp -> account.isAmp
                asset.isLightning -> account.isLightning
                else -> throw Exception("No supported network")
            }
        }.map {
            if (asset.isLightning && it.accountAsset.account.isLiquid) {
                it.accountAsset // Swap
            } else {
                AccountAsset.fromAccountAsset(account = it, assetId = asset.assetId, session = session)
            }
        }
    }
}
