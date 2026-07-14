package com.blockstream.data.backend.lwk

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.blockstream.data.gdk.data.Block
import com.blockstream.jade.Loggable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.time.Clock

/**
 * Keeps one Waterfalls subscription per account and turns the hints it receives into block updates
 * and blockchain scans. The accounts it is not currently subscribed to are reported through
 * [subscribed] so the caller can poll them instead.
 */
class WaterfallsSubscriptionManager(
    private val scope: CoroutineScope,
    private val onTip: suspend (Block) -> Unit,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    /** The accounts with a live stream. Every other account has to be polled. */
    val subscribed: StateFlow<Set<String>>
    field = MutableStateFlow<Set<String>>(emptySet())

    private val jobs = ConcurrentMutableMap<String, Job>()
    private val streams = ConcurrentMutableMap<String, WaterfallsStream>()
    private val lastEventAt = ConcurrentMutableMap<String, Long>()

    /** Subscribes [account] unless it is already subscribed. */
    fun ensureSubscribed(account: WaterfallsAccount) {
        jobs.computeIfAbsent(account.id) {
            scope.launch { subscribe(account) }
        }
    }

    private suspend fun subscribe(account: WaterfallsAccount) {
        var backoffMs = MIN_BACKOFF_MS

        while (currentCoroutineContext().isActive) {
            try {
                // Opening blocks a thread and hands back a stream only its owner can close, so it
                // has to complete and be registered even if this loop is being cancelled meanwhile.
                // A cancellable open would drop the stream and park its worker thread for good.
                val stream = withContext(NonCancellable) { account.openWaterfallsStream() }
                streams[account.id] = stream
                lastEventAt[account.id] = now()
                subscribed.update { it + account.id }
                logger.d { "Waterfalls subscribed ${account.id}" }

                try {
                    // Nothing that happened while this account had no stream is replayed on it, and
                    // a scan that fails stays owed: while the stream is healthy the poll skips this
                    // account, so nothing else would ever pick that work back up.
                    var scanOwed = true

                    while (currentCoroutineContext().isActive) {
                        if (scanOwed) {
                            scanOwed = !scan(account)
                        }

                        val event = stream.next() ?: break

                        backoffMs = MIN_BACKOFF_MS
                        lastEventAt[account.id] = now()
                        logger.d { "Waterfalls ${event.kind} for ${account.id}" }

                        event.tip?.let { onTip(it) }

                        if (event.kind.invalidatesWallet) {
                            scanOwed = true
                        }
                    }
                } finally {
                    subscribed.update { it - account.id }
                    streams.remove(account.id)
                    lastEventAt.remove(account.id)
                    stream.close()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.w { "Waterfalls subscription ${account.id} failed: ${e.message}" }
            }

            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
        }
    }

    /** Scans [account], reporting whether it succeeded. A failure must not take the stream down. */
    private suspend fun scan(account: WaterfallsAccount): Boolean = try {
        account.scanBlockchain()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.w { "Waterfalls scan for ${account.id} failed: ${e.message}" }
        false
    }

    /**
     * Closes every stream that has not produced an event for [STALE_TIMEOUT_MS].
     *
     * Waterfalls reports every new block, so a live stream is never quiet for that long: silence
     * means the connection died without saying so, leaving the read parked. Closing it releases the
     * read, hands the account back to the poll, and lets the loop reconnect.
     */
    fun closeStale() {
        val deadline = now() - STALE_TIMEOUT_MS

        val stale = streams.block { open ->
            open.keys
                .filter { (lastEventAt[it] ?: Long.MAX_VALUE) <= deadline }
                .mapNotNull { id -> open.remove(id) }
        }

        stale.forEach {
            logger.w { "Waterfalls stream went silent, closing it" }
            it.close()
        }
    }

    /**
     * Ends every stream and stops the subscription loops.
     *
     * The streams are closed before the jobs are cancelled, and closed by this method rather than
     * by the loops themselves: a loop parked in [WaterfallsStream.next] is blocked inside LWK,
     * where cancellation cannot reach it, and only closing its stream releases it.
     */
    fun close() {
        streams.block { it.values.toList().also { _ -> it.clear() } }.forEach { it.close() }
        jobs.block { it.values.toList().also { _ -> it.clear() } }.forEach { it.cancel() }
        subscribed.value = emptySet()
    }

    companion object : Loggable() {
        const val MIN_BACKOFF_MS = 2_000L
        const val MAX_BACKOFF_MS = 60_000L

        /** Liquid produces a block about every minute, and every block is an event on a live stream. */
        const val STALE_TIMEOUT_MS = 180_000L
    }
}
