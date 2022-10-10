package com.blockstream.gdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.lifecycle.MutableLiveData
import com.blockstream.crypto.R
import com.blockstream.gdk.data.Asset
import com.blockstream.gdk.params.AssetsParams
import com.blockstream.gdk.params.GetAssetsParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KLogging

enum class CacheStatus {
    Empty, Latest
}

data class AssetStatus(
    var cacheStatus: CacheStatus = CacheStatus.Empty,
    var onProgress: Boolean = false,
)

/*
 * NetworkAssetManager is responsible of updating Assets and handle different caches
 * App Cache: cached data from apk
 */
class NetworkAssetManager constructor(
    private val context: Context,
    val coroutineScope: CoroutineScope,
    val qaTester: AssetQATester,
) {
    private var metadata = mutableMapOf<String, Asset?>()
    private var icons = mutableMapOf<String, Bitmap?>()

    // Internal representation of the Status
    private val status = AssetStatus()

    private val statusLiveData = MutableLiveData(status)
    private val assetsUpdatedEvent = MutableLiveData(0)

    fun getAssetsUpdated() = assetsUpdatedEvent

    fun getAsset(assetId: String, assetsProvider: AssetsProvider): Asset? {
        // Asset from GDK (cache or up2date)
        if (!metadata.containsKey(assetId)) {
            try {
                // If null save it in cache either way
                assetsProvider.getAssets(GetAssetsParams(listOf(assetId))).assets?.get(assetId).let {
                    metadata[assetId] = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return metadata[assetId]
    }

    fun getAssetIcon(assetId: String, assetsProvider: AssetsProvider): Bitmap? {
        if (!icons.containsKey(assetId)) {
            try {
                // If null save it in cache either way
                assetsProvider.getAssets(GetAssetsParams(listOf(assetId))).icons?.get(assetId).let {
                    icons[assetId] = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return icons[assetId]
    }

    fun hasAssetIcon(assetId: String): Boolean = icons.containsKey(assetId)

    fun getAssetDrawableOrNull(assetId: String, assetsProvider: AssetsProvider): Drawable? {
        getAssetIcon(assetId, assetsProvider)?.let {
            return BitmapDrawable(context.resources, it)
        }

        return null
    }

    fun getAssetDrawableOrDefault(assetId: String, assetsProvider: AssetsProvider): Drawable {
        return getAssetDrawableOrNull(assetId, assetsProvider)
            ?: context.getDrawable(R.drawable.ic_unknown_asset_60)!!
    }

    fun updateAssetsIfNeeded(provider: AssetsProvider, forceUpdate: Boolean = false) {
        if (status.cacheStatus != CacheStatus.Latest || forceUpdate) {

            coroutineScope.launch(context = Dispatchers.IO) {

                try {
                    statusLiveData.postValue(status.apply { onProgress = true })

                    // Allow forceUpdate to override QATester settings
                    if (!qaTester.isAssetFetchDisabled() || forceUpdate) {
                        // Try to update the registry
                        provider.refreshAssets(
                            AssetsParams(
                                assets = true,
                                icons = true,
                                refresh = true
                            )
                        )

                        // Clear our local cache
                        metadata.clear()

                        status.cacheStatus = CacheStatus.Latest
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

    companion object : KLogging()
}