package com.blockstream.domain.send

import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.extensions.getSafeQueryParameter
import com.blockstream.common.extensions.tryCatch
import com.blockstream.common.gdk.GdkSession
import com.eygraber.uri.toKmpUriOrNull

/**
 * Resolves the candidate assets for a send flow given a raw user [input].
 *
 * Supported inputs include on-chain Bitcoin, Liquid, and Lightning encodings (addresses, URIs,
 * invoices). Resolution rules:
 * - Bitcoin or Lightning → return a single `EnrichedAsset` corresponding to the network
 *   `policyAsset` (i.e., the native asset of the network).
 * - Liquid with `assetid` query parameter → return the specific Liquid asset identified by
 *   `assetid`.
 * - Liquid without `assetid` → return all Liquid assets from the wallet that both belong to the
 *   Liquid network and have a positive balance.
 *
 * Parsing details:
 * - Network detection and basic input validation are delegated to the active [GdkSession].
 * - Query parameters are extracted using a safe helper that also works with opaque URIs (e.g.,
 *   `liquidnetwork:lq1...?...`). See `toKmpUriOrNull()` and `getSafeQueryParameter(...)`.
 *
 * Errors:
 * - Throws an `Exception("id_invalid_address")` when the input cannot be parsed into a supported
 *   network/address.
 *
 * Thread-safety: this use case performs read-only operations against session state and is safe to
 * call from coroutines.
 */
class GetSendAssetsUseCase() {

    /**
     * Resolves the set of assets relevant to the given send [address].
     *
     * Behavior summary:
     * - Bitcoin/Lightning → returns one `EnrichedAsset` for `network.policyAsset`.
     * - Liquid with `assetid` → returns the specified asset.
     * - Liquid without `assetid` → returns all wallet assets with positive balance on Liquid.
     *
     * Notes:
     * - Uses [GdkSession.parseInput] for network detection.
     * - Uses safe query reading for `assetid` so that opaque URIs are supported.
     * - Liquid branch builds the list from `session.walletAssets`, filters for positive balances,
     *   and then filters to assets that actually belong to the session's Liquid network.
     *
     * @param session current wallet/session context used for parsing and asset lookups
     * @param address a raw address/payment request/URI string (on-chain, Liquid, or Lightning)
     * @return a non-empty list of candidate assets, or throws if the input is invalid
     * @throws Exception with message `id_invalid_address` when the input cannot be parsed
     */
    suspend operator fun invoke(session: GdkSession, address: String): List<EnrichedAsset> {

        return tryCatch {
            val network = session.parseInput(address)?.first

            val assetId = tryCatch { address.toKmpUriOrNull()?.getSafeQueryParameter("assetid") }

            if (network != null) {
                if (network.isBitcoin || network.isLightning) {
                    listOf(EnrichedAsset.create(session = session, assetId = network.policyAsset))
                } else if (network.isLiquid) {
                    if (assetId != null) {
                        listOf(EnrichedAsset.create(session = session, assetId = assetId))
                    } else {
                        // Find assets from your liquid accounts
                        session.walletAssets.value.data()?.assets?.filter { it.value > 0 }?.map {
                            EnrichedAsset.create(session = session, assetId = it.key)
                        }?.filter {
                            it.isLiquidNetwork(session)
                        }?.takeIf { it.isNotEmpty() } ?: listOf(
                            EnrichedAsset.create(
                                session = session,
                                network = network
                            )
                        ) // If no assets found, just return the network asset
                    }
                } else null
            } else null
        } ?: throw Exception("id_invalid_address")
    }
}
