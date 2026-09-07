package com.blockstream.data.backend.lwk

import com.blockstream.data.gdk.data.Block
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LwkSyncLoopTest {

    @Test
    fun pollsTheTipAndScansAnAccountWithoutAStream() = runTest {
        val tips = mutableListOf<Block>()
        val manager = manager(tips)
        val account = FakeWaterfallsAccount()

        loop(manager, tips, accounts = { listOf(account) }).start(backgroundScope)
        runCurrent()

        assertEquals(listOf(TIP), tips)
        assertTrue(account.scans >= 1)

        // Waterfalls only accepts a subscription for a descriptor it has already scanned, so the
        // scan is what makes the account eligible for a stream.
        assertEquals(setOf(account.id), manager.subscribed.value)
    }

    @Test
    fun aSubscribedAccountIsLeftToItsStream() = runTest {
        val tips = mutableListOf<Block>()
        val manager = manager(tips)
        val account = FakeWaterfallsAccount()

        loop(manager, tips, accounts = { listOf(account) }).start(backgroundScope)
        runCurrent()
        val scans = account.scans
        val polled = tips.size

        advanceTimeBy(INTERVAL)
        runCurrent()

        assertEquals(scans, account.scans)
        assertEquals(polled, tips.size)
    }

    @Test
    fun pollsTheTipWhenThereAreNoAccounts() = runTest {
        val tips = mutableListOf<Block>()

        loop(manager(tips), tips, accounts = { emptyList() }).start(backgroundScope)
        runCurrent()

        assertEquals(listOf(TIP), tips)
    }

    @Test
    fun oneAccountFailingDoesNotStarveTheNext() = runTest {
        val tips = mutableListOf<Block>()
        val manager = manager(tips)
        val broken = FakeWaterfallsAccount(id = "liquid:0").apply { scanFailure = IllegalStateException("scan failed") }
        val healthy = FakeWaterfallsAccount(id = "liquid:1")

        loop(manager, tips, accounts = { listOf(broken, healthy) }).start(backgroundScope)
        runCurrent()

        assertEquals(setOf(healthy.id), manager.subscribed.value)
    }

    @Test
    fun aFailingTipPollStillScans() = runTest {
        val tips = mutableListOf<Block>()
        val account = FakeWaterfallsAccount()

        loop(
            manager(tips),
            tips,
            accounts = { listOf(account) },
            fetchTip = { throw IllegalStateException("tip unavailable") }
        ).start(backgroundScope)
        runCurrent()

        assertTrue(tips.isEmpty())
        assertTrue(account.scans >= 1)
    }

    @Test
    fun aPassThatThrowsDoesNotEndTheLoop() = runTest {
        val tips = mutableListOf<Block>()
        val account = FakeWaterfallsAccount()
        var failures = 1

        loop(manager(tips), tips, accounts = {
            if (failures-- > 0) throw IllegalStateException("accounts unavailable")
            listOf(account)
        }).start(backgroundScope)
        runCurrent()
        assertTrue(tips.isEmpty())
        assertEquals(0, account.scans)

        // This loop is the only thing that recovers a lost subscription, so a pass that throws
        // must not be its last.
        advanceTimeBy(INTERVAL)
        runCurrent()

        assertEquals(listOf(TIP), tips)
        assertTrue(account.scans >= 1)
    }

    private fun TestScope.manager(tips: MutableList<Block>) =
        WaterfallsSubscriptionManager(
            scope = backgroundScope,
            onTip = { tips += it },
            now = { testScheduler.currentTime }
        )

    private fun loop(
        manager: WaterfallsSubscriptionManager,
        tips: MutableList<Block>,
        accounts: () -> List<WaterfallsAccount>,
        fetchTip: suspend () -> Block = { TIP }
    ) = LwkSyncLoop(
        subscriptionManager = manager,
        accounts = accounts,
        fetchTip = fetchTip,
        onTip = { tips += it },
        interval = INTERVAL
    )

    companion object {
        private val TIP = Block(hash = "tip", height = 1, timestamp = 0)
        private val INTERVAL = LwkSyncLoop.INTERVAL
    }
}
