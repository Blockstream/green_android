package com.blockstream.common.managers

import com.blockstream.common.gdk.data.LiquidAssets
import com.blockstream.common.gdk.params.AssetsParams
import com.blockstream.common.gdk.params.GetAssetsParams

interface AssetsProvider {
    fun refreshAssets(params: AssetsParams)
    fun getAssets(params: GetAssetsParams): LiquidAssets?
}

/*
 * AssetManager is responsible of updating Assets and handle different caches
 * App Cache: cached data from apk
 * GDK Cache: cached data from a previous successful fetch
 */
object AssetManager {
    private val liquidAssetManager by lazy { NetworkAssetManager() }
    private val liquidTestnetAssetManager by lazy { NetworkAssetManager() }

    fun getNetworkAssetManager(isMainnet: Boolean): NetworkAssetManager {
        return if (isMainnet) {
            liquidAssetManager
        } else {
            liquidTestnetAssetManager
        }
    }
}