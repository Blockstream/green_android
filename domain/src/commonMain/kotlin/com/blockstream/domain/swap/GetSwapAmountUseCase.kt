@file:OptIn(ExperimentalCoroutinesApi::class)

package com.blockstream.domain.swap

import com.blockstream.data.data.Denomination
import com.blockstream.data.extensions.isNotBlank
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.data.swap.QuoteMode
import com.blockstream.data.swap.QuoteValidity
import com.blockstream.data.swap.SwapAmount
import com.blockstream.data.utils.UserInput
import com.blockstream.data.utils.toAmountLook
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Provides a reactive flow of [SwapAmount] based on user input and selected swap pair.
 *
 * This use case combines source/destination balances, the user-entered amount, and current swap limits
 * to validate the transaction. It calculates the expected receive amounts (in crypto and fiat) and
 * provides descriptive error messages if the input violates balance or provider limits.
 */
class GetSwapAmountUseCase(
    private val getQuoteUseCase: GetQuoteUseCase
) {

    /**
     * Returns a [Flow] that emits updated [SwapAmount] whenever any input changes.
     *
     * The validation logic includes:
     * 1. Parsing user input according to the selected denomination.
     * 2. Checking against the source account balance (Insufficient Funds).
     * 3. Fetching and validating against minimum and maximum swap limits for the pair.
     * 4. Calculating the estimated amount to be received after provider fees.
     *
     * @param session the current [GdkSession]
     * @param from [Flow] of the source account balance
     * @param to [Flow] of the destination account balance
     * @param amount [Flow] of the user-entered amount string
     * @param denomination [Flow] of the selected [Denomination]
     * @return a [Flow] containing the calculated [SwapAmount] with optional error messages
     */
    operator fun invoke(
        session: GdkSession,
        from: Flow<AccountAssetBalance>,
        to: Flow<AccountAssetBalance>,
        amountFrom: Flow<String>,
        amountTo: Flow<String>,
        quoteMode: Flow<QuoteMode>,
        denomination: Flow<Denomination>
    ): Flow<SwapAmount> {

        val account = quoteMode.flatMapLatest {
            if (it.isSend) from else to
        }

        val amount = quoteMode.flatMapLatest {
            if (it.isSend) amountFrom else amountTo
        }

        val balance = combine(account, amount, denomination) { account, amount, denomination ->
            amount.takeIf { it.isNotBlank() }?.let {
                UserInput.parseUserInputSafe(
                    session = session, input = it, assetId = account.asset.assetId, denomination = denomination
                ).getBalance()
            }
        }

        val quote = getQuoteUseCase(
            session = session,
            from = from.map { it.accountAsset.toSwapAsset() }.distinctUntilChanged(),
            to = to.map { it.accountAsset.toSwapAsset() }.distinctUntilChanged(),
            satoshi = balance.map {
                it?.satoshi ?: 0
            },
            quoteMode = quoteMode
        )

        return combine(
            from.distinctUntilChanged(),
            to.distinctUntilChanged(),
            amountFrom,
            quote,
            denomination
        ) { from, to, amountFrom, quote, denomination ->

            val swapAmount = SwapAmount(
                quote = quote,
                amountFrom = quote?.sendAmount.toAmountLook(
                    session = session, assetId = from.assetId, denomination = denomination, withGrouping = false, withUnit = false
                ) ?: "",
                amountFromExchange = quote?.sendAmount.toAmountLook(
                    session = session,
                    assetId = from.assetId,
                    denomination = Denomination.exchange(session, denomination),
                )?.let { if (denomination.isFiat) it else "≈ $it" },
                amountTo = quote?.receiveAmount.toAmountLook(
                    session = session, assetId = to.assetId, denomination = denomination, withGrouping = false, withUnit = false
                ) ?: "",
                amountToExchange = quote?.receiveAmount.toAmountLook(
                    session = session,
                    assetId = to.assetId,
                    denomination = Denomination.exchange(session, denomination),
                )?.let { if (denomination.isFiat) it else "≈ $it" }
            )

            val balance = UserInput.parseUserInputSafe(
                session = session, input = amountFrom, assetId = from.asset.assetId, denomination = denomination
            ).getBalance()

            val satoshi = balance?.satoshi ?: 0

            val isValid = quote?.isValid(satoshi) ?: QuoteValidity.VALID

            val error = when {
                satoshi > (from.satoshi ?: 0) -> "id_insufficient_funds"
                isValid == QuoteValidity.MIN -> {
                    quote?.minimal.toAmountLook(
                        session = session,
                        assetId = from.account.network.policyAsset,
                        denomination = denomination.notFiat(),
                        withUnit = true
                    )?.let {
                        "id_amount_too_low_s|$it"
                    }
                }

                isValid == QuoteValidity.MAX -> {
                    quote?.maximal.toAmountLook(
                        session = session,
                        assetId = from.account.network.policyAsset,
                        denomination = denomination.notFiat(),
                        withUnit = true
                    )?.let {
                        "id_amount_too_high_s|$it"
                    }
                }

                else -> null
            }

            swapAmount.copy(error = error)
        }
    }
}
