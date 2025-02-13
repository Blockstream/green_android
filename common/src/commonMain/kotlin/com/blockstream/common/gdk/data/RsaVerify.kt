package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.Serializable

@Serializable
data class RsaVerify(
    val result: Boolean? = null,
    val error: String? = null,
) : GreenJson<RsaVerify>() {
    override fun kSerializer() = serializer()
}