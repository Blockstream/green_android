package com.blockstream.green.database

import android.os.Parcelable
import com.blockstream.DeviceBrand
import com.blockstream.gdk.GAJson
import com.blockstream.green.devices.Device
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class DeviceIdentifier constructor(
    @SerialName("name") val name: String,
    @SerialName("unique_identifier") val uniqueIdentifier: String,
    @SerialName("brand") val brand: DeviceBrand,
    @SerialName("connection") val connectionType: Device.ConnectionType,
): GAJson<DeviceIdentifier>(), Parcelable {

    override fun kSerializer(): KSerializer<DeviceIdentifier> {
        return serializer()
    }
}