package com.blockstream.gdk

import android.content.Context
import com.blockstream.gdk.data.Assets
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.params.AssetsParams
import com.blockstream.gdk.params.GetAssetsParams
import kotlinx.coroutines.CoroutineScope
import mu.KLogging

interface AssetQATester {
    fun isAssetFetchDisabled(): Boolean
    fun isAssetIconsFetchDisabled(): Boolean
}

interface AssetsProvider {
    fun refreshAssets(params: AssetsParams)
    fun getAssets(params: GetAssetsParams): Assets
}

/*
 * AssetManager is responsible of updating Assets and handle different caches
 * App Cache: cached data from apk
 * GDK Cache: cached data from a previous successful fetch
 */
class AssetManager constructor(
    private val context: Context,
    val coroutineScope: CoroutineScope,
    val qaTester: AssetQATester,
) {

    private val liquidAssetManager by lazy { NetworkAssetManager(context, coroutineScope, qaTester)}
    private val liquidTestnetAssetManager by lazy { NetworkAssetManager(context, coroutineScope, qaTester)}

    fun getNetworkAssetManager(network: Network): NetworkAssetManager {
        return if (network.isMainnet) {
            liquidAssetManager
        } else {
            liquidTestnetAssetManager
        }
    }

    companion object: KLogging()
}