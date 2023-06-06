package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GdkJson
import com.blockstream.common.gdk.data.Device
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceParams constructor(
    @SerialName("device") val device: Device? = null,
) : GdkJson<DeviceParams>() {
    override val encodeDefaultsValues = false

    override fun kSerializer() = serializer()

    companion object {
        fun fromDeviceOrEmpty(device: Device?) = device?.let { DeviceParams(device = it) } ?: DeviceParams()
    }
}
