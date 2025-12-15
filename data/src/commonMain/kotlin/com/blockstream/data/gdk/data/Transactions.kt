package com.blockstream.data.gdk.data

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Transactions(
    @SerialName("transactions")
    val transactions: List<Transaction> = listOf(),
) : GreenJson<Transactions>() {
    override fun keepJsonElement() = true

    override fun kSerializer(): KSerializer<Transactions> = serializer()
}