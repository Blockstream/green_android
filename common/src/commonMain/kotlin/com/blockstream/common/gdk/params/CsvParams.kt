package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CsvParams(
    @SerialName("value") val value: Int,
) : GdkJson<CsvParams>() {

    override fun kSerializer() = serializer()
}