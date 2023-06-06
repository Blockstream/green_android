package com.blockstream.green.database

import android.os.Parcelable
import com.blockstream.common.gdk.device.DeviceBrand
import com.blockstream.common.gdk.GdkJson
import com.blockstream.green.devices.Device
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class DeviceIdentifier constructor(
    @SerialName("name") val name: String,
    @SerialName("unique_identifier") val uniqueIdentifier: String,
    @SerialName("brand") val brand: DeviceBrand,
    @SerialName("connection") val connectionType: Device.ConnectionType,
): GdkJson<DeviceIdentifier>(), Parcelable {

    override fun kSerializer() = serializer()
}