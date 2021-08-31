package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


// Params used also in getUnspentOutputs

@Serializable
data class BalanceParams constructor(
    @SerialName("subaccount") val subaccount: Long,
    // Only 0 or 1 is allowed for num_confs.
    // These correspond to including unconfirmed UTXOs and including only confirmed UTXOS respectively.
    @SerialName("num_confs") val confirmations: Int,
    @SerialName("dust_limit") val dustLimit: Int? = null,
    @SerialName("expired_at") val expiredAt: Long? = null,
    @SerialName("confidential") val confidential: Boolean? = null,
) : GAJson<BalanceParams>() {
    override val encodeDefaultsValues = false

    override fun kSerializer(): KSerializer<BalanceParams> {
        return serializer()
    }
}
