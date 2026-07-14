package com.blockstream.data.backend.lwk

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test

/**
 * Opening a Waterfalls subscription blocks a thread and hands back something only its owner can
 * close. Virtual time cannot model a cancellation that lands while that call is in flight, so this
 * runs on real dispatchers.
 */
class WaterfallsSubscriptionCancellationTest {

    @Test
    fun cancellingWhileOpeningStillClosesTheStream() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val manager = WaterfallsSubscriptionManager(scope = scope, onTip = {})

        val account = FakeWaterfallsAccount().apply {
            beforeOpen = {
                withContext(Dispatchers.IO) {
                    Thread.sleep(300)
                }
            }
        }

        manager.ensureSubscribed(account)

        // Log out while the subscription is still being opened. Nothing has been registered yet, so
        // if the loop loses what it opened, the subscription's worker thread is parked for good.
        delay(100)
        manager.close()

        val stream = withTimeout(2_000) { account.opened.await() }
        withTimeout(2_000) { stream.awaitClosed() }

        scope.cancel()
    }
}
