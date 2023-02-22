package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import com.blockstream.gdk.data.Addressee
import com.blockstream.gdk.data.Network
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValidateAddresseesParams private constructor(
    @SerialName("addressees") val addressees: List<Addressee>,
) : GAJson<ValidateAddresseesParams>() {
    override fun kSerializer() = serializer()

    companion object{
        fun create(network: Network, address: String): ValidateAddresseesParams {
            return ValidateAddresseesParams(
                addressees = listOf(
                    Addressee(
                        address = address,
                        assetId = network.policyAssetOrNull,
                        satoshi = 1_000_000 // GDK needs an amount larger than the dust
                    )
                )
            )
        }
    }
}