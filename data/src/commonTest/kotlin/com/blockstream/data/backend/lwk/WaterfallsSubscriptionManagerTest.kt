package com.blockstream.data.backend.lwk

import com.blockstream.data.gdk.data.Block
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WaterfallsSubscriptionManagerTest {

    @Test
    fun subscribingScansOnce() = runTest {
        val manager = manager(mutableListOf())
        val account = FakeWaterfallsAccount()

        manager.ensureSubscribed(account)
        runCurrent()

        // The server replays nothing, so connecting has a gap only a scan can close.
        assertEquals(1, account.scans)
        assertEquals(setOf(account.id), manager.subscribed.value)
    }

    @Test
    fun tipEventUpdatesTheBlockWithoutScanning() = runTest {
        val tips = mutableListOf<Block>()
        val manager = manager(tips)
        val account = FakeWaterfallsAccount()

        manager.ensureSubscribed(account)
        runCurrent()

        account.streams.single().emit(
            WaterfallsEvent(kind = WaterfallsEventKind.TIP, tip = Block(hash = "abc", height = 42, timestamp = 7))
        )
        runCurrent()

        assertEquals(listOf(Block(hash = "abc", height = 42, timestamp = 7)), tips)
        assertEquals(1, account.scans) // still just the scan from connecting
    }

    @Test
    fun mempoolEventScansTheAccount() = runTest {
        val tips = mutableListOf<Block>()
        val manager = manager(tips)
        val account = FakeWaterfallsAccount()

        manager.ensureSubscribed(account)
        runCurrent()

        account.streams.single().emit(WaterfallsEvent(kind = WaterfallsEventKind.MEMPOOL, tip = null))
        runCurrent()

        assertEquals(2, account.scans)
        assertTrue(tips.isEmpty())
    }

    @Test
    fun blockEventScansAndUpdatesTheBlock() = runTest {
        val tips = mutableListOf<Block>()
        val manager = manager(tips)
        val account = FakeWaterfallsAccount()

        manager.ensureSubscribed(account)
        runCurrent()

        account.streams.single().emit(
            WaterfallsEvent(kind = WaterfallsEventKind.BLOCK, tip = Block(hash = "def", height = 43, timestamp = 8))
        )
        runCurrent()

        assertEquals(2, account.scans)
        assertEquals(listOf(Block(hash = "def", height = 43, timestamp = 8)), tips)
    }

    @Test
    fun reorgEventScans() = runTest {
        val manager = manager(mutableListOf())
        val account = FakeWaterfallsAccount()

        manager.ensureSubscribed(account)
        runCurrent()

        account.streams.single().emit(WaterfallsEvent(kind = WaterfallsEventKind.REORG, tip = null))
        runCurrent()

        assertEquals(2, account.scans)
    }

    @Test
    fun unknownEventScans() = runTest {
        val manager = manager(mutableListOf())
        val account = FakeWaterfallsAccount()

        manager.ensureSubscribed(account)
        runCurrent()

        account.streams.single().emit(WaterfallsEvent(kind = WaterfallsEventKind.UNKNOWN, tip = null))
        runCurrent()

        assertEquals(2, account.scans)
    }

    @Test
    fun endedStreamUnsubscribesAndReconnects() = runTest {
        val manager = manager(mutableListOf())
        val account = FakeWaterfallsAccount()

        manager.ensureSubscribed(account)
        runCurrent()
        assertEquals(setOf(account.id), manager.subscribed.value)

        account.streams.single().end()
        runCurrent()
        assertTrue(manager.subscribed.value.isEmpty())

        advanceTimeBy(WaterfallsSubscriptionManager.MIN_BACKOFF_MS)
        runCurrent()

        assertEquals(2, account.streams.size)
        assertEquals(setOf(account.id), manager.subscribed.value)
    }

    @Test
    fun erroringStreamUnsubscribesAndReconnects() = runTest {
        val manager = manager(mutableListOf())
        val account = FakeWaterfallsAccount()

        manager.ensureSubscribed(account)
        runCurrent()

        val stream = account.streams.single()
        stream.nextFailure = IllegalStateException("stream read failed")
        stream.emit(WaterfallsEvent(kind = WaterfallsEventKind.TIP, tip = null))
        runCurrent()

        assertTrue(manager.subscribed.value.isEmpty())
        assertTrue(stream.closed)

        advanceTimeBy(WaterfallsSubscriptionManager.MIN_BACKOFF_MS)
        runCurrent()

        assertEquals(2, account.streams.size)
        assertEquals(setOf(account.id), manager.subscribed.value)
    }

    @Test
    fun failedSubscribeRetriesWithGrowingBackoff() = runTest {
        val manager = manager(mutableListOf())
        val account = FakeWaterfallsAccount().apply { openFailures = 2 }

        manager.ensureSubscribed(account)
        runCurrent()
        assertEquals(0, account.streams.size)
        assertTrue(manager.subscribed.value.isEmpty())

        advanceTimeBy(WaterfallsSubscriptionManager.MIN_BACKOFF_MS)
        runCurrent()
        assertEquals(0, account.streams.size)

        // The second failure doubled the wait, so the minimum backoff is no longer enough.
        advanceTimeBy(WaterfallsSubscriptionManager.MIN_BACKOFF_MS)
        runCurrent()
        assertEquals(0, account.streams.size)

        advanceTimeBy(WaterfallsSubscriptionManager.MIN_BACKOFF_MS)
        runCurrent()
        assertEquals(1, account.streams.size)
        assertEquals(setOf(account.id), manager.subscribed.value)
    }

    @Test
    fun backoffResetsAfterAnEventArrives() = runTest {
        val manager = manager(mutableListOf())
        val account = FakeWaterfallsAccount().apply { openFailures = 1 }

        manager.ensureSubscribed(account)
        runCurrent()

        advanceTimeBy(WaterfallsSubscriptionManager.MIN_BACKOFF_MS)
        runCurrent()
        val first = account.streams.single()

        first.emit(WaterfallsEvent(kind = WaterfallsEventKind.MEMPOOL, tip = null))
        runCurrent()
        first.end()
        runCurrent()

        // Without the reset the wait would still be the doubled backoff.
        advanceTimeBy(WaterfallsSubscriptionManager.MIN_BACKOFF_MS)
        runCurrent()

        assertEquals(2, account.streams.size)
    }

    @Test
    fun backoffIsCapped() = runTest {
        val manager = manager(mutableListOf())
        val account = FakeWaterfallsAccount().apply { openFailures = Int.MAX_VALUE }

        manager.ensureSubscribed(account)
        runCurrent()

        // Well past the point where doubling would have run past the cap.
        advanceTimeBy(WaterfallsSubscriptionManager.MAX_BACKOFF_MS * 10)
        runCurrent()
        val attempts = account.attempts

        // Once it is capped, waiting the cap buys exactly one more attempt.
        advanceTimeBy(WaterfallsSubscriptionManager.MAX_BACKOFF_MS)
        runCurrent()

        assertEquals(attempts + 1, account.attempts)
    }

    @Test
    fun subscribingTwiceOpensOneStream() = runTest {
        val manager = manager(mutableListOf())
        val account = FakeWaterfallsAccount()

        manager.ensureSubscribed(account)
        manager.ensureSubscribed(account)
        runCurrent()

        assertEquals(1, account.streams.size)
    }

    @Test
    fun closeEndsStreamsAndStopsReconnecting() = runTest {
        val manager = manager(mutableListOf())
        val account = FakeWaterfallsAccount()

        manager.ensureSubscribed(account)
        runCurrent()
        val stream = account.streams.single()

        manager.close()

        // close() has to end the stream itself. It cannot leave that to the cancelled loop's
        // finally: in production the loop is parked in a blocking read that cancellation never
        // reaches, so that finally would never run.
        assertTrue(stream.closed)

        runCurrent()
        assertTrue(manager.subscribed.value.isEmpty())

        advanceTimeBy(WaterfallsSubscriptionManager.MAX_BACKOFF_MS * 2)
        runCurrent()

        assertEquals(1, account.streams.size)
    }

    @Test
    fun failedScanKeepsTheStreamAlive() = runTest {
        val manager = manager(mutableListOf())
        val account = FakeWaterfallsAccount()

        manager.ensureSubscribed(account)
        runCurrent()
        val stream = account.streams.single()

        account.scanFailure = IllegalStateException("scan failed")
        stream.emit(WaterfallsEvent(kind = WaterfallsEventKind.BLOCK, tip = null))
        runCurrent()

        assertEquals(2, account.scans)
        assertEquals(setOf(account.id), manager.subscribed.value)
        assertFalse(stream.closed)

        account.scanFailure = null
        stream.emit(WaterfallsEvent(kind = WaterfallsEventKind.MEMPOOL, tip = null))
        runCurrent()

        assertEquals(3, account.scans)
    }

    @Test
    fun closeStaleClosesASilentStreamAndReconnects() = runTest {
        val manager = manager(mutableListOf())
        val account = FakeWaterfallsAccount()

        manager.ensureSubscribed(account)
        runCurrent()
        val stream = account.streams.single()

        advanceTimeBy(WaterfallsSubscriptionManager.STALE_TIMEOUT_MS + 1)
        runCurrent()

        manager.closeStale()

        // Closing is what releases a read parked in a wedged stream.
        assertTrue(stream.closed)

        runCurrent()
        assertTrue(manager.subscribed.value.isEmpty())

        advanceTimeBy(WaterfallsSubscriptionManager.MIN_BACKOFF_MS)
        runCurrent()

        assertEquals(2, account.streams.size)
        assertEquals(setOf(account.id), manager.subscribed.value)
    }

    @Test
    fun closeStaleLeavesALiveStreamAlone() = runTest {
        val manager = manager(mutableListOf())
        val account = FakeWaterfallsAccount()

        manager.ensureSubscribed(account)
        runCurrent()
        val stream = account.streams.single()

        // An event keeps the stream alive past the timeout.
        advanceTimeBy(WaterfallsSubscriptionManager.STALE_TIMEOUT_MS - 1)
        runCurrent()
        stream.emit(WaterfallsEvent(kind = WaterfallsEventKind.TIP, tip = null))
        runCurrent()

        advanceTimeBy(WaterfallsSubscriptionManager.STALE_TIMEOUT_MS - 1)
        runCurrent()

        manager.closeStale()
        runCurrent()

        assertFalse(stream.closed)
        assertEquals(setOf(account.id), manager.subscribed.value)
        assertEquals(1, account.streams.size)
    }

    @Test
    fun anOwedScanIsRetriedOnTheNextEvent() = runTest {
        val manager = manager(mutableListOf())
        val account = FakeWaterfallsAccount()

        manager.ensureSubscribed(account)
        runCurrent()
        val stream = account.streams.single()

        account.scanFailure = IllegalStateException("scan failed")
        stream.emit(WaterfallsEvent(kind = WaterfallsEventKind.BLOCK, tip = null))
        runCurrent()
        assertEquals(2, account.scans)

        // A tip does not invalidate the wallet, but the scan the failure left owed must still run:
        // nothing else would ever scan this account again while its stream stays healthy.
        account.scanFailure = null
        stream.emit(WaterfallsEvent(kind = WaterfallsEventKind.TIP, tip = null))
        runCurrent()
        assertEquals(3, account.scans)

        // Once it has succeeded, a further tip scans nothing.
        stream.emit(WaterfallsEvent(kind = WaterfallsEventKind.TIP, tip = null))
        runCurrent()
        assertEquals(3, account.scans)
    }

    private fun TestScope.manager(tips: MutableList<Block>) =
        WaterfallsSubscriptionManager(
            scope = backgroundScope,
            onTip = { tips += it },
            now = { testScheduler.currentTime }
        )
}

