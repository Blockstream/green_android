package com.blockstream.data.data

import breez_sdk.LnUrlWithdrawRequestData
import kotlinx.serialization.Serializable

@Serializable
data class LnUrlWithdrawRequestSerializable(
    var callback: String,
    var k1: String,
    var defaultDescription: String,
    var minWithdrawable: ULong,
    var maxWithdrawable: ULong
) {
    fun deserialize() = LnUrlWithdrawRequestData(
        callback = callback,
        k1 = k1,
        defaultDescription = defaultDescription,
        minWithdrawable = minWithdrawable,
        maxWithdrawable = maxWithdrawable
    )
}

fun LnUrlWithdrawRequestData.toSerializable() = LnUrlWithdrawRequestSerializable(
    callback = callback,
    k1 = k1,
    defaultDescription = defaultDescription,
    minWithdrawable = minWithdrawable,
    maxWithdrawable = maxWithdrawable
)
