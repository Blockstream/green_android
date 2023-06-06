package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionUnblindedData(
    @SerialName("txid") val txid: String,
    @SerialName("type") val type: String,
    @SerialName("version") val version: Int,
    @SerialName("inputs") val inputs: List<InputUnblindedData>,
    @SerialName("outputs") val outputs: List<OutputUnblindedData>
) : GdkJson<TransactionUnblindedData>() {
    override fun kSerializer() = serializer()
}