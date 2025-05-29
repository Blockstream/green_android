package com.blockstream.common.lightning

import com.blockstream.common.serializers.ListUByteSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AppGreenlightCredentials constructor(
    @Serializable(with = ListUByteSerializer::class)
    @SerialName("deviceKey")
    val deviceKey: List<UByte>,
    @Serializable(with = ListUByteSerializer::class)
    @SerialName("deviceCert")
    val deviceCert: List<UByte>
) {

    override fun toString(): String {
        return Json.encodeToString(this)
    }

    fun toJson() = toString()

    companion object {

        fun fromJsonString(jsonString: String): AppGreenlightCredentials {
            return Json.decodeFromString(jsonString)
        }
    }
}