package com.blockstream.common.managers

import com.blockstream.common.CountlyBase
import com.blockstream.common.data.AppConfig
import com.blockstream.common.data.Promo
import com.blockstream.common.data.PromoFile
import com.blockstream.common.extensions.tryCatch
import com.blockstream.common.extensions.tryCatchNull
import com.blockstream.green.utils.Loggable
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.prepareGet
import io.ktor.client.utils.DEFAULT_HTTP_BUFFER_SIZE
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use

class PromoManager constructor(
    appConfig: AppConfig,
    val settingsManager: SettingsManager,
    val countly: CountlyBase
) {
    private val scope = CoroutineScope(context = Dispatchers.Default)

    private val cacheDir = "${appConfig.cacheDir}/promos"

    private val _promos: MutableStateFlow<List<Promo>> = MutableStateFlow(listOf())
    val promos: StateFlow<List<Promo>> = _promos

    init {
        logger.d { "PromoManager init" }

        tryCatchNull {
            FileSystem.SYSTEM.createDirectory(cacheDir.toPath())
        }

        Promo.CacheDir = cacheDir

        combine(
            merge(flowOf(Unit), countly.remoteConfigUpdateEvent),
            settingsManager.appSettingsStateFlow
        ) { _, appSettings ->
            if (!appSettings.tor) {
                updatePromos()
            }
        }.launchIn(scope)
    }

    private suspend fun updatePromos() {
        countly.getRemoteConfigValueForPromos()?.also { promos ->

            val allResources = promos.map {
                listOfNotNull(it.imageSmallFile, it.imageLargeFile, it.videoLargeFile)
            }.flatten()

            // Delete all unused resources
            FileSystem.SYSTEM.listOrNull(cacheDir.toPath())?.forEach { file ->
                if (allResources.find { it.file == file.name } == null) {
                    logger.d { "Delete $file" }
                    FileSystem.SYSTEM.delete(file)
                }
            }

            promos.filter {
                it.isVisible && !settingsManager.isPromoDismissed(it.id)
            }.filter {
                downloadPromoContentIfNeeded(it)
            }.also {
                _promos.value = it
            }
        }
    }

    private fun promoResources(promo: Promo): List<PromoFile> {
        return listOfNotNull(promo.imageSmallFile, promo.imageLargeFile, promo.videoLargeFile)
    }

    private suspend fun downloadPromoContentIfNeeded(promo: Promo): Boolean {
        logger.d { "downloadPromoContentIfNeeded for id:${promo.id}" }
        return promoResources(promo)
            .filter {
                !fileExists(it.filePath)
            }
            .all {
                downloadPromoFile(it)
            }
    }

    private fun fileExists(path: String) = FileSystem.SYSTEM.exists(path.toPath())

    private suspend fun downloadPromoFile(promoFile: PromoFile): Boolean {
        logger.d { "downloadFile ${promoFile.url} to ${promoFile.filePath}" }

        val path = promoFile.filePath.toPath()

        val tempPath = "${promoFile.filePath}_temp".toPath()

        return try {

            FileSystem.SYSTEM.sink(tempPath).buffer().use { sink ->
                val client = HttpClient()

                client.prepareGet(promoFile.url).execute { httpResponse ->
                    val channel: ByteReadChannel = httpResponse.body()

                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(DEFAULT_HTTP_BUFFER_SIZE.toLong())
                        while (!packet.exhausted()) {
                            sink.write(packet.readByteArray())
                        }
                    }
                }
            }

            FileSystem.SYSTEM.atomicMove(tempPath, path)

            logger.d { "File downloaded successfully to ${promoFile.file}" }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            logger.e { "Error while downloading the file: ${promoFile.file}" }
            FileSystem.SYSTEM.delete(path)

            false
        }
    }

    fun clearCache() {
        scope.launch {
            tryCatch {
                FileSystem.SYSTEM.listOrNull(cacheDir.toPath())?.forEach {
                    FileSystem.SYSTEM.delete(it)
                }

                updatePromos()
            }
        }
    }

    companion object : Loggable()
}