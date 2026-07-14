package com.blockstream.data.backend.lwk

import com.blockstream.data.extensions.tryCatchNull
import com.blockstream.data.gdk.data.Block
import com.blockstream.utils.Loggable
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import lwk.WaterfallsSubscription
import lwk.WaterfallsSubscriptionEvent

/**
 * Presents LWK's blocking descriptor subscription as a [WaterfallsStream].
 */
class LwkWaterfallsStream(private val subscription: WaterfallsSubscription) : WaterfallsStream {
    private val closed = atomic(false)

    /**
     * [WaterfallsSubscription.nextUpdate] parks the thread until the server sends a hint, so it
     * always runs on [Dispatchers.IO]. Cancelling the caller does not release it — only [close] does.
     */
    override suspend fun next(): WaterfallsEvent? = withContext(Dispatchers.IO) {
        subscription.nextUpdate()?.toWaterfallsEvent()
    }

    /**
     * Ends the stream and frees it. Closing has to happen before destroying: destroying is
     * reference-counted and safe while [next] is in flight, but closing a destroyed subscription
     * throws. Closing twice is normal — the manager closes the stream and the loop that owned it
     * closes it again — so the second call must do nothing at all.
     */
    override fun close() {
        if (!closed.compareAndSet(expect = false, update = true)) return

        tryCatchNull { subscription.closeSubscription() }
        tryCatchNull { subscription.destroy() }
    }

    companion object: Loggable()
}

private fun WaterfallsSubscriptionEvent.toWaterfallsEvent(): WaterfallsEvent = WaterfallsEvent(
    kind = when (kind) {
        "tip" -> WaterfallsEventKind.TIP
        "mempool" -> WaterfallsEventKind.MEMPOOL
        "block" -> WaterfallsEventKind.BLOCK
        "reorg" -> WaterfallsEventKind.REORG
        else -> WaterfallsEventKind.UNKNOWN
    },
    tip = tip?.let {
        Block(
            hash = it.blockHash,
            height = it.height.toLong(),
            timestamp = it.timestamp.toLong()
        )
    }
)
