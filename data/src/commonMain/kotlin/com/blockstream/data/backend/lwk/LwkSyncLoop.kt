package com.blockstream.data.backend.lwk

import com.blockstream.data.gdk.data.Block
import com.blockstream.jade.Loggable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Keeps LWK accounts current while they have no live Waterfalls stream. Every pass recycles the
 * streams that went silent, polls the tip for the accounts that still depend on it, scans those
 * accounts and hands them to [subscriptionManager]. It is the only thing that brings an account
 * back once its stream is gone, so a pass that throws is logged and the next one still runs.
 */
class LwkSyncLoop(
    private val subscriptionManager: WaterfallsSubscriptionManager,
    private val accounts: () -> List<WaterfallsAccount>,
    private val fetchTip: suspend () -> Block,
    private val onTip: suspend (Block) -> Unit,
    private val interval: Duration = INTERVAL
) {
    fun start(scope: CoroutineScope): Job = scope.launch {
        while (isActive) {
            try {
                sync()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.w { "LWK sync failed: ${e.message}" }
            }

            delay(interval)
        }
    }

    private suspend fun sync() {
        // A wedged stream never ends by itself; recycling it here hands the account back.
        subscriptionManager.closeStale()

        val subscribed = subscriptionManager.subscribed.value
        val accounts = accounts()
        val unsubscribed = accounts.filter { it.id !in subscribed }

        try {
            // A subscribed account is told about every new tip, so the tip is only worth
            // fetching while some account still depends on the poll.
            if (unsubscribed.isNotEmpty() || accounts.isEmpty()) {
                onTip(fetchTip())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.w { "LWK tip poll failed: ${e.message}" }
        }

        // One account's failure must not starve the accounts behind it.
        unsubscribed.forEach { account ->
            try {
                account.scanBlockchain()

                // Waterfalls only accepts a subscription for a descriptor it has already scanned.
                subscriptionManager.ensureSubscribed(account)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.w { "LWK scan for ${account.id} failed: ${e.message}" }
            }
        }
    }

    companion object : Loggable() {
        val INTERVAL = 20.seconds
    }
}
