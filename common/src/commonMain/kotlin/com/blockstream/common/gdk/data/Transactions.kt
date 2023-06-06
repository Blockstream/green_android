package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class Transactions(
    @SerialName("transactions") val transactions: List<Transaction> = listOf(),
): GdkJson<Transactions>(){
    override val keepJsonElement: Boolean = true

    override fun kSerializer(): KSerializer<Transactions> = serializer()
}