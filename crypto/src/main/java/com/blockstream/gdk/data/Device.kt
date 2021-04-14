package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import com.blockstream.gdk.serializers.DeviceLiquidSupportSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Device(
    @SerialName("name") val name: String,
    @SerialName("supports_arbitrary_scripts") val supportsArbitraryScripts: Boolean,
    @SerialName("supports_low_r") val supportsLowR: Boolean,

    @Serializable(with = DeviceLiquidSupportSerializer::class)
    @SerialName("supports_liquid") val supportsLiquid: DeviceLiquidSupport,
): GAJson<Device>() {

    override fun kSerializer(): KSerializer<Device> {
        return serializer()
    }
}