package com.blockstream.data.backend.lwk

import com.blockstream.data.extensions.tryCatchNull
import com.blockstream.utils.Loggable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import lwk.BlockHeader
import lwk.ElectrumClient
import lwk.EsploraClient
import lwk.Network
import lwk.Transaction
import lwk.Txid
import lwk.Update
import lwk.WaterfallsClient
import lwk.Wollet
import lwk.WolletDescriptor
import kotlin.time.Duration.Companion.seconds

class LwkNetworkClient(val isTestnet: Boolean) : AutoCloseable {
    private val network = if (isTestnet) Network.testnet() else Network.mainnet()
    private val waterfallsUrl = if (isTestnet) WATERFALLS_URL_TESTNET else WATERFALLS_URL_MAINNET

    private val clients = mapOf(
        "waterfalls" to EsploraClient.newWaterfalls(waterfallsUrl, network),
        "esplora" to network.defaultEsploraClient(),
        "electrum" to network.defaultElectrumClient()
    )

    // Subscriptions are the only Waterfalls-specific call; scans, tip and broadcast go through the
    // esplora-compatible client in `clients`.
    private val waterfallsClient = WaterfallsClient(waterfallsUrl, network)

    suspend fun fullScanToIndex(wollet: Wollet, index: Int = BIP44_GAP_LIMIT): Update? = attemptClient("fullScanToIndex", esploraClient = {
        it.fullScanToIndex(wollet, index.toUInt())
    }, electrumClient = {
        it.fullScanToIndex(wollet, index.toUInt())
    })

    suspend fun broadcast(tx: Transaction): Txid = attemptClient("broadcast", esploraClient = {
        it.broadcast(tx)
    }, electrumClient = {
        it.broadcast(tx)
    })

    suspend fun tip(): BlockHeader = attemptClient("tip", esploraClient = {
        it.tip()
    }, electrumClient = {
        it.tip()
    })

    /**
     * Opens a Waterfalls subscription for [descriptor].
     *
     * `subscribe` blocks a thread and spawns a worker that only [WaterfallsStream.close] releases,
     * so the caller has to take ownership of the result even when it is being cancelled. That is
     * the caller's job: this must be invoked from inside a [NonCancellable] context, or a
     * cancellation landing mid-call discards the stream and parks its worker thread for good.
     */
    suspend fun openWaterfallsStream(descriptor: WolletDescriptor): WaterfallsStream =
        withContext(Dispatchers.IO) {
            LwkWaterfallsStream(waterfallsClient.subscribe(descriptor = descriptor))
        }

    private suspend fun <T> attemptClient(
        op: String,
        timeout: Long = 120,
        esploraClient: (EsploraClient) -> T,
        electrumClient: (ElectrumClient) -> T
    ): T =
        withContext(Dispatchers.IO) {
            var lastError: Throwable? = null
            for ((name, client) in clients) {
                coroutineContext.ensureActive()

                try {
                    logger.d { "$op via $name" }

                    return@withContext withTimeout(timeout.seconds) {
                        when (client) {
                            is EsploraClient -> esploraClient.invoke(client)
                            is ElectrumClient -> electrumClient.invoke(client)
                            else -> throw Exception("no client found")
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    logger.w { "LWK $name client timed out after ${timeout}secs" }
                    lastError = e
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.w { "LWK $name client failed: ${e.message}" }
                    lastError = e
                }
            }

            throw lastError ?: IllegalStateException("$op failed: no client attempted")
        }

    override fun close() {
        clients.forEach {
            tryCatchNull {
                it.value.destroy()
            }
        }

        tryCatchNull {
            waterfallsClient.destroy()
        }
    }

    companion object : Loggable() {
        const val BIP44_GAP_LIMIT = 20

        const val WATERFALLS_URL_MAINNET = "https://waterfalls.liquidwebwallet.org/liquid/api"
        const val WATERFALLS_URL_TESTNET = "https://waterfalls-elements-testnet.esplora.staging.blockstream.io:17771/"
    }
}