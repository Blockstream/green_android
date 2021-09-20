package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class Transactions(
    @SerialName("transactions") val transactions: List<Transaction> = listOf(),
): GAJson<Transactions>(){
    override val keepJsonElement: Boolean = true

    override fun kSerializer(): KSerializer<Transactions> = serializer()
}