internal class FakeWaterfallsStream : WaterfallsStream {
    private val events = Channel<WaterfallsEvent>(Channel.UNLIMITED)
    private val closedSignal = CompletableDeferred<Unit>()

    var closed = false
        private set

    /** Thrown by the next [next] call, once. */
    var nextFailure: Exception? = null

    suspend fun emit(event: WaterfallsEvent) {
        events.send(event)
    }

    fun end() {
        events.close()
    }

    suspend fun awaitClosed() = closedSignal.await()

    override suspend fun next(): WaterfallsEvent? {
        nextFailure?.let {
            nextFailure = null
            throw it
        }
        return events.receiveCatching().getOrNull()
    }

    override fun close() {
        closed = true
        events.close()
        closedSignal.complete(Unit)
    }
}

internal class FakeWaterfallsAccount(override val id: String = "liquid:0") : WaterfallsAccount {
    val streams = mutableListOf<FakeWaterfallsStream>()

    /** Completed with the first stream this account opens. */
    val opened = CompletableDeferred<FakeWaterfallsStream>()

    var scans = 0
        private set

    /** Every call to [openWaterfallsStream], whether it connected or threw. */
    var attempts = 0
        private set

    var openFailures = 0
    var scanFailure: Exception? = null

    /** Runs before a stream is handed back, so a test can make connecting slow. */
    var beforeOpen: (suspend () -> Unit)? = null

    override suspend fun openWaterfallsStream(): WaterfallsStream {
        attempts++

        beforeOpen?.invoke()

        if (openFailures > 0) {
            openFailures--
            throw IllegalStateException("subscribe rejected")
        }

        return FakeWaterfallsStream().also {
            streams += it
            opened.complete(it)
        }
    }

    override suspend fun scanBlockchain() {
        scans++
        scanFailure?.let { throw it }
    }
}
