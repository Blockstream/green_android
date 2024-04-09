package com.blockstream.common.gdk.data

import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.gdk.GdkSession

data class Assets constructor(val assetsOrNull: Map<String, Long>? = null) {
    val assets
        get() = assetsOrNull ?: emptyMap()

    val isLoading
        get() = assetsOrNull == null

    // By default policy asset is first
    val policyAssetOrNull
        get() = assets.entries.firstOrNull()?.value

    val policyAsset
        get() = policyAssetOrNull ?: 0

    // Expect the first asset to be the policy BTC or L-BTC
    val policyId
        get() = assets.keys.firstOrNull()

    val hasFunds: Boolean
        get() = assets.values.sum() > 0

    val size
        get() = assets.size

    val withFunds
        get() = assets.filterValues { it > 0 }

    fun isEmpty() = assets.isEmpty()

    fun isNotEmpty() = !isEmpty()

    fun balanceOrNull(assetId: String?) = assets[assetId]

    fun balance(assetId: String) = balanceOrNull(assetId) ?: 0

    fun containsAsset(assetId: String) = assets.containsKey(assetId)

    fun toEnrichedAssets(session: GdkSession) = assetsOrNull?.mapKeys {
        EnrichedAsset.create(session = session, assetId = it.key)
    }

    fun toAccountAsset(account: Account, session: GdkSession): List<AccountAsset> {
        return assets.keys.map {
            AccountAsset.fromAccountAsset(account, it, session)
        }
    }
}