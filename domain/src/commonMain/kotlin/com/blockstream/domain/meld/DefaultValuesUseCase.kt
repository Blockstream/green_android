package com.blockstream.domain.meld

import com.blockstream.data.CountlyBase
import com.blockstream.data.extensions.tryCatchNullSuspend
import com.blockstream.data.json.DefaultJson
import kotlinx.serialization.json.decodeFromJsonElement

class DefaultValuesUseCase(
    private val countly: CountlyBase
) {
    suspend operator fun invoke(
        symbol: String
    ): List<String> = tryCatchNullSuspend {
        countly.getRemoteConfigValueAsJsonElement("buy_default_values")?.let {
            DefaultJson.decodeFromJsonElement<com.blockstream.data.meld.data.BuyDefaultValues>(it)
        }?.let {
            it.buyDefaultValues[symbol.lowercase()]
        } ?: emptyList()
    } ?: emptyList()
}