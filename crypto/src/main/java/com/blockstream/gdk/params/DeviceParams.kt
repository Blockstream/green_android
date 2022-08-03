package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import com.blockstream.gdk.data.Device
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mu.KLogging

@Serializable
data class DeviceParams constructor(
    @SerialName("device") val device: Device? = null,
) : GAJson<DeviceParams>() {
    override val encodeDefaultsValues = false

    override fun kSerializer(): KSerializer<DeviceParams> {
        return serializer()
    }

    companion object: KLogging(){
        fun fromDeviceOrEmpty(device: Device?) = device?.let { DeviceParams(device = it) } ?: DeviceParams()
    }
}
