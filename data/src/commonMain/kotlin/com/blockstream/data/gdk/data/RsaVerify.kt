package com.blockstream.data.gdk.data

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.Serializable

@Serializable
data class RsaVerify(
    val result: Boolean? = null,
    val error: String? = null,
) : GreenJson<RsaVerify>() {
    override fun kSerializer() = serializer()
}