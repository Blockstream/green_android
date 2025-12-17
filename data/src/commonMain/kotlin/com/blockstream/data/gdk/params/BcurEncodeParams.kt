package com.blockstream.data.gdk.params

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BcurEncodeParams constructor(
    @SerialName("ur_type")
    val urType: String,
    @SerialName("data")
    val data: String? = null, // cbor hex or base64 for crypto-psbt
    @SerialName("json")
    val jsonString: String? = null,
    @SerialName("num_words")
    val numWords: Int? = null,
    @SerialName("index")
    val index: Long? = null,
    @SerialName("private_key")
    val privateKey: String? = null,
    @SerialName("max_fragment_len")
    val maxFragmentLen: Int = 50
) : GreenJson<BcurEncodeParams>() {
    override fun explicitNulls(): Boolean = false
    override fun encodeDefaultsValues(): Boolean = true

    override fun kSerializer() = serializer()
}
