package com.blockstream.common.data

import breez_sdk.LnUrlAuthRequestData
import cafe.adriel.voyager.core.lifecycle.JavaSerializable
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize

@Parcelize
data class LnUrlAuthRequestDataSerializable(
    var k1: String,
    var domain: String,
    var url: String,
    var action: String? = null
) : Parcelable, JavaSerializable {
    fun deserialize() = LnUrlAuthRequestData(
        k1 = k1,
        domain = domain,
        url = url,
        action = action
    )
}

fun LnUrlAuthRequestData.toSerializable() =
    LnUrlAuthRequestDataSerializable(k1 = k1, domain = domain, url = url, action = action)