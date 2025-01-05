package com.blockstream.common.data

import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.devices.ConnectionType
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.devices.DeviceModel
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Parcelize
@Serializable
data class DeviceIdentifier constructor(
    @SerialName("name") val name: String,
    @SerialName("unique_identifier") val uniqueIdentifier: String,
    @SerialName("brand") val brand: DeviceBrand? = null, // Deprecated in favor of DeviceModel
    @SerialName("model") val model: DeviceModel? = null,
    @SerialName("connection") val connectionType: ConnectionType,
) : GreenJson<DeviceIdentifier>(), Parcelable {

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
