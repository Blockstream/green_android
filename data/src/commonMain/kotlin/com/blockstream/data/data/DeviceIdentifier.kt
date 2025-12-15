package com.blockstream.data.data

import com.blockstream.data.devices.ConnectionType
import com.blockstream.data.devices.DeviceBrand
import com.blockstream.data.devices.DeviceModel
import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DeviceIdentifier constructor(
    @SerialName("name")
    val name: String,
    @SerialName("unique_identifier")
    val uniqueIdentifier: String,
    @SerialName("brand")
    val brand: DeviceBrand? = null, // Deprecated in favor of DeviceModel
    @SerialName("model")
    val model: DeviceModel? = null,
    @SerialName("connection")
    val connectionType: ConnectionType,
) : GreenJson<DeviceIdentifier>() {

    override fun kSerializer() = serializer()

    companion object {
        fun fromString(jsonString: String): List<DeviceIdentifier>? = try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

fun List<DeviceIdentifier>.toJson(): String {
    return Json.encodeToString(this)
}

fun String.toDeviceIdentifierList(): List<DeviceIdentifier>? = DeviceIdentifier.fromString(this)
