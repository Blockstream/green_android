package com.blockstream.common.gdk.params

import com.arkivanov.essenty.parcelable.IgnoredOnParcel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.essenty.parcelable.TypeParceler
import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.parcelizer.JsonElementParceler
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement

@Parcelize
@Serializable
@TypeParceler<JsonElement?, JsonElementParceler>()
data class CreateTransactionParams constructor(
    @SerialName("subaccount") val subaccount: Long? = null,
    @kotlin.jvm.Transient
    @IgnoredOnParcel
    @SerialName("addressees") val addressees: List<JsonElement>? = null, // This can also be a BIP21 URI
    @Transient
    val addresseesAsParams: List<AddressParams>? = null, // This can also be a BIP21 URI
    @SerialName("fee_rate") val feeRate: Long? = null,
    @SerialName("private_key") val privateKey: String? = null, // sweep
    @SerialName("passphrase") val passphrase: String? = null, // sweep
    @IgnoredOnParcel
    @SerialName("previous_transaction") val previousTransaction: JsonElement? = null, // bump
    @SerialName("memo") val memo: String? = null,
    @IgnoredOnParcel
    @SerialName("utxos") val utxos: JsonElement? = null,
) : GreenJson<CreateTransactionParams>(), Parcelable {

    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()
}
