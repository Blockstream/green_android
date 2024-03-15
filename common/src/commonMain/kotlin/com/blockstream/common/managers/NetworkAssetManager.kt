package com.blockstream.common.managers

import co.touchlab.kermit.Logger
import com.blockstream.common.gdk.data.Asset
import com.blockstream.common.gdk.params.AssetsParams
import com.blockstream.common.gdk.params.GetAssetsParams
import com.blockstream.common.utils.Loggable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.until

enum class CacheStatus {
    Empty, Latest
}

data class AssetStatus constructor(
    var cacheStatus: CacheStatus = CacheStatus.Empty,
    var updatedAt: Instant? = null,
    var onProgress: Boolean = false,
)

/*
 * NetworkAssetManager is responsible of updating Assets and handle different caches
 * App Cache: cached data from apk
 */
class NetworkAssetManager constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val metadata = mutableMapOf<String, Asset?>()
    private val icons = mutableMapOf<String, ByteArray?>()

    private val _statusStateFlow = MutableStateFlow(AssetStatus())
    private val _status get() = _statusStateFlow.value

    private val _assetsUpdateSharedFlow = MutableSharedFlow<Unit>(replay = 0)
    val assetsUpdateFlow = _assetsUpdateSharedFlow.asSharedFlow()

    fun cacheAssets(assetIds: Collection<String>, assetsProvider: AssetsProvider) {
        assetIds.filter { !metadata.containsKey(it) && !icons.containsKey(it) }.takeIf { it.isNotEmpty() }?.also { unCachedIds ->
            assetsProvider.getAssets(GetAssetsParams(unCachedIds))?.also { assets ->
                // get_assets only returns non null assets, so we need to add nulls for the missing assets
                unCachedIds.forEach { assetId ->
                    metadata[assetId] = assets.assets?.get(assetId)
                    icons[assetId] = assets.icons?.get(assetId)
                }
            }
        }
    }

    fun getAsset(assetId: String, assetsProvider: AssetsProvider): Asset? {
        // Asset from GDK (cache or up2date)
        if (!metadata.containsKey(assetId)) {
            try {
                Logger.i { "Cache Asset Metadata Missed: $assetId" }
                // If null save it in cache either way
                assetsProvider.getAssets(GetAssetsParams(listOf(assetId)))?.assets?.get(assetId).let {
                    metadata[assetId] = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return metadata[assetId]
    }

    fun getAssetIcon(assetId: String, assetsProvider: AssetsProvider): ByteArray? {
        if (!icons.containsKey(assetId)) {
            try {
                Logger.i { "Cache Asset Icon Missed: $assetId" }
                // If null save it in cache either way
                assetsProvider.getAssets(GetAssetsParams(listOf(assetId)))?.icons?.get(assetId).let {
                    icons[assetId] = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return icons[assetId]
    }

    fun hasAssetIcon(assetId: String): Boolean = icons.containsKey(assetId)


    fun updateAssetsIfNeeded(provider: AssetsProvider) {
        val lastUpdate = _status.updatedAt?.until(Clock.System.now(), DateTimeUnit.SECOND, TimeZone.UTC)

        if (lastUpdate == null || lastUpdate > 120) {
            logger.i { "Liquid Assets are being updated... ${lastUpdate?.let { "Cache is $it secs old." } ?: "Cache is empty."}" }
            scope.launch {
                try {
                    _statusStateFlow.value = _status.apply { onProgress = true }

                    // Try to update the registry
                    provider.refreshAssets(
                        AssetsParams(
                            assets = true,
                            icons = true,
                            refresh = true
                        )
                    )

                    // Remove null assets from cache
                    metadata.filterValues { it == null }.forEach {
                        metadata.remove(it.key)
                    }

                    icons.filterValues { it == null }.forEach {
                        icons.remove(it.key)
                    }

                    _status.cacheStatus = CacheStatus.Latest
                    _status.updatedAt = Clock.System.now().also {
                        logger.i { "Liquid Assets updated at $it" }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    _statusStateFlow.value = _status.apply { onProgress = false }
                    _assetsUpdateSharedFlow.tryEmit(Unit)
                }
            }
        } else {
            logger.i { "Liquid Assets cached at ${_status.updatedAt.toString()}, $lastUpdate secs old. Skipped." }
        }
    }

    companion object: Loggable()
}