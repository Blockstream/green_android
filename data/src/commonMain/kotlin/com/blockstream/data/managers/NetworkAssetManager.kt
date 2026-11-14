package com.blockstream.data.managers

import com.blockstream.data.BTC_POLICY_ASSET
import com.blockstream.data.CountlyBase
import com.blockstream.data.LN_BTC_POLICY_ASSET
import com.blockstream.data.backend.NetworkBackend
import com.blockstream.data.data.CountlyAsset
import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.gdk.data.Asset
import com.blockstream.data.gdk.params.AssetsParams
import com.blockstream.data.gdk.params.GetAssetsParams
import com.blockstream.utils.Loggable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.until
import kotlin.time.Clock
import kotlin.time.Instant

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
class NetworkAssetManager constructor(private val isMainnet: Boolean, private val countly: CountlyBase) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val metadata = mutableMapOf<String, Asset?>()
    private val icons = mutableMapOf<String, ByteArray?>()

    private val _statusStateFlow = MutableStateFlow(AssetStatus())
    private val _status get() = _statusStateFlow.value

    private val _assetsUpdateSharedFlow = MutableSharedFlow<Unit>(replay = 0)
    val assetsUpdateFlow = _assetsUpdateSharedFlow.asSharedFlow()

    val countlyAssetsFlow: StateFlow<List<CountlyAsset>>
        field = MutableStateFlow(listOf())

    init {
        countly.remoteConfigUpdateEvent.onEach {
            updateCountlyAssets()
        }.launchIn(scope)
    }

    fun getCountlyAsset(id: String?) = countlyAssetsFlow.value.find { it.assetId == id }

    fun updateCountlyAssets() {
        countly.getRemoteConfigValueForAssets(if (isMainnet) LIQUID_ASSETS_KEY else LIQUID_ASSETS_TESTNET_KEY).also {
            countlyAssetsFlow.value = it ?: listOf()
        }
    }

    suspend fun getEnrichedAsset(assetId: String, assetsProvider: AssetsProvider): EnrichedAsset {
        if (assetId == LN_BTC_POLICY_ASSET) {
            return (getEnrichedAsset(
                assetsProvider = assetsProvider,
                assetId = BTC_POLICY_ASSET
            ).copy(assetId = LN_BTC_POLICY_ASSET, name = "Lightning Bitcoin"))
        }

        val asset = getAsset(assetId, assetsProvider)
        val countlyAsset = getCountlyAsset(assetId)

        return EnrichedAsset(
            assetId = assetId,
            name = asset?.name,
            precision = asset?.precision ?: 0,
            ticker = asset?.ticker,
            entity = asset?.entity,
            isAmp = countlyAsset?.isAmp ?: false,
            isAmpLegacy = countlyAsset?.isAmpLegacy ?: false,
            weight = countlyAsset?.weight ?: 0,
        )
    }

    suspend fun cacheAssets(assetIds: Collection<String>, assetsProvider: AssetsProvider) {
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

    suspend fun getAsset(assetId: String, assetsProvider: AssetsProvider): Asset? {

        // Only allow liquid assets
        if(assetId == BTC_POLICY_ASSET) return null
        if(assetId == LN_BTC_POLICY_ASSET) return null

        // Asset from GDK (cache or up2date)
        if (!metadata.containsKey(assetId)) {
            try {
                logger.i { "Cache Asset Metadata Missed: $assetId" }
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

    fun getAssetIconOrNull(assetId: String): ByteArray? {
        return icons[assetId]
    }

    suspend fun getAssetIcon(assetId: String, assetsProvider: AssetsProvider): ByteArray? {
        if (!icons.containsKey(assetId)) {
            try {
                logger.i { "Cache Asset Icon Missed: $assetId" }
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

    fun hasAssetIcon(assetId: String): Boolean = icons[assetId] != null

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

                    cacheAssets(countlyAssetsFlow.value.map { it.assetId }, provider)

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

    companion object : Loggable() {
        const val LIQUID_ASSETS_KEY = "liquid_assets"
        const val LIQUID_ASSETS_TESTNET_KEY = "liquid_assets_testnet"
    }
}