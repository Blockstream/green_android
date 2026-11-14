// Builds the asset_info entries of a Jade sign_liquid_tx request from the transaction
// outputs, so Jade can show tickers and precision-formatted amounts for assets that are
// not in its built-in registry snapshot.
package com.blockstream.data.jade

import com.blockstream.data.gdk.data.Asset
import com.blockstream.data.gdk.data.InputOutput
import com.blockstream.data.gdk.params.GetAssetsParams
import com.blockstream.data.managers.AssetsProvider
import com.blockstream.jade.api.AssetInfo
import co.touchlab.kermit.Logger

// The policy asset is skipped since Jade has it built in. Assets missing from the registry
// or without a contract are skipped as well; Jade then shows them without a ticker.
// A failed registry lookup is treated the same way rather than blocking the signing.
suspend fun assetInfoForOutputs(
    outputs: List<InputOutput>,
    policyAsset: String,
    assetsProvider: AssetsProvider
): List<AssetInfo> {
    val assetIds = outputs.mapNotNull { it.assetId }.distinct().filter { it != policyAsset }

    if (assetIds.isEmpty()) {
        return listOf()
    }

    val registry = try {
        assetsProvider.getAssets(GetAssetsParams(assetIds))?.assets ?: mapOf()
    } catch (e: Exception) {
        Logger.w(tag = "JadeAssetInfo", throwable = e) { "Asset registry lookup failed, signing without asset info" }
        mapOf()
    }

    return assetIds.mapNotNull { assetId -> registry[assetId]?.toAssetInfo() }
}

private fun Asset.toAssetInfo(): AssetInfo? {
    val contract = contract ?: return null
    val issuancePrevout = issuancePrevout ?: return null

    return AssetInfo(assetId = assetId, contract = contract, issuancePrevout = issuancePrevout)
}
