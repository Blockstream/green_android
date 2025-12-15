package com.blockstream.data.gdk.params

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CsvParams(
    @SerialName("value")
    val value: Int,
) : GreenJson<CsvParams>() {

    override fun kSerializer() = serializer()
}