package com.blockstream.lightning

import breez_sdk.GreenlightCredentials
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KLogging

@Serializable
data class AppGreenlightCredentials(
    @Serializable(with = ListUByteSerializer::class)
    @SerialName("deviceKey")
    val deviceKey: List<UByte>,
    @Serializable(with = ListUByteSerializer::class)
    @SerialName("deviceCert")
    val deviceCert: List<UByte>
) {
    fun toGreenlightCredentials(): GreenlightCredentials {
        return GreenlightCredentials(
            deviceKey = deviceKey,
            deviceCert = deviceCert
        )
    }

    override fun toString(): String {
        return Json.encodeToString(this)
    }

    fun toJson() = toString()

    companion object : KLogging() {

        fun fromJsonString(jsonString: String): AppGreenlightCredentials {
            return Json.decodeFromString(jsonString)
        }

        fun fromGreenlightCredentials(greenlightCredentials: GreenlightCredentials): AppGreenlightCredentials {
            return AppGreenlightCredentials(
                deviceKey = greenlightCredentials.deviceKey,
                deviceCert = greenlightCredentials.deviceCert
            )
        }
    }
}