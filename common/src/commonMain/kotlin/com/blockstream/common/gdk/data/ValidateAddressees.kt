package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ValidateAddressees constructor(
    @SerialName("addressees") val addressees: List<JsonObject>,
    @SerialName("errors") val errors: List<String>? = null,
    @SerialName("is_valid") val isValid: Boolean,
): GdkJson<ValidateAddressees>() {
    override fun kSerializer() = serializer()
}