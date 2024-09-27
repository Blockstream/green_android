package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.data.Network
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValidateAddresseesParams(
    @SerialName("addressees") val addressees: List<AddressParams>,
) : GreenJson<ValidateAddresseesParams>() {
    override fun explicitNulls(): Boolean = false

    override fun kSerializer() = serializer()

    companion object {
        fun create(network: Network, address: String): ValidateAddresseesParams {
            return ValidateAddresseesParams(
                addressees = listOf(
                    AddressParams(
                        address = address,
                        assetId = network.policyAssetOrNull,
                        satoshi = 1_000_000 // GDK needs an amount larger than the dust
                    )
                )
            )
        }
    }
}