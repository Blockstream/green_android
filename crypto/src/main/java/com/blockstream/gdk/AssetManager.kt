package com.blockstream.gdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.lifecycle.MutableLiveData
import com.blockstream.crypto.R
import com.blockstream.gdk.data.Asset
import com.blockstream.gdk.data.Assets
import com.blockstream.gdk.params.AssetsParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KLogging

interface AssetQATester {
    fun isAssetGdkCacheDisabled(): Boolean
    fun isAssetFetchDisabled(): Boolean
    fun isAssetIconsFetchDisabled(): Boolean
}

interface AssetsProvider {
    fun refreshAssets(params: AssetsParams): Assets
}

enum class CacheStatus {
    Empty, Gdk, Latest
}

data class AssetStatus(
    var metadataStatus: CacheStatus = CacheStatus.Empty,
    var iconStatus: CacheStatus = CacheStatus.Empty,
    var onProgress: Boolean = false,
)

/*
 * AssetManager is responsible of updating Assets and handle different caches
 * App Cache: cached data from apk
 * GDK Cache: cached data from a previous successful fetch
 */
class AssetManager constructor(
    private val context: Context,
    val coroutineScope: CoroutineScope,
    val QATester: AssetQATester,
) {
    private var metadata: Map<String, Asset> = mapOf()
    private var icons: Map<String, Bitmap?> = mapOf()

    // Internal representation of the Status
    private val status = AssetStatus()

    private val statusLiveData =  MutableLiveData(status)
    private val assetsUpdatedEvent =  MutableLiveData(0)

    fun getAssetsUpdated() = assetsUpdatedEvent

    private fun setGdkCache(assets: Assets) {
        logger.info { "Liquid Assets update from GDK" }
        this.metadata = assets.assets
        this.icons = assets.icons ?: mapOf()

        // Status: Gdk
        status.metadataStatus = CacheStatus.Gdk
        status.iconStatus = CacheStatus.Gdk
    }

    private fun updateMetadata(assets: Assets) {
        logger.info { "Liquid Assets metadata update from session" }
        this.metadata = assets.assets
        // Status: Metadata Latest
        status.metadataStatus = CacheStatus.Latest
    }

    // Currently unused as the assets are integrated in the build
    private fun updateIcons(assets: Assets) {
        logger.info { "Liquid Assets icon update from session" }
        this.icons = assets.icons ?: mapOf()
        // Status: Icons Latest
        status.iconStatus = CacheStatus.Latest
    }

    fun getAsset(assetId: String): Asset? {
        // Asset from GDK (cache or up2date)
        return metadata[assetId]
    }

    fun hasAssetIcon(assetId: String): Boolean = getAssetIcon(assetId) != null

    fun getAssetDrawableOrNull(assetId: String): Drawable? {
        getAssetIcon(assetId)?.let {
            return BitmapDrawable(context.resources, it)
        }

        return null
    }

    fun getAssetDrawableOrDefault(assetId: String): Drawable {
        return getAssetDrawableOrNull(assetId) ?: context.getDrawable(R.drawable.ic_unknown_asset_60)!!
    }

    fun updateAssetsIfNeeded(provider: AssetsProvider) {
        // Init from GDK if required
        if (status.metadataStatus == CacheStatus.Empty && !QATester.isAssetGdkCacheDisabled()) {
            try {
                statusLiveData.postValue(status.apply { onProgress = true })

                setGdkCache(
                    provider.refreshAssets(
                        AssetsParams(
                            assets = true,
                            icons = true,
                            refresh = false
                        )
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }finally {
                statusLiveData.postValue(status.apply { onProgress = false })
            }
        }

        updateMetadataFromSession(provider, false)
    }

    private fun updateMetadataFromSession(provider: AssetsProvider, forceUpdate: Boolean) {
        if (status.metadataStatus != CacheStatus.Latest || status.iconStatus != CacheStatus.Latest || forceUpdate) {

            coroutineScope.launch(context = Dispatchers.IO) {

                try {

                    statusLiveData.postValue(status.apply { onProgress = true })

                    if (status.metadataStatus != CacheStatus.Latest || forceUpdate) {

                        // Allow forceUpdate to override QATester settings
                        if (!QATester.isAssetFetchDisabled() || forceUpdate) {
                            // Try to update the registry - only metadata
                            // Fetch assets without icons as we have better chances to complete the network call
                            updateMetadata(
                                provider.refreshAssets(
                                    AssetsParams(
                                        assets = true,
                                        icons = false,
                                        refresh = true
                                    )
                                )
                            )
                        }
                    }

                    if (status.iconStatus != CacheStatus.Latest || forceUpdate) {

                        // Allow forceUpdate to override QATester settings
                        if (!QATester.isAssetIconsFetchDisabled() || forceUpdate) {
                            // Try to update the registry - only icons
                            updateIcons(
                                provider.refreshAssets(
                                    AssetsParams(
                                        assets = false,
                                        icons = true,
                                        refresh = true
                                    )
                                )
                            )
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    statusLiveData.postValue(status.apply { onProgress = false })
                    assetsUpdatedEvent.postValue((assetsUpdatedEvent.value ?: 0) + 1)
                }
            }
        }
    }

    fun getAssetIcon(assetId: String): Bitmap? {
        return icons[assetId]
    }

    companion object: KLogging()
}