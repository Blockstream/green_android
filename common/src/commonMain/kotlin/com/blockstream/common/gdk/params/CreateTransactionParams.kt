package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement

@Serializable
data class CreateTransactionParams constructor(
    @SerialName("subaccount") val subaccount: Long? = null,
    @SerialName("addressees") val addressees: List<JsonElement>? = null, // This can also be a BIP21 URI
    @Transient
    val addresseesAsParams: List<AddressParams>? = null, // This can also be a BIP21 URI
    @SerialName("fee_rate") val feeRate: Long? = null,
    @SerialName("private_key") val privateKey: String? = null, // sweep
    @SerialName("passphrase") val passphrase: String? = null, // sweep
    @SerialName("previous_transaction") val previousTransaction: JsonElement? = null, // bump
    @SerialName("memo") val memo: String? = null,
    @SerialName("utxos") val utxos: JsonElement? = null,
) : GreenJson<CreateTransactionParams>() {

    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()
}
