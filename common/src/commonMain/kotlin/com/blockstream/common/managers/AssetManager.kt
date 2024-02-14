package com.blockstream.common.managers

import com.blockstream.common.gdk.data.LiquidAssets
import com.blockstream.common.gdk.params.AssetsParams
import com.blockstream.common.gdk.params.GetAssetsParams
import org.koin.core.annotation.Single

interface AssetQATester {
    fun isAssetFetchDisabled(): Boolean
}

interface AssetsProvider {
    fun refreshAssets(params: AssetsParams)
    fun getAssets(params: GetAssetsParams): LiquidAssets?
}

/*
 * AssetManager is responsible of updating Assets and handle different caches
 * App Cache: cached data from apk
 * GDK Cache: cached data from a previous successful fetch
 */
class AssetManager constructor(
    val qaTester: AssetQATester? = null
) {

    private val liquidAssetManager by lazy { NetworkAssetManager(qaTester)}
    private val liquidTestnetAssetManager by lazy { NetworkAssetManager(qaTester)}

    fun getNetworkAssetManager(isMainnet: Boolean): NetworkAssetManager {
        return if (isMainnet) {
            liquidAssetManager
        } else {
            liquidTestnetAssetManager
        }
    }
}