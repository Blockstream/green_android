package com.blockstream.domain.swap

import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.swap.Quote
import com.blockstream.data.swap.QuoteMode
import com.blockstream.data.swap.SwapAsset
import com.blockstream.jade.Loggable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class GetQuoteUseCase() {
    operator fun invoke(
        session: GdkSession, from: Flow<SwapAsset>, to: Flow<SwapAsset>, satoshi: Flow<Long>, quoteMode: Flow<QuoteMode>
    ): Flow<Quote?> {
        val swapInfo = combine(from.distinctUntilChanged(), to.distinctUntilChanged()) { _, _ ->
            tryCatch { session.lwk.refreshSwapInfo() }
        }

        return combine(satoshi, quoteMode, swapInfo, from, to) { satoshi, quoteMode, _, from, to ->
            if (satoshi == 0L) {
                null
            } else {
                tryCatch { session.lwk.quote(satoshi, quoteMode, from, to) }?.also {
                    logger.d { "Quote: $it" }
                }
            }
        }
    }

    companion object : Loggable()
}

fun AccountAsset.toSwapAsset() = when {
    account.isLightning -> SwapAsset.Lightning
    account.isLiquid -> SwapAsset.Liquid
    account.isBitcoin -> SwapAsset.Bitcoin
    else -> throw Exception("Invalid account type)")
}