package com.blockstream.domain.wallet

import com.blockstream.data.backend.NetworkEvent
import com.blockstream.data.comparators.ComparatorAssets
import com.blockstream.data.data.DataState
import com.blockstream.data.extensions.toSortedLinkedHashMap
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Assets
import com.blockstream.domain.base.DataStateObservableUseCase
import com.blockstream.utils.Loggable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * Aggregates every account's balance into a single wallet-level [Assets].
 *
 * Each account is queried for its balance; the per-account balances are summed per asset id and
 * sorted with [ComparatorAssets] (policy asset first, then by value/metadata).
 *
 * While [observe] is collected the aggregate is recomputed on every network event (a new block, or
 * a transaction touching the wallet) and whenever the set of accounts changes. The stream is shared,
 * so concurrent collectors drive a single refresh rather than one each.
 */
class GetWalletAssetsUseCase(private val session: GdkSession) : DataStateObservableUseCase<Unit, Assets>() {

    override fun shouldExecute(params: Unit): Boolean = session.isConnected

    override suspend fun doAsyncWork(params: Unit) {
        val walletAssets = session.accounts.value
            .mapNotNull {
                tryCatch {
                    session.getBalance(account = it)
                }
            }
            .fold(Assets()) { acc, assets -> acc + assets }.assets
            .toSortedLinkedHashMap(ComparatorAssets(session = session))
            .let {
                Assets(it)
            }

        set(walletAssets)
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override fun createObservable(params: Unit): Flow<DataState<Assets>> {
        // Refresh on a network event (a new block, or a transaction touching the wallet).
        // Merge every backend's live events. The event flow does not replay, so a (re)subscribe
        // alone does not refresh and the first live event after subscribing is not dropped.
        val networkEventTriggers = session.networkBackendsStateFlow
            .flatMapLatest { backends ->
                backends.values.map { it.networkEventsFlow }.merge()
            }
            .filter { event ->
                when (event) {
                    is NetworkEvent.Block -> true
                    is NetworkEvent.Transaction -> true
                    is NetworkEvent.Synced -> true
                }
            }
            .map { }

        // Refresh when the set of accounts changes (created, archived, hidden/unhidden) — a
        // lifecycle change rather than a network event, which would otherwise be missed.
        val accountTriggers = session.accounts
            .drop(1)
            .map { }

        val lightningTriggers = session.isLightningLoading
            .filter { !it }
            .map { }

        // Emits nothing itself; the refresh writes to the cache that get() exposes, and lives only
        // while observe() is collected. Debounced once over the merged triggers so a burst from
        // either source — or both arriving together — collapses into a single doWork.
        val refresh = flow<DataState<Assets>> {
            merge(networkEventTriggers, accountTriggers, lightningTriggers)
                .debounce(750)
                .collect {
                    doWork(Unit)
                }
        }

        return merge(get(), refresh)
    }

    companion object : Loggable()
}
