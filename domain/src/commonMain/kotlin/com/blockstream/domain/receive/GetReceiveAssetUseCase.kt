package com.blockstream.domain.receive

import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.gdk.GdkSession

/**
 * Use case that determines which assets are relevant for a send flow given a raw input string.
 *
 * Input can be a variety of schemes (e.g., on-chain Bitcoin, Liquid, or Lightning). The logic:
 * - If the parsed network is Bitcoin or Lightning, returns a single `EnrichedAsset` for the
 *   network's `policyAsset` (i.e., the native asset for that network).
 * - If the parsed network is Liquid and the input contains an `assetid` query parameter, returns
 *   that specific asset.
 * - If the parsed network is Liquid and no `assetid` is present, returns all positive-balance
 *   assets from the user's wallet that belong to the Liquid network.
 *
 * Notes on query parsing:
 * - This use case uses a safe query-parameter extraction strategy to support opaque URIs
 *   (e.g., `liquidnetwork:lq1...?...`) that are not hierarchical. See `getSafeQueryParameter`
 *   and `UriUtils.getQueryParameter` for details.
 *
 * Errors:
 * - If the input cannot be parsed into a supported network/address, the operation throws with
 *   message `id_invalid_address`.
 */
class GetReceiveAssetsUseCase() {

    /**
     * Resolves the set of assets relevant to the given send [input].
     *
     * Parsing is delegated to the active [GdkSession] which identifies the target
     * network and, when applicable, the `assetid` embedded in the input (including
     * opaque URIs such as `liquidnetwork:` via `getSafeQueryParameter`).
     *
     * Behavior summary:
     * - Bitcoin/Lightning → returns one `EnrichedAsset` for `network.policyAsset`.
     * - Liquid with `assetid` → returns the specified asset.
     * - Liquid without `assetid` → returns all wallet assets with positive balance on Liquid.
     *
     * @param session current wallet/session context used for parsing and asset lookups
     * @param input a raw address/payment request/URI string (e.g., on-chain, Liquid, or Lightning)
     * @return a non-empty list of candidate assets, or throws if the input is invalid
     * @throws Exception with message `id_invalid_address` when the input cannot be parsed
     */
    suspend operator fun invoke(session: GdkSession): List<EnrichedAsset> {

        return tryCatch {

            val policies = listOfNotNull(
                EnrichedAsset.createOrNull(
                    session = session,
                    assetId = session.bitcoin?.policyAsset
                ),
                EnrichedAsset.createOrNull(
                    session = session,
                    assetId = session.lightning?.policyAsset
                ).takeIf { session.hasLightning },
                EnrichedAsset.createOrNull(
                    session = session,
                    assetId = session.liquid?.policyAsset
                )
            ).toSet()

            val popularAssets = (session.enrichedAssets.value.takeIf { session.liquid != null }
                ?.filter { !it.isAmp || session.hasAmpAccount }?.map {
                    EnrichedAsset.create(session = session, assetId = it.assetId)
                }?.toSet() ?: emptySet())

            val walletAsset = session.walletAssets.value.data()?.assets?.keys?.map {
                EnrichedAsset.create(session = session, assetId = it)
            }?.toSet() ?: emptySet()

            val anyAssets = setOfNotNull(
                EnrichedAsset.createAnyAsset(session = session, isAmp = false)
                    .takeIf { session.hasAmpAccount },
                EnrichedAsset.createAnyAsset(session = session, isAmp = true)
                    .takeIf { !session.isHwWatchOnly }
            )

            (policies + popularAssets + walletAsset.sortedWith(session::sortEnrichedAssets) + anyAssets).toList()

        } ?: listOf()
    }
}
