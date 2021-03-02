package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Device(
    @SerialName("name") val name: String,
    @SerialName("supports_arbitrary_scripts") val supportsArbitraryScripts: String,
    @SerialName("supports_low_r") val supportsLowR: Boolean,

    // TODO change it to enum
    @SerialName("supports_liquid") val supportsLiquid: Int,

)