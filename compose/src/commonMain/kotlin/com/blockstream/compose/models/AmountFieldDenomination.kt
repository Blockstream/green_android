package com.blockstream.compose.models

import com.blockstream.compose.components.AmountFieldState
import com.blockstream.data.data.Denomination
import com.blockstream.data.utils.FIAT_DECIMALS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart

internal data class AmountFieldDenomination(
    val label: String,
    val maxFractionDigits: Int,
)

internal val Denomination.maxFractionDigits: Int
    get() = if (isFiat) FIAT_DECIMALS else Int.MAX_VALUE

internal fun Denomination.toAmountFieldDenomination(label: String = denomination) = AmountFieldDenomination(
    label = label,
    maxFractionDigits = maxFractionDigits,
)

internal fun GreenViewModel.amountFieldDenomination(
    assetId: Flow<String?> = flowOf(null),
): Flow<AmountFieldDenomination> = combine(denomination, assetId) { denomination, assetId ->
    denomination.toAmountFieldDenomination(
        label = sessionOrNull?.let {
            denomination.assetTickerOrFallback(session = it, assetId = assetId)
        } ?: denomination.denomination,
    )
}.flowOn(Dispatchers.IO)
    .onStart { emit(denomination.value.toAmountFieldDenomination()) }
    .distinctUntilChanged()

internal fun GreenViewModel.initialAmountFieldState(value: String) = denomination.value
    .toAmountFieldDenomination()
    .let { AmountFieldState(value = value, denominationLabel = it.label, maxFractionDigits = it.maxFractionDigits) }
