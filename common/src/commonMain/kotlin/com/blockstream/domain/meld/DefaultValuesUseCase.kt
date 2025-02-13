package com.blockstream.domain.meld

import com.blockstream.common.CountlyBase
import com.blockstream.common.extensions.tryCatchNullSuspend
import com.blockstream.green.data.json.DefaultJson
import com.blockstream.green.data.meld.data.BuyDefaultValues
import kotlinx.serialization.json.decodeFromJsonElement


class DefaultValuesUseCase(
    private val countly: CountlyBase
) {
    suspend operator fun invoke(
        symbol: String
    ): List<String> = tryCatchNullSuspend {
        countly.getRemoteConfigValueAsJsonElement("buy_default_values")?.let {
            DefaultJson.decodeFromJsonElement<BuyDefaultValues>(it)
        }?.let {
            it.buyDefaultValues[symbol.lowercase()]
        } ?: emptyList()
    } ?: emptyList()
}