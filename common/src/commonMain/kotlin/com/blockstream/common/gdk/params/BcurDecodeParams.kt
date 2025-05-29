package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BcurDecodeParams constructor(
    @SerialName("part")
    val part: String
) : GreenJson<BcurDecodeParams>() {

    override fun kSerializer() = serializer()
}