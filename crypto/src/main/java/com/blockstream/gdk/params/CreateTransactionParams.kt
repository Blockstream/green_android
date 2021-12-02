package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CreateTransactionParams constructor(
    @SerialName("subaccount") val subaccount: Long,
    @SerialName("addressees") val addressees: List<AddressParams>? = null, // This can also be a BIP21 URI
    @SerialName("send_all") val sendAll: Boolean = false,
    @SerialName("fee_rate") val feeRate: Long? = null,
    @SerialName("private_key") val privateKey: String? = null, // sweep
    @SerialName("passphrase") val passphrase: String? = null, // sweep
    @SerialName("previous_transaction") val previousTransaction: JsonElement? = null, // bump
    @SerialName("memo") val memo: String? = null,
    @SerialName("utxos") val utxos: JsonElement? = null,
) : GAJson<CreateTransactionParams>() {
    override val encodeDefaultsValues = false

    override fun kSerializer(): KSerializer<CreateTransactionParams> {
        return serializer()
    }
}
