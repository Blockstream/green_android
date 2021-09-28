package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionUnblindedData(
    @SerialName("txid") val txid: String,
    @SerialName("type") val type: String,
    @SerialName("version") val version: Int,
    @SerialName("inputs") val inputs: List<InputUnblindedData>,
    @SerialName("outputs") val outputs: List<OutputUnblindedData>
) : GAJson<TransactionUnblindedData>() {
    override fun kSerializer(): KSerializer<TransactionUnblindedData> = serializer()
}