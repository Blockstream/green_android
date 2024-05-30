package com.blockstream.common.data

import breez_sdk.LnUrlWithdrawRequestData
import cafe.adriel.voyager.core.lifecycle.JavaSerializable
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize

@Parcelize
data class LnUrlWithdrawRequestSerializable(
    var callback: String,
    var k1: String,
    var defaultDescription: String,
    var minWithdrawable: ULong,
    var maxWithdrawable: ULong
) : Parcelable, JavaSerializable {
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
