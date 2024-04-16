package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.data.AccountAsset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement


@Serializable
data class CreateTransactionParams constructor(
    @SerialName("from") val from: AccountAsset? = null,
    @SerialName("to") val to: AccountAsset? = null,

    @kotlin.jvm.Transient
    @SerialName("addressees") val addressees: List<JsonElement>? = null, // This can also be a BIP21 URI
    @Transient
    val addresseesAsParams: List<AddressParams>? = null, // This can also be a BIP21 URI
    @SerialName("fee_rate") val feeRate: Long? = null,
    @SerialName("private_key") val privateKey: String? = null, // sweep
    @SerialName("passphrase") val passphrase: String? = null, // sweep
    @kotlin.jvm.Transient
    @SerialName("previous_transaction") val previousTransaction: JsonElement? = null, // bump
    @SerialName("memo") val memo: String? = null,
    @kotlin.jvm.Transient
    @SerialName("utxos") val utxos: JsonElement? = null,
) : GreenJson<CreateTransactionParams>() {

    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()
}
