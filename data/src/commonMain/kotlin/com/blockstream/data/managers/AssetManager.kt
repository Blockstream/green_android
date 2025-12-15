package com.blockstream.data.managers

import com.blockstream.data.gdk.data.LiquidAssets
import com.blockstream.data.gdk.params.AssetsParams
import com.blockstream.data.gdk.params.GetAssetsParams

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