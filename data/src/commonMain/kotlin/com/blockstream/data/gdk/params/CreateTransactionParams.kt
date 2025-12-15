package com.blockstream.data.gdk.params

import com.blockstream.data.gdk.GreenJson
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.lwk.NormalSubmarineSwap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class CreateTransactionParams constructor(
    @SerialName("subaccount")
    val subaccount: Long? = null,
    @SerialName("from")
    val from: AccountAsset? = null,
    @SerialName("to")
    val to: AccountAsset? = null,
    @SerialName("isRedeposit")
    val isRedeposit: Boolean = false,
    @SerialName("addressees")
    val addressees: List<JsonElement>? = null, // This can also be a BIP21 URI
    @SerialName("fee_rate")
    val feeRate: Long? = null,
    @SerialName("private_key")
    val privateKey: String? = null, // sweep
    @SerialName("previous_transaction")
    val previousTransaction: JsonElement? = null, // bump
    @SerialName("memo")
    val memo: String? = null,
    @SerialName("utxos")
    val utxos: Map<String, List<JsonElement>>? = null,
    @SerialName("fee_subaccount")
    val feeSubaccount: Long? = null,
    @SerialName("submarine_swap")
    val submarineSwap: NormalSubmarineSwap? = null,
) : GreenJson<CreateTransactionParams>() {

    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()

    val addresseesAsParams: List<AddressParams>? by lazy {
        addressees?.map {
            json.decodeFromJsonElement(it)
        }
    }
}

fun List<AddressParams>.toJsonElement() = this.map {
    GreenJson.json.encodeToJsonElement(it)
}
