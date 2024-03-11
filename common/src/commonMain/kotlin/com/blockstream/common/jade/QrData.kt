package com.blockstream.common.jade

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QrData constructor(
    @SerialName("data") val data: String,
) : GreenJson<QrData>() {

    override fun encodeDefaultsValues(): Boolean = true
    override fun kSerializer() = serializer()
}

@Serializable
data class QrDataResponse constructor(
    @SerialName("id") val id: String = "0",
    @SerialName("method") val method: String,
    @SerialName("params") val params: QrData,
) : GreenJson<QrDataResponse>() {

    override fun encodeDefaultsValues(): Boolean = true
    override fun kSerializer() = serializer()
}

