package com.blockstream.data.data

import breez_sdk.LnUrlAuthRequestData
import kotlinx.serialization.Serializable

@Serializable
data class LnUrlAuthRequestDataSerializable(
    var k1: String,
    var domain: String,
    var url: String,
    var action: String? = null
) {
    fun deserialize() = LnUrlAuthRequestData(
        k1 = k1,
        domain = domain,
        url = url,
        action = action
    )
}

fun LnUrlAuthRequestData.toSerializable() =
    LnUrlAuthRequestDataSerializable(k1 = k1, domain = domain, url = url, action = action)