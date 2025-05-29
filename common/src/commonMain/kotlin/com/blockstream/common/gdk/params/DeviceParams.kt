package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.data.Device
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceParams constructor(
    @SerialName("device")
    val device: Device? = null,
) : GreenJson<DeviceParams>() {
    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()

    companion object {
        val Empty = DeviceParams()

        fun fromDeviceOrEmpty(device: Device?) = device?.let { DeviceParams(device = it) } ?: DeviceParams()
    }
}
