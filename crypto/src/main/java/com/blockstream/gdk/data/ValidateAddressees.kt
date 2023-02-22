package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ValidateAddressees constructor(
    @SerialName("addressees") val addressees: List<JsonObject>,
    @SerialName("errors") val errors: List<String>? = null,
    @SerialName("is_valid") val isValid: Boolean,
): GAJson<ValidateAddressees>() {
    override fun kSerializer() = serializer()

}