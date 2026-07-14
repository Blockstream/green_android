package com.blockstream.data.backend.lwk

import com.blockstream.data.gdk.data.Block

/**
 * The types the Waterfalls descriptor subscription is expressed in: the hints the server pushes,
 * the stream they arrive on, and the account a stream watches.
 */

/**
 * A hint from the Waterfalls server. It never carries wallet state: anything other than
 * [WaterfallsEventKind.TIP] means the account's watched scripts may have changed and the
 * authoritative state has to come from a blockchain scan.
 */
data class WaterfallsEvent(
    val kind: WaterfallsEventKind,
    val tip: Block?
)

enum class WaterfallsEventKind {
    /** A new chain tip, with no change to the account's watched scripts. */
    TIP,

    /** A watched script changed in the mempool. */
    MEMPOOL,

    /** A new block changed a watched script. */
    BLOCK,

    /** The chain reorganized. */
    REORG,

    /** A kind this client does not know. Treated as an invalidation so a scan settles the truth. */
    UNKNOWN;

    val invalidatesWallet: Boolean
        get() = this != TIP
}

/** A live Waterfalls subscription for a single descriptor. */
interface WaterfallsStream {
    /** Suspends until the next hint arrives. Returns null once the stream has ended. */
    suspend fun next(): WaterfallsEvent?

    /** Ends the stream, releasing a pending [next]. Safe to call more than once. */
    fun close()
}

/** An account the Waterfalls server can watch: it opens a stream for its descriptor and rescans itself. */
interface WaterfallsAccount {
    val id: String

    suspend fun openWaterfallsStream(): WaterfallsStream

    suspend fun scanBlockchain()
}
