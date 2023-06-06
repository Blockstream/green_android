package com.blockstream.common.managers

import com.blockstream.common.gdk.data.Assets
import com.blockstream.common.gdk.params.AssetsParams
import com.blockstream.common.gdk.params.GetAssetsParams
import kotlinx.coroutines.CoroutineScope

interface AssetQATester {
    fun isAssetFetchDisabled(): Boolean
}

interface AssetsProvider {
    fun refreshAssets(params: AssetsParams)
    fun getAssets(params: GetAssetsParams): Assets?
}

/*
 * AssetManager is responsible of updating Assets and handle different caches
 * App Cache: cached data from apk
 * GDK Cache: cached data from a previous successful fetch
 */
class AssetManager constructor(
    private val coroutineScope: CoroutineScope,
    val qaTester: AssetQATester,
) {

    private val liquidAssetManager by lazy { NetworkAssetManager(coroutineScope, qaTester)}
    private val liquidTestnetAssetManager by lazy { NetworkAssetManager(coroutineScope, qaTester)}

    fun getNetworkAssetManager(isMainnet: Boolean): NetworkAssetManager {
        return if (isMainnet) {
            liquidAssetManager
        } else {
            liquidTestnetAssetManager
        }
    }
}