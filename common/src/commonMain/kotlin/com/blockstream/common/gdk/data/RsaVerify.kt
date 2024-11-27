package com.blockstream.common.gdk.data

import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class RsaVerify(
    val result: Boolean? = null,
    val error: String? = null,
) : GreenJson<RsaVerify>(), Parcelable {
    override fun kSerializer() = serializer()
}