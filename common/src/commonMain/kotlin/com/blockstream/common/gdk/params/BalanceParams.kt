package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GreenJson
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
) : GreenJson<BalanceParams>() {
    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()
}
