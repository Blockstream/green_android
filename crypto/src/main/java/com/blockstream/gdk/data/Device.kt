package com.blockstream.gdk.data

import android.os.Parcelable
import com.blockstream.gdk.GAJson
import com.blockstream.gdk.serializers.DeviceSupportsAntiExfilProtocolSerializer
import com.blockstream.gdk.serializers.DeviceSupportsLiquidSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Device constructor(
    @SerialName("name") val name: String,
    @SerialName("supports_arbitrary_scripts") val supportsArbitraryScripts: Boolean,
    @SerialName("supports_low_r") val supportsLowR: Boolean,
    @SerialName("supports_host_unblinding") val supportsHostUnblinding: Boolean,
    @SerialName("supports_external_blinding") val supportsExternalBlinding: Boolean,

    @Serializable(with = DeviceSupportsLiquidSerializer::class)
    @SerialName("supports_liquid") val supportsLiquid: DeviceSupportsLiquid,
    @Serializable(with = DeviceSupportsAntiExfilProtocolSerializer::class)
    @SerialName("supports_ae_protocol") val supportsAntiExfilProtocol: DeviceSupportsAntiExfilProtocol,

    // Ignore it for now
    // @SerialName("device_type") val deviceType: String?, // is set by GDK (hardware)
): GAJson<Device>(), Parcelable {
    val isJade
        get() = name.lowercase() == "jade"

    val isTrezor
        get() = name.lowercase() == "trezor"

    val isLedger
        get() = name.lowercase() == "ledger"

    override fun kSerializer(): KSerializer<Device> {
        return serializer()
    }
}