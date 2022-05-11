package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProxySettings constructor(
    @SerialName("proxy") val proxy: String? = null,
    @SerialName("use_tor") val tor: Boolean? = false,
